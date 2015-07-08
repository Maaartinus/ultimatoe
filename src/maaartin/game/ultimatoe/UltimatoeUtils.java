package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.experimental.UtilityClass;

import com.google.common.collect.ImmutableList;

import maaartin.game.GamePlayer;

@UtilityClass public final class UltimatoeUtils {
	static String indexesToMoveString(int majorIndex, int minorIndex) {
		checkArgument(0<=minorIndex && minorIndex<9);
		checkArgument(0<=majorIndex && majorIndex<9);
		final int x = 3 * (majorIndex%3) + (minorIndex%3);
		final int y = 3 * (majorIndex/3) + (minorIndex/3);
		return coordinatesToMoveString(x, y);
	}

	static int stringToMajorIndex(String move) {
		checkArgument(move.length() == 2);
		final int y = move.charAt(0) - '0';
		checkArgument(0<=y && y<9);
		final int x = move.charAt(1) - '0';
		checkArgument(0<=x && x<9);
		return 3 * (y/3) + (x/3);
	}

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

	static char scoreToWinner(double score) {
		return score>0 ? PLAYER_0 : score<0 ? PLAYER_1 : BORDER;
	}

	static final ImmutableList<GamePlayer<Ultimatoe>> PLAYERS =
			ImmutableList.<GamePlayer<Ultimatoe>>copyOf(UltimatoePlayer.values());

	final static char PLAYER_0 = UltimatoePlayer.O.toChar();
	final static char PLAYER_1 = UltimatoePlayer.X.toChar();
	final static char PLAYABLE = '·';
	final static char NON_PLAYABLE = ' ';
	final static char BORDER = '*';
}
