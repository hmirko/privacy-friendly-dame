/*
 This file is part of Privacy Friendly Dame.

 Privacy Friendly Dame is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly Dame is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendlydame.game;

import java.util.ArrayList;

 // data for complete board state
public class Board {
    //private CheckersGame checkersGame;
    private Piece board[][];

    public boolean isGameSquare(int x, int y) {
        // within 8x8 dimensions and is odd-square
        return (x >= 0 && y >= 0 && x < 8 && y < 8 && (x + y) % 2 > 0);
    }

    public boolean isGameSquare(Position pos) {
        return isGameSquare(pos.x, pos.y);
    }


    //
    private Position[] RED_DIRECTIONS = new Position[]{new Position(-1, 1), new Position(1, 1)};
    private Position[] BLACK_DIRECTIONS = new Position[]{new Position(-1, -1), new Position(1, -1)};
    private Position[] BOTH_DIRECTIONS = new Position[]{new Position(-1, 1), new Position(1, 1), new Position(-1, -1), new Position(1, -1)};
    private Position[] NO_DIRECTIONS = new Position[]{};

    private Position[] getNeighbors(int color, boolean king) {
        if (king) {
            return BOTH_DIRECTIONS;
        } else if (color == CheckersGame.WHITE) {
            return RED_DIRECTIONS;
        } else if (color == CheckersGame.BLACK) {
            return BLACK_DIRECTIONS;
        } else {
            return BOTH_DIRECTIONS;
        }
    }


    // create new board
    public Board(CheckersGame checkersGame) {
        //this.checkersGame = checkersGame;
        board = new Piece[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int side = (y < 3) ? CheckersGame.WHITE : (y > 4) ? CheckersGame.BLACK : 0;
                boolean validSquare = this.isGameSquare(x, y);
                if (side != CheckersGame.NONE && validSquare) {
                    board[x][y] = new Piece(side, false);
                } else {
                    board[x][y] = null;
                }
            }
        }
    }

    // create from existing positions
    public Board(int[][] positions) {
        //this.checkersGame = checkersGame;
        board = new Piece[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (positions[x][y] > CheckersGame.NONE) {
                    int side = positions[x][y] % CheckersGame.KINGED;
                    boolean kinged = positions[x][y] > CheckersGame.KINGED;
                    board[x][y] = new Piece(side, kinged);
                } else {
                    board[x][y] = null;
                }
            }
        }
    }

    // save positions as int[][]
    public int[][] saveBoard() {
        int result[][] = new int[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (board[x][y] != null) {
                    Piece piece = board[x][y];
                    result[x][y] = piece.getColor();
                    if (piece.isKing()) {
                        result[x][y] += CheckersGame.KINGED;
                    }
                } else {
                    result[x][y] = CheckersGame.NONE;
                }
            }
        }
        return result;
    }

    // get a piece on the board
    public Piece getPiece(int x, int y) {
        return (isGameSquare(x, y) ? board[x][y] : null);
    }
    public Piece getPiece(Position pos) {
        return getPiece(pos.x, pos.y);
    }

    // find a piece on the board
    public Position getPosition(Piece piece) {
        int x = 0, y = 0;
        for (; x < 8; x++) {
            for (; y < 8; y++) {
                if (getPiece(x, y) == piece) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    //
    public ArrayList<Move> getCaptures(Position start, boolean allowAnyMove)
    {
        ArrayList<Move> base = new ArrayList<>();
        Piece piece = getPiece(start);
        int color = piece.getColor();
        boolean isKing = piece.isKing();

        // add jumps in each direction
        Position[] directions = getNeighbors(color, isKing);
        for (Position dir : directions) {
            // if the current piece is a king the search range is increased by one in each iteration
            if (isKing) {
                for (int i = 0; i < 8; i++) {
                    Position target = start.plus(dir);
                    for (int j = 0; j < i; j++) {
                        target = target.plus(dir);
                    }
                    Position dest = target.plus(dir);
                    Piece targetPiece = getPiece(target);
                    Piece destPiece = getPiece(dest);

                    // look for a valid landing space with an opposing piece in-between
                    if (isGameSquare(dest) && destPiece == null &&
                            targetPiece != null &&
                            targetPiece.getColor() != color) {
                        Move newMove = new Move(start);
                        newMove.add(dest);
                        newMove.addCapture(target);
                        base.add(newMove);
                    }
                    // if 2 pieces are back-to-back or the position is off-board
                    // the search is terminated in the current direction
                    else if ((targetPiece != null && destPiece != null) || !isGameSquare(target)) {
                        break;
                    }
                }
            }
            else {
                Position target = start.plus(dir);
                Position dest = target.plus(dir);
                Piece targetPiece = getPiece(target);
                Piece destPiece = getPiece(dest);

                // look for a valid landing space with an opposing piece in-between
                if (isGameSquare(dest) && destPiece == null &&
                        targetPiece != null &&
                        targetPiece.getColor() != color) {
                    Move newMove = new Move(start);
                    newMove.add(dest);
                    newMove.addCapture(target);
                    base.add(newMove);
                }
            }
        }

        // find longest for each jump choice
        return getCaptures(start, base, allowAnyMove);
    }

    //
    public ArrayList<Move> getCaptures(Position start, ArrayList<Move> expand, boolean allowAnyMove)
    {
        ArrayList<Move> finalCaptures = new ArrayList<>();
        ArrayList<Move> furtherCaptures = new ArrayList<>();

        Piece piece = getPiece(start);
        int color = piece.getColor();
        boolean isKing = piece.isKing();

        // create longer moves from existing ones
        for (Move move : expand) {
            Position[] directions = getNeighbors(color, isKing || move.kings);
            Position current = move.end();
            boolean continues = false;
            for (Position dir : directions)
            {
                Position target = current.plus(dir);
                Position dest = target.plus(dir);
                Piece targetPiece = getPiece(target);
                Piece destPiece = getPiece(dest);

                // look for a valid landing space with an opposing piece in-between
                if (isGameSquare(dest) && destPiece == null &&
                        targetPiece != null &&
                        targetPiece.getColor() != color) {
                    // check that the 'opposing piece' hasn't been captured in this move sequence yet
                    boolean valid = true;
                    for (Position captured : move.captures) {
                        if (captured.equals(target)) {
                            valid = false;
                            break;
                        }
                    }
                    // valid piece to capture
                    if (valid) {
                        Move newMove = new Move(move);
                        newMove.add(dest);
                        newMove.addCapture(target);
                        furtherCaptures.add(newMove);
                        continues = true;
                    }
                }
            }

            // only add this move if there are no longer alternatives
            if (!continues || allowAnyMove) {
                finalCaptures.add(move);
            }
        }

        if (furtherCaptures.size() > 0) {
            furtherCaptures = getCaptures(start, furtherCaptures, allowAnyMove);
        }
        finalCaptures.addAll(furtherCaptures);

        return finalCaptures;
    }

    // get a set of possible moves from a place on the board
    public ArrayList<Move> getMoves(Position start, boolean allowAnyMove) {
        Piece piece = getPiece(start);

        ArrayList<Move> immediateMoves = new ArrayList<>();

        // check neighboring positions
        Position[] neighbors = getNeighbors(piece.getColor(), piece.isKing());
        for (Position pos : neighbors) {
            // check each square if it is free to move to
            if (piece.isKing()) {
                for (int i = 0; i < 8; i++) {
                    Position dest = start.plus(pos);
                    for (int j = 0; j < i; j++) {
                        dest = dest.plus(pos);
                    }
                    Piece destPiece = getPiece(dest);

                    // add current square if square is on board and no other piece is on that position
                    if (isGameSquare(dest) && destPiece == null) {
                        Move newMove = new Move(start);
                        newMove.add(dest);
                        immediateMoves.add(newMove);
                    }
                    // else position is off board or another piece occupies the position
                    // so no further checking is required in this direction
                    else {
                        break;
                    }
                }
            }
            else {
                Position dest = start.plus(pos);
                Piece destPiece = getPiece(dest);

                if (isGameSquare(dest) && destPiece == null) {
                    Move newMove = new Move(start);
                    newMove.add(dest);
                    immediateMoves.add(newMove);
                }
            }
        }

        ArrayList<Move> captures = getCaptures(start, allowAnyMove);
        immediateMoves.addAll(captures);
        return immediateMoves;
    }


    // get possible moves for current player
    public Move[] getMoves(int turn, boolean allowAnyMove) {
        ArrayList<Move> finalMoves;
        ArrayList<Move> potentialMoves = new ArrayList<>();
        ArrayList<Position> startingPositions = new ArrayList<>();

        // add moves for each matching piece
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece piece = getPiece(x, y);
                if (piece != null && piece.getColor() == turn) {
                    Position start = new Position(x, y);
                    potentialMoves.addAll(
                            getMoves(start, allowAnyMove)
                    );
                }
            }
        }

        // check if non-jumping moves need to be removed
        finalMoves = potentialMoves;
        if (allowAnyMove == false) {
            boolean areCaptures = false;
            for (Move sequence : potentialMoves) {
                if (sequence.captures.size() > 0) {
                    areCaptures = true;
                    break;
                }
            }
            if (areCaptures) {
                finalMoves = new ArrayList<>();
                for (Move sequence : potentialMoves) {
                    if (sequence.captures.size() > 0) {
                        finalMoves.add(sequence);
                    }
                }
            }
        }

        // return choices as a sequence of positions
        return finalMoves.toArray(new Move[finalMoves.size()]);
    }

    // carry out a move sequence
    public void makeMove(Move move) {
        Position start = move.start();
        Position end = move.end();
        Piece piece = getPiece(start);
        int otherColor = (piece.getColor() == CheckersGame.WHITE) ? CheckersGame.BLACK : CheckersGame.WHITE;
        // clear visited positions
        for (Position pos : move.positions) {
            board[pos.x][pos.y] = null;
        }
        // clear captured positions and decrease piece count
        for (Position cap : move.captures) {
            board[cap.x][cap.y] = null;
        }
        // place at end position
        board[end.x][end.y] = piece;
        // check if piece was kinged
        if (move.kings) {
            piece.makeKing();
        }
    }

    public int pseudoScore() {
        int score = 0;
        int blackPieces = 0;
        int redPieces = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece piece = board[x][y];
                if (piece != null) {
                    int weight = piece.isKing() ? 5 : 2;
                    if (piece.getColor() == CheckersGame.WHITE) {
                        weight *= -1;
                        redPieces++;
                    } else {
                        blackPieces++;
                    }
                    score += weight;
                }
            }
        }
        if (blackPieces == 0) {
            score = -1000;
        } else if (redPieces == 0) {
            score = 1000;
        }

        return score;
    }
}
