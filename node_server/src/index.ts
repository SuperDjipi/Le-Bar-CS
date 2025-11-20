/**
 * Ce fichier est le point d'entrée principal et le cœur du serveur de jeu Node.js.
 * Il est responsable de :
 * 1. Démarrer un serveur web Express.
 * 2. Lancer un serveur WebSocket par-dessus le serveur Express pour la communication en temps réel.
 * 3. Gérer les connexions, déconnexions et messages des clients.
 * 4. Maintenir l'état de toutes les parties en mémoire.
 * 5. Agir comme un "contrôleur" qui reçoit les événements des clients et délègue la logique
 *    de jeu au "moteur de jeu" (`GameEngine`).
 */

import express from 'express';
import { WebSocketServer, WebSocket } from 'ws';
// Import des modèles de données et des types d'événements
import type { ClientToServerEvent, ServerToClientEvent } from './models/GameEvents.js';
import type { GameState, Tile, UserProfile, Player, PlacedTile } from './models/GameModels.js';
import { GameStatus } from './models/GameModels.js';
// Import des modules de logique métier
import { createTileBag, drawTiles } from './logic/TileBag.js';
import { createEmptyBoard, createNewBoard } from './models/BoardModels.js';
import { processPlayMove } from './logic/GameEngine.js'; // Le moteur de jeu principal
import { URL } from 'url'; // Utile pour parser l'URL de connexion
import { v4 as generateUUID } from 'uuid';
import { initializeDatabase } from './db/database.js';

// --- GESTION DES PARTIES EN MÉMOIRE ---

/**
 * La "base de données" en mémoire pour toutes les parties actives.
 * C'est une Map qui associe un identifiant de partie (`gameId`) à son état complet (`GameState`).
 * NOTE : Ces données sont volatiles et seront perdues si le serveur redémarre.
 */
const games = new Map<string, GameState>();

/**
 * La gestion des connexions WebSocket actives.
 * C'est une structure de données imbriquée :
 * Map<gameId, Map<playerId, WebSocket>>
 * - La clé externe est l'ID de la partie.
 * - La valeur est une autre Map qui associe l'ID d'un joueur (`playerId`) à son instance WebSocket.
 * Cela nous permet de savoir qui est qui et d'envoyer des messages ciblés.
 */
const connections = new Map<string, Map<string, WebSocket>>();

/**
 * Initialise le conteneur de connexions pour une partie donnée si ce n'est pas déjà fait.
 */
function initGameConnections(gameId: string) {
    if (!connections.has(gameId)) {
        connections.set(gameId, new Map<string, WebSocket>());
    }
}
    /**
     * Génère un code de partie simple de 4 lettres majuscules.
     */
    function generateGameCode(): string {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        let code = '';
        for (let i = 0; i < 4; i++) {
            code += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        // TODO: Plus tard, on vérifiera que ce code n'est pas déjà utilisé.
        return code;
    }


/**
 * Prépare une version personnalisée du `GameState` pour un joueur spécifique.
 * Cette fonction est cruciale pour la sécurité et la confidentialité :
 * - Elle vide les chevalets (`rack`) de tous les autres joueurs.
 * - Elle ne révèle pas le contenu de la pioche (`tileBag`).
 * @param gameState L'état de jeu officiel et complet.
 * @param playerId L'ID du joueur pour qui l'état est préparé.
 * @returns Un objet contenant l'état "public" et le chevalet privé du joueur.
 */
function prepareStateForPlayer(gameState: GameState, playerId: string): { stateForPlayer: GameState, playerRack: Tile[] } {
    let playerRack: Tile[] = [];
    const stateForPlayer: GameState = {
        ...gameState,
        players: gameState.players.map(p => {
            if (p.id === playerId) {
                playerRack = p.rack;
            }
            return { ...p, rack: [] }; // On vide le chevalet pour les autres
        }),
        tileBag: [] // On ne révèle jamais la pioche au client
    };
    return { stateForPlayer, playerRack };
}

// --- DÉMARRAGE DU SERVEUR ---

async function startServer() {
    const db = await initializeDatabase(); // On initialise la DB en premier
    const app = express();
    // Middleware pour servir les fichiers statiques (HTML, CSS, JS) du dossier 'public'.
    app.use(express.static('public'));
    // Middleware pour permettre à Express de comprendre le JSON envoyé dans le corps des requêtes POST.
    app.use(express.json());

    const port = 8080;
    // On lance le serveur HTTP Express...
    const server = app.listen(port, () => {
        console.log(`✅ Serveur démarré et à l'écoute sur http://localhost:${port}`);
    });
    // ...et on attache le serveur WebSocket à ce serveur HTTP.
    const wss = new WebSocketServer({ server });

    // --- DÉBUT DE L'API D'INSCRIPTION ---

    /**
     * Route API pour l'inscription d'un nouveau joueur.
     * Attend une requête POST sur /api/register avec un corps JSON
     * contenant 'name' et 'password'.*/
    app.post('/api/register', async (req, res) => {// La fonction devient async
        const { name, password } = req.body;
        if (!name || !password) {
            return res.status(400).send({ message: "Le pseudo et le mot de passe sont requis." });
        }

        try {
            // On vérifie si le nom existe déjà dans la base de données
            const existingUser = await db.get('SELECT * FROM users WHERE LOWER(name) = ?', name.toLowerCase());
            if (existingUser) {
                return res.status(409).send({ message: "Ce pseudo est déjà pris." });
            }

            // Création du profil
            const newPlayerId = generateUUID();
            const hashedPassword = password; // TODO: HASH ME!

            // On exécute la requête SQL pour insérer le nouvel utilisateur
            await db.run(
                'INSERT INTO users (id, name, hashedPassword) VALUES (?, ?, ?)',
                [newPlayerId, name, hashedPassword]
            );

            console.log(`✅ Nouveau joueur inséré dans la DB : ${name}`);
            res.status(201).send({ message: `Profil pour '${name}' créé avec succès !`, playerId: newPlayerId });

        } catch (error) {
            console.error("Erreur lors de l'inscription:", error);
            res.status(500).send({ message: "Erreur interne du serveur." });
        }
    });
    // --- FIN DE L'API D'INSCRIPTION ---

    // --- DÉBUT DE L'API DE CRÉATION DE PARTIE ---
    /**
     * Route API pour créer une nouvelle partie.
     * Attend une requête POST sur /api/games.
     * Le corps de la requête doit contenir l'ID du joueur qui crée la partie.
     * @body { "creatorId": "xxxx-yyyy-zzzz" }
     */
    app.post('/api/games', (req, res) => {
        const { creatorId } = req.body;

        if (!creatorId) {
            return res.status(400).send({ message: "L'ID du créateur est requis." });
        }

        // 1. Générer un code de partie simple et unique
        const gameId = generateGameCode(); // On va créer cette fonction


        // 2. Récupérer le profil du créateur depuis la base de données
        // TODO: Pour l'instant, on crée un joueur factice. Plus tard, on le récupérera de la DB.
        const creatorProfile = { id: creatorId, name: "Hôte" }; // Version temporaire

        // 3. Créer le nouvel état de la partie
        const newGame: GameState = {
            id: gameId,
            board: createEmptyBoard(),
            players: [
                {
                    id: creatorProfile.id,
                    name: creatorProfile.name,
                    score: 0,
                    rack: [], // Le chevalet sera rempli plus tard, au démarrage
                    isActive: true
                }
            ],
            tileBag: createTileBag(),
            status: GameStatus.WAITING_FOR_PLAYERS,
            moves: [],
            turnNumber: 1,
            currentPlayerIndex: 0,
        };

        // 4. Sauvegarder la nouvelle partie en mémoire
        games.set(gameId, newGame);
        initGameConnections(gameId); // On prépare le "salon" WebSocket pour cette partie

        console.log(`✅ Nouvelle partie créée par ${creatorProfile.name}. Code: ${gameId}`);

        // 5. Renvoyer une réponse de succès au client
        res.status(201).send({ 
            message: "Partie créée avec succès !",
            gameId: gameId 
        });
    });

    // --- FIN DE L'API DE CRÉATION DE PARTIE ---
    

    // --- LOGIQUE PRINCIPALE DE CONNEXION ---

    /**
     * Ce bloc est exécuté à chaque fois qu'un nouveau client établit une connexion WebSocket.
     */
    wss.on('connection', (ws, req) => {
        // On parse l'URL pour extraire le gameId et le playerId
        const requestUrl = new URL(req.url!, `http://${req.headers.host}`);
        const gameId = requestUrl.pathname.split('/').pop()?.split('?')[0]; // Extrait l'ID de la partie de l'URL
        const playerId = requestUrl.searchParams.get('playerId'); // Extrait l'ID du joueur des paramètres de l'URL

        // Sécurité : on vérifie que les informations sont valides
        if (!gameId || !playerId || !games.has(gameId)) {
            console.log(`❌ Tentative de connexion invalide: gameId=${gameId}, playerId=${playerId}`);
            ws.close();
            return;
        }

        const gameConnections = connections.get(gameId)!;

        // On associe l'instance WebSocket au joueur
        gameConnections.set(playerId, ws);
        console.log(`Joueur ${playerId} vient de se connecter à la partie ${gameId}.`);

        // --- ENVOI DE L'ÉTAT INITIAL ---
        console.log(`Début d'envoi`);
        const initialGameState = games.get(gameId)!;
        const { stateForPlayer, playerRack } = prepareStateForPlayer(initialGameState, playerId);

        console.log(`Avant d'envoyer ${JSON.stringify(stateForPlayer)}`);
        const welcomeEvent: ServerToClientEvent = {
            type: "GAME_STATE_UPDATE",
            payload: {
                gameState: stateForPlayer,
                playerRack: playerRack
            }
        };
        ws.send(JSON.stringify(welcomeEvent));
        console.log(`Envoyé l'état initial personnalisé pour ${playerId}.\n${JSON.stringify(stateForPlayer)}`);

        /**
         * Ce bloc est exécuté à chaque fois qu'un message est reçu de ce client spécifique.
         */
        ws.on('message', (message) => {
            try {
                const event: ClientToServerEvent = JSON.parse(message.toString());
             // Aiguillage des événements reçus du client
                if (event.type === "PLAY_MOVE") {
                    const currentGame = games.get(gameId)!;
                    const { placedTiles } = event.payload;

                    // On délègue TOUTE la logique de traitement du coup au GameEngine.
                    const nextGameState = processPlayMove(currentGame, placedTiles);

                    if (nextGameState) {
                        // Si le moteur retourne un nouvel état, le coup était valide.
                        games.set(gameId, nextGameState); // Mise à jour de l'état maître.

                        // Diffusion (broadcast) de l'état mis à jour à tous les joueurs connectés.
                        console.log(`✅ Coup validé! Diffusion du nouvel état personnalisé.`);
                        nextGameState.players.forEach(player => {
                            const clientWs = gameConnections.get(player.id);
                            if (clientWs && clientWs.readyState === WebSocket.OPEN) {
                                const { stateForPlayer, playerRack } = prepareStateForPlayer(nextGameState, player.id);
                                const updateEvent: ServerToClientEvent = {
                                    type: "GAME_STATE_UPDATE",
                                    payload: { gameState: stateForPlayer, playerRack }
                                };
                                clientWs.send(JSON.stringify(updateEvent));
                                console.log(`   - Envoyé état à ${player.id}.`);
                            }
                        });
                    } else {
                        // Si le moteur retourne null, le coup était invalide.
                        console.log("❌ Coup invalide! Envoi d'un message d'erreur.");
                        const errorEvent: ServerToClientEvent = {
                            type: "ERROR",
                            payload: { message: "Votre coup est invalide." }
                        };
                        ws.send(JSON.stringify(errorEvent));
                    }
                }
                // TODO: Ajouter ici le traitement des autres types d'événements (PASS_TURN, EXCHANGE_TILES...)
            } catch (error) {
                console.error("Erreur lors du traitement du message:", error);
            }
        });

        /**
         * Ce bloc est exécuté lorsque le client ferme sa connexion.
         */
        ws.on('close', () => {
            console.log(`👋 Joueur ${playerId} déconnecté.`);
            gameConnections.delete(playerId); // On le retire de la liste des connexions actives.
        });
    });
}

// On lance le serveur
startServer().catch(error => {
    console.error("Impossible de démarrer le serveur:", error);
});