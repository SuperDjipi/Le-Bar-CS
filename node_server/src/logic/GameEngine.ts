import type { GameState, PlacedTile, Player, Board } from '../models/GameModels.js';
import { createNewBoard } from '../models/BoardModels.js';
import { isPlacementValid, isMoveConnected } from './MoveValidator.js';
import { findAllWordsFormedByMove } from './WordFinder.js';
import { isWordValid } from './Dictionary.js';
import { calculateTotalScore } from './ScoreCalculator.js';
import { drawTiles } from './TileBag.js';

/**
 * Prend un état de jeu et un coup, et retourne le nouvel état de jeu si le coup est valide.
 * Si le coup est invalide, retourne null.
 */
export function processPlayMove(
    currentGame: GameState,
    placedTiles: PlacedTile[]
): GameState | null {

    const placedPositions = placedTiles.map(p => p.position);
    const originalBoard = currentGame.board;

    // 1. CRÉER LE PLATEAU POTENTIEL
    const newBoard = createNewBoard(originalBoard, placedTiles);

    // 2. VALIDER LE COUP (toutes les règles)
    const foundWords = findAllWordsFormedByMove(newBoard, placedTiles); // Doit être ici pour le score
    const placementIsValid = isPlacementValid(originalBoard, placedPositions);
    const connectedIsValid = isMoveConnected(originalBoard, placedPositions, currentGame.turnNumber);
    const allWordsInDico = foundWords.size > 0 && Array.from(foundWords).every(word => isWordValid(word.text));
    const isMoveFullyValid = placementIsValid && connectedIsValid && allWordsInDico;

    console.log(`MOTEUR DE JEU - VALIDATION: Placement=${placementIsValid}, Connexion=${connectedIsValid}, Dico=${allWordsInDico} -> Final=${isMoveFullyValid}`);

    if (!isMoveFullyValid) {
        return null; // Le coup est invalide, on ne retourne rien.
    }

    // --- LOGIQUE DE MISE À JOUR DE L'ÉTAT DU JEU (Étape 7) ---

    // a) Calcule le score et l'ajoute au joueur
    const score = calculateTotalScore(foundWords, newBoard, placedPositions);
    const currentPlayerId = currentGame.players[currentGame.currentPlayerIndex]!.id;

    const updatedPlayers = currentGame.players.map(player => {
        if (player.id === currentPlayerId) {
            return { ...player, score: player.score + score };
        }
        return player;
    });

    // b) Verrouille les tuiles
    const lockedBoard = JSON.parse(JSON.stringify(newBoard));
    placedPositions.forEach(pos => {
        if (lockedBoard[pos.row][pos.col].tile) {
            lockedBoard[pos.row][pos.col].isLocked = true;
        }
    });

    // c) Pioche de nouvelles tuiles
    let currentTileBag = currentGame.tileBag;
    const { drawnTiles, newBag } = drawTiles(currentTileBag, placedTiles.length);
    currentTileBag = newBag;

    const finalPlayers = updatedPlayers.map(player => {
        if (player.id === currentPlayerId) {
            const playedTileIds = new Set(placedTiles.map(p => p.tile.id));
            const remainingRack = player.rack.filter(t => !playedTileIds.has(t.id));
            const newRack = [...remainingRack, ...drawnTiles];
            return { ...player, rack: newRack };
        }
        return player;
    });

    // d) Passe au joueur suivant
    const nextPlayerIndex = (currentGame.currentPlayerIndex + 1) % currentGame.players.length;

    // On assemble le nouvel état de jeu officiel
    const nextGameState: GameState = {
        ...currentGame,
        board: lockedBoard,
        players: finalPlayers,
        tileBag: currentTileBag,
        moves: [],
        turnNumber: currentGame.turnNumber + 1,
        currentPlayerIndex: nextPlayerIndex,
    };

    return nextGameState;
}

// TODO: Ajouter ici d'autres fonctions comme processPassTurn, processXchgeTiles, etc.