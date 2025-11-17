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
import type { GameState, Tile, Player, PlacedTile } from './models/GameModels.js';
import { GameStatus } from './models/GameModels.js';
// Import des modules de logique métier
import { createTileBag, drawTiles } from './logic/TileBag.js';
import { createEmptyBoard, createNewBoard } from './models/BoardModels.js';
import { processPlayMove } from './logic/GameEngine.js'; // Le moteur de jeu principal
import { URL } from 'url'; // Utile pour parser l'URL de connexion

// Profil joueur
interface UserProfile {
    id: string; // C'est le playerId
    name: string;
    // avatarUrl: string; // Pour plus tard
}
const userProfiles = new Map<string, UserProfile>(); // Notre "base de données" d'utilisateurs


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
initGameConnections('123'); // Pour notre partie de test

// --- CRÉATION D'UNE PARTIE DE TEST AU DÉMARRAGE ---
// (Cette section est utile pour le développement, mais devrait être remplacée par une API de création de partie plus tard)
function createTestGame(): GameState {
    let tileBag = createTileBag();
    const player1Draw = drawTiles(tileBag, 7);
    const player1: Player = { id: 'a8040f2b-ba4b-44ed-889a-e9b27f118f32', name: '-Alpha', score: 0, rack: player1Draw.drawnTiles, isActive: true };
    tileBag = player1Draw.newBag;

    const player2Draw = drawTiles(tileBag, 7);
    const player2: Player = { id: 'player2', name: 'Joueur 2', score: 0, rack: player2Draw.drawnTiles, isActive: false };
    tileBag = player2Draw.newBag;

    return {
        id: '123',
        board: createEmptyBoard(),
        players: [player1, player2],
        tileBag: tileBag,
        moves: [],
        status: GameStatus.PLAYING,
        turnNumber: 1,
        currentPlayerIndex: 0,
    };
}
games.set('123', createTestGame());
console.log("- (Partie de test '123' créée.)");

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
const app = express();
const port = 8080;
// On lance le serveur HTTP Express...
const server = app.listen(port, () => {
    console.log(`✅ Serveur démarré et à l'écoute sur http://localhost:${port}`);
});
// ...et on attache le serveur WebSocket à ce serveur HTTP.
const wss = new WebSocketServer({ server });

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
    const initialGameState = games.get(gameId)!;
    const { stateForPlayer, playerRack } = prepareStateForPlayer(initialGameState, playerId);
    const welcomeEvent: ServerToClientEvent = {
        type: "GAME_STATE_UPDATE",
        payload: {
            gameState: stateForPlayer,
            playerRack: playerRack
        }
    };
    ws.send(JSON.stringify(welcomeEvent));
    console.log(`Envoyé l'état initial personnalisé pour ${playerId}.`);

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
if (event.type === "REGISTER_PROFILE") {
    const { name } = event.payload;
    if (name) {
        const newProfile: UserProfile = { id: playerId, name }; // 'playerId' vient de la connexion
        userProfiles.set(playerId, newProfile);
        console.log(`Profil enregistré/mis à jour pour ${playerId}: ${name}`);
        // On peut renvoyer une confirmation au client
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