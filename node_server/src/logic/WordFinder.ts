import type { Board, Position, Tile, FoundWord } from '../models/GameModels.js';
import { Direction } from '../models/GameModels.js';

const BOARD_SIZE = 15;

function getTile(board: Board, position: Position): Tile | null {
    if (position.row >= 0 && position.row < BOARD_SIZE && position.col >= 0 && position.col < BOARD_SIZE) {
        return board[position.row]![position.col]!.tile;
    }
    return null;
}

export function findHorizontalWord(board: Board, startPos: Position): FoundWord | null {
    if (getTile(board, startPos) === null) {
        return null;
    }

    const row = startPos.row;

    // 1. Trouver la colonne de début
    let startIndex = startPos.col;
    while (startIndex > 0 && getTile(board, { row, col: startIndex - 1 }) !== null) {
        startIndex--;
    }

    // 2. Trouver la colonne de fin
    let endIndex = startPos.col;
    while (endIndex < BOARD_SIZE - 1 && getTile(board, { row, col: endIndex + 1 }) !== null) {
        endIndex++;
    }

    if (startIndex === endIndex) {
        return null; // Mot d'une seule lettre, non connecté
    }

    // 3. Construire le mot
    let wordText = "";
    for (let col = startIndex; col <= endIndex; col++) {
        const tile = getTile(board, { row, col });
        if (!tile) return null; // Sécurité
        wordText += tile.letter;
    }

    return { text: wordText, direction: Direction.HORIZONTAL };
}

export function findVerticalWord(board: Board, startPos: Position): FoundWord | null {
    if (getTile(board, startPos) === null) {
        return null;
    }

    const col = startPos.col;

    // 1. Trouver la ligne de début (en remontant)
    let startIndex = startPos.row;
    while (startIndex > 0 && getTile(board, { row: startIndex - 1, col }) !== null) {
        startIndex--;
    }

    // 2. Trouver la ligne de fin (en descendant)
    let endIndex = startPos.row;
    while (endIndex < BOARD_SIZE - 1 && getTile(board, { row: endIndex + 1, col }) !== null) {
        endIndex++;
    }

    if (startIndex === endIndex) {
        return null;
    }

    // 3. Construire le mot
    let wordText = "";
    for (let row = startIndex; row <= endIndex; row++) {
        const tile = getTile(board, { row, col });
        if (!tile) return null;
        wordText += tile.letter;
    }

    return { text: wordText, direction: Direction.VERTICAL };
}

export function findAllWordsFormedByMove(board: Board, placedTiles: { position: Position, tile: Tile }[]): Set<FoundWord> {
    if (placedTiles.length === 0) {
        return new Set();
    }

    const allFoundWords = new Set<FoundWord>();

    for (const placed of placedTiles) {
        const hWord = findHorizontalWord(board, placed.position);
        if (hWord) {
            allFoundWords.add(hWord);
        }

        const vWord = findVerticalWord(board, placed.position);
        if (vWord) {
            allFoundWords.add(vWord);
        }
    }

    // Convertir le Set en tableau et le re-convertir en Set pour éliminer les doublons par valeur
    const uniqueWords = Array.from(allFoundWords).map(w => JSON.stringify(w));
    return new Set(uniqueWords.map(s => JSON.parse(s)));
}