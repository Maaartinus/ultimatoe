package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.experimental.UtilityClass;

import maaartin.game.StandardPlayer;

@UtilityClass public final class UltimatoeUtils {
	/**
	 * Convert a pair of indexes to a string representation of a move,
	 * which can be passed to {@link Ultimatoe#play(String)}.
	 *
	 * <p>Both indexes run from left to right, and then top to bottom.
	 *
	 * @param majorIndex The number of a board ,in range 0 to 8.
	 * @param minorIndex The number of a field of a board, in range 0 to 8.
	 * @return
	 */
	static String indexesToMoveString(int majorIndex, int minorIndex) {
		checkArgument(0<=minorIndex && minorIndex<9);
		checkArgument(0<=majorIndex && majorIndex<9);
		final int x = 3 * (majorIndex%3) + (minorIndex%3);
		final int y = 3 * (majorIndex/3) + (minorIndex/3);
		return coordinatesToMoveString(x, y);
	}

	/** A part of the inverse function to {@link #indexesToMoveString(int, int)}. */
	static int stringToMajorIndex(String move) {
		checkArgument(move.length() == 2);
		final int y = move.charAt(0) - '0';
		checkArgument(0<=y && y<9);
		final int x = move.charAt(1) - '0';
		checkArgument(0<=x && x<9);
		return 3 * (y/3) + (x/3);
	}

	/** A part of the inverse function to {@link #indexesToMoveString(int, int)}. */
	static int stringToMinorIndex(String move) {
		checkArgument(move.length() == 2);
		final int y = move.charAt(0) - '0';
		checkArgument(0<=y && y<9);
		final int x = move.charAt(1) - '0';
		checkArgument(0<=x && x<9);
		return 3 * (y%3) + (x%3);
	}

	static String coordinatesToMoveString(int x, int y) {
		checkArgument(0<=x && x<9);
		checkArgument(0<=y && y<9);
		return y + "" + x;
	}

	static final int[][] WINNING_SETS = {
		{0, 1, 2}, // upper row
		{3, 4, 5}, // middle row
		{6, 7, 8}, // lower row
		{0, 3, 6}, // left column
		{1, 4, 7}, // middle column
		{2, 5, 8}, // right column
		{0, 4, 8}, // main diagonal
		{2, 4, 6}, // antidiagonal
	};

	final static char PLAYER_0 = StandardPlayer.PLAYER_O.toChar();
	final static char PLAYER_1 = StandardPlayer.PLAYER_X.toChar();
	final static char PLAYABLE = '·';
	final static char NON_PLAYABLE = ' ';
	final static char BORDER = '*';
}
