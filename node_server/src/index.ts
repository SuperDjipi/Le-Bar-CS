import express from 'express';
import { WebSocketServer, WebSocket } from 'ws';
import type { ClientToServerEvent, ServerToClientEvent } from './models/GameEvents.js';
import { findAllWordsFormedByMove } from './logic/WordFinder.js';
import { isPlacementValid, isMoveConnected } from './logic/MoveValidator.js';
import type { GameState, Tile, Player, PlacedTile } from './models/GameModels.js';
import { GameStatus } from './models/GameModels.js';
import { createTileBag, drawTiles } from './logic/TileBag.js';
import { calculateTotalScore } from './logic/ScoreCalculator.js';
import { createEmptyBoard, createNewBoard } from './models/BoardModels.js';
import { processPlayMove } from './logic/GameEngine.js';


import { isWordValid } from './logic/Dictionary.js';

// --- Gestion des parties en mémoire ---
const games = new Map<string, GameState>();
// Associe un gameId à un Set de WebSockets (tous les joueurs de la partie)
// IMPORTANT : Nous devons savoir quel joueur correspond à quelle connexion WebSocket.
const connections = new Map<string, Map<string, WebSocket>>(); // gameId -> (playerId -> WebSocket)

function initGameConnections(gameId: string) {
    if (!connections.has(gameId)) {
        connections.set(gameId, new Map<string, WebSocket>());
    }
}
initGameConnections('123'); // Pour notre partie de test

// --- Création d'une partie de test ---
function createTestGame(): GameState {
    let tileBag = createTileBag();

    // Distribuer 7 tuiles au joueur 1
    const player1Draw = drawTiles(tileBag, 7);
    const player1: Player = { id: 'player1', name: 'Joueur 1', score: 0, rack: player1Draw.drawnTiles, isActive: true };
    tileBag = player1Draw.newBag;

    // Distribuer 7 tuiles au joueur 2
    const player2Draw = drawTiles(tileBag, 7);
    const player2: Player = { id: 'player2', name: 'Joueur 2', score: 0, rack: player2Draw.drawnTiles, isActive: false };
    tileBag = player2Draw.newBag;

    return {
        id: '123',
        board: createEmptyBoard(),
        players: [player1, player2],
        tileBag: tileBag, // On stocke le reste de la pioche
        moves: [],
        status: GameStatus.PLAYING,
        turnNumber: 1,
        currentPlayerIndex: 0,
    };
}
games.set('123', createTestGame());
console.log("- (Partie de test '123' créée.)");
// --- Nouvelle fonction pour personnaliser l'état ---
function prepareStateForPlayer(gameState: GameState, playerId: string
): { stateForPlayer: GameState, playerRack: Tile[] } {
    let playerRack: Tile[] = [];

    // On crée une version du GameState où tous les chevalets sont vides...
    const stateForPlayer: GameState = {
        ...gameState,
        players: gameState.players.map(p => {
            if (p.id === playerId) {
                // ...sauf pour le joueur concerné, on garde son chevalet pour l'envoyer séparément.
                playerRack = p.rack;
            }
            return { ...p, rack: [] }; // On vide le chevalet pour les autres
        }),
        tileBag: [] // On ne révèle jamais la pioche au client
    };

    return { stateForPlayer, playerRack };
}
// --- Démarrage du serveur ---
const app = express();
const port = 8080;
const server = app.listen(port, () => {
    console.log(`✅ Serveur  démarré et à l'écoute sur http://localhost:${port}`);
});
const wss = new WebSocketServer({ server });

// --- Logique principale de connexion ---
wss.on('connection', (ws, req) => {
    const gameId = req.url?.split('/').pop();
    if (!gameId || !games.has(gameId)) {
        console.log(`❌ Tentative de connexion à une partie invalide: ${gameId}`);
        ws.close();
        return;
    }
    const gameConnections = connections.get(gameId)!;

    // Pour les tests, on assigne un playerId basé sur l'ordre de connexion.
    const playerId = `player${gameConnections.size + 1}`;
    if (gameConnections.size >= games.get(gameId)!.players.length) {
        console.log("Trop de joueurs, connexion refusée.");
        ws.close();
        return;
    }
    gameConnections.set(playerId, ws); // On associe le joueur à sa connexion    
    console.log(`Joueur ${playerId} vient de se connecter à la partie ${gameId}.`);

    // Envoyer l'état actuel du jeu au joueur qui vient de se connecter
    const initialGameState = games.get(gameId)!;

    // --- ON UTILISE NOTRE NOUVELLE FONCTION ---
    const { stateForPlayer, playerRack } = prepareStateForPlayer(initialGameState, playerId);
    const welcomeEvent: ServerToClientEvent = {
        type: "GAME_STATE_UPDATE",
        payload: {
            gameState: stateForPlayer,
            playerRack: playerRack // On envoie le chevalet du joueur dans le champ dédié
        }
    };
    ws.send(JSON.stringify(welcomeEvent));
    console.log(`Envoyé l'état initial personnalisé pour ${playerId}.`);

    ws.on('message', (message) => {
        try {
            const event: ClientToServerEvent = JSON.parse(message.toString());

            if (event.type === "PLAY_MOVE") {
                const currentGame = games.get(gameId)!;
                const { placedTiles } = event.payload;
                // --- On délègue TOUT le travail au moteur de jeu ---
                const nextGameState = processPlayMove(currentGame, placedTiles);

                if (nextGameState) {
                    // Le coup était valide, le moteur a retourné le nouvel état.

                    // On met à jour l'état officiel sur le serveur
                    games.set(gameId, nextGameState);

                    // On prépare et on diffuse le nouvel état à tous les joueurs
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
                    // Le coup était invalide, le moteur a retourné null.
                    console.log("❌ Coup invalide! Envoi d'un message d'erreur.");
                    const errorEvent: ServerToClientEvent = {
                        type: "ERROR",
                        payload: { message: "Votre coup est invalide." }
                    };
                    ws.send(JSON.stringify(errorEvent));
                }
            }

        } catch (error) {
            console.error("Erreur lors du traitement du message:", error);
        }
    });

    ws.on('close', () => {
        console.log(`👋 Joueur ${playerId} déconnecté.`);
        gameConnections.delete(playerId);
    });
});
