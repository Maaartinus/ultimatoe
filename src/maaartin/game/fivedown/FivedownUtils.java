package maaartin.game.fivedown;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.experimental.UtilityClass;

import maaartin.game.GamePlayer;
import maaartin.game.StandardPlayer;

@UtilityClass public final class FivedownUtils {
	static String coordinatesToMoveString(int x, int y) {
		return intToDigit(y) + "" + intToDigit(x);
	}

	static int stringToX(String move) {
		checkArgument(move.length() == 2);
		return digitToInt(move.charAt(1));
	}

	static int stringToY(String move) {
		checkArgument(move.length() == 2);
		return digitToInt(move.charAt(0));
	}

	private static char intToDigit(int z) {
		checkArgument(0<=z && z<36);
		return (char) (z<10 ? z + '0' : z + 'A' - 10);
	}

	private static int digitToInt(char c) {
		return c <= '9' ? c - '0' : c - 'A' + 10;
	}

	static char toChar(GamePlayer p, boolean isPlayable) {
		switch ((StandardPlayer) p) {
			case PLAYER_O: return isPlayable ? PLAYER_0_PLAYABLE : PLAYER_0_NON_PLAYABLE;
			case PLAYER_X: return isPlayable ? PLAYER_1_PLAYABLE : PLAYER_1_NON_PLAYABLE;
			case NOBODY: return isPlayable ? EMPTY_PLAYABLE : EMPTY_NON_PLAYABLE;
		}
		throw new RuntimeException("impossible");
	}

	static boolean isPlayable(char c) {
		return PLAYABLE_STRING.indexOf(c) > -1;
	}

	static StandardPlayer getPlayer(char c) {
		if (Character.toUpperCase(c) == StandardPlayer.PLAYER_X.toChar()) return StandardPlayer.PLAYER_X;
		if (Character.toUpperCase(c) == StandardPlayer.PLAYER_O.toChar()) return StandardPlayer.PLAYER_O;
		return StandardPlayer.NOBODY;
	}

	final static char PLAYER_0_PLAYABLE = Character.toUpperCase(StandardPlayer.PLAYER_O.toChar());
	final static char PLAYER_1_PLAYABLE = Character.toUpperCase(StandardPlayer.PLAYER_X.toChar());
	final static char EMPTY_PLAYABLE = '·';
	final static char PLAYER_0_NON_PLAYABLE = Character.toLowerCase(StandardPlayer.PLAYER_O.toChar());
	final static char PLAYER_1_NON_PLAYABLE = Character.toLowerCase(StandardPlayer.PLAYER_X.toChar());
	final static char EMPTY_NON_PLAYABLE = ' ';

	private static final String PLAYABLE_STRING = PLAYER_0_PLAYABLE + "" + PLAYER_1_PLAYABLE + "" + EMPTY_PLAYABLE;
}
