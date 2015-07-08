package maaartin.game.ultimatoe;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.math.IntMath;

import maaartin.game.GamePlayer;

@Immutable @EqualsAndHashCode(of="data") public final class UltimatoeBoard {
	private UltimatoeBoard(int data) {
		this.data = data;
		winner = computeWinner(data);
		int possibilities = 0;
		if (winner.isDummy()) {
			for (final GamePlayer<Ultimatoe> player : UltimatoeUtils.PLAYERS) {
				if (player.isDummy()) continue;
				for (int i=0; i<9; ++i) {
					if (!getPlayer(data, i).isDummy()) continue;
					final int childData = childData(data, i, player);
					children[childIndex(i, player)] = getBoard(childData);
					possibilities |= 1 << i;
				}
			}
		}
		this.possibilities = possibilities;
	}

	private static UltimatoeBoard getBoard(int data) {
		if (ALL_BOARDS[data] == null) ALL_BOARDS[data] = new UltimatoeBoard(data);
		return ALL_BOARDS[data];
	}

	private static UltimatoePlayer computeWinner(int data) {
		for (final int[] winningSet : WINNING_SETS) {
			final UltimatoePlayer player = getPlayer(data, winningSet[0]);
			if (player.isDummy()) continue;
			if (player != getPlayer(data, winningSet[1])) continue;
			if (player != getPlayer(data, winningSet[2])) continue;
			return player;
		}
		return UltimatoePlayer.NONE;
	}

	private static UltimatoePlayer getPlayer(int data, int index) {
		final int x = (data >> (2*index)) & 3;
		return x==1 ? UltimatoePlayer.O : x==2 ? UltimatoePlayer.X : UltimatoePlayer.NONE;
	}

	private static int childData(int data, int index, GamePlayer<Ultimatoe> player) {
		return data + ((player.ordinal() + 1) << (2*index));
	}

	@Override public String toString() {
		final StringBuilder result = new StringBuilder();
		for (int i=0; i<9; ++i) {
			result.append(getPlayer(data, i).toChar());
			if (i==2 || i==5) result.append("\n");
		}
		return result.toString();
	}

	@Nullable UltimatoeBoard play(int index, GamePlayer<Ultimatoe> player) {
		return children[childIndex(index, player)];
	}

	UltimatoePlayer get(int index) {
		return getPlayer(data, index);
	}

	boolean isPlayable(int index) {
		return (possibilities & (1<<index)) != 0;
	}

	boolean isFinished() {
		return possibilities == 0;
	}

	private int childIndex(int index, GamePlayer<Ultimatoe> player) {
		return 2*index + player.ordinal();
	}

	static final int[][] WINNING_SETS = {
		{0, 1, 2},
		{3, 4, 5},
		{6, 7, 8},
		{0, 3, 6},
		{1, 4, 7},
		{2, 5, 8},
		{0, 4, 8},
		{2, 4, 6},
	};

	private static final UltimatoeBoard[] ALL_BOARDS = new UltimatoeBoard[IntMath.pow(4, 9)];
	static final UltimatoeBoard EMPTY_BOARD = getBoard(0);

	private final int data;
	@Getter @NonNull private final UltimatoePlayer winner;
	@Getter private final int possibilities;
	private final UltimatoeBoard[] children = new UltimatoeBoard[2*9];
}
