package maaartin.game.ultimatoe;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import lombok.Getter;
import lombok.NonNull;

import com.google.common.math.IntMath;

@Immutable public final class UltimatoeBoard {
	private UltimatoeBoard(int data) {
		this.data = data;
		winner = computeWinner(data);
		int possibilities = 0;
		if (winner.isNone()) {
			for (final UltimatoePlayer player : UltimatoePlayer.values()) {
				if (player.isNone()) continue;
				for (int i=0; i<9; ++i) {
					if (!getPlayer(data, i).isNone()) continue;
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
			if (player.isNone()) continue;
			if (player != getPlayer(data, winningSet[1])) continue;
			if (player != getPlayer(data, winningSet[2])) continue;
			return player;
		}
		return UltimatoePlayer.NONE;
	}

	private static UltimatoePlayer getPlayer(int data, int index) {
		final int x = (data >> (2*index)) & 3;
		return UltimatoePlayer.values()[x];
	}

	private static int childData(int data, int index, UltimatoePlayer player) {
		return data + (player.ordinal() << (2*index));
	}

	@Override public boolean equals(Object obj) {//TODO
		if (!(obj instanceof UltimatoeBoard)) return false;
		final UltimatoeBoard that = (UltimatoeBoard) obj;
		return this.data == that.data;
	}

	@Override public int hashCode() {
		return data;
	}

	@Override public String toString() {
		final StringBuilder result = new StringBuilder();
		for (int i=0; i<9; ++i) {
			result.append(getPlayer(data, i).toChar());
			if (i==2 || i==5) result.append("\n");
		}
		return result.toString();
	}

	@Nullable UltimatoeBoard play(int index, UltimatoePlayer player) {
		return children[childIndex(index, player)];
	}

	UltimatoePlayer get(int index) {
		return getPlayer(data, index);
	}

	boolean isFinished() {
		return possibilities == 0;
	}

	private int childIndex(int index, UltimatoePlayer player) {
		return 2*index + player.ordinal() - 1;
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
