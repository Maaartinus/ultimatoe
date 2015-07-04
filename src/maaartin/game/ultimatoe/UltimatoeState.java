package maaartin.game.ultimatoe;

import java.util.Arrays;
import java.util.Random;

import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableBiMap;

import maaartin.game.IGameState;

@RequiredArgsConstructor(access=AccessLevel.PRIVATE) @EqualsAndHashCode
public final class UltimatoeState implements IGameState<UltimatoeState> {
	static final class ToStringHelper {
		ToStringHelper(UltimatoeState state) {
			final char[][] result = new char[11][11];
			for (int i=0; i<11; ++i) Arrays.fill(result[i], UltimatoeFieldState.BORDER.toChar());

			for (int y1=0; y1<3; ++y1) {
				for (int x1=0; x1<3; ++x1) {
					final int majorIndex = x1 + 3*y1;
					final UltimatoeBoard board = state.boards[majorIndex];
					for (int y0=0; y0<3; ++y0) {
						for (int x0=0; x0<3; ++x0) {
							final UltimatoePlayer player = board.get(x0 + 3*y0);
							final char c = computeChar(state, majorIndex, player);
							result[4*y1 + y0][4*x1 + x0] = c;
						}
					}
				}
			}

			final StringBuilder sb = new StringBuilder();
			for (int i=0; i<11; ++i) {
				if (i>0) sb.append("\n");
				sb.append(result[i]);
			}
			toString = sb.toString();
		}

		private char computeChar(UltimatoeState state, int majorIndex, UltimatoePlayer player) {
			if (!player.isNone()) return player.toChar();
			if (state.isPlayable(majorIndex)) return UltimatoeFieldState.PLAYABLE.toChar();
			return UltimatoeFieldState.NON_PLAYABLE.toChar();
		}

		private final String toString;
	}

	@Override public String toString() {
		return new ToStringHelper(this).toString;
	}

	@Override public ImmutableBiMap<UltimatoeState, String> children() {
		final ImmutableBiMap.Builder<UltimatoeState, String> result = ImmutableBiMap.builder();
		for (int i=0; i<9; ++i) {
			if (!isPlayable(i)) continue;
			for (int j=0; j<9; ++j) {
				final UltimatoeState child = play(i, j);
				if (child==null) continue;
				result.put(child, i + "" + j);
			}
		}
		return result.build();
	}

	@Override @Nullable public UltimatoeState play(String selector) {
		if (selector.length() != 2) return null;
		final int majorIndex = selector.charAt(0) - '0';
		final int minorIndex = selector.charAt(1) - '0';
		return play(majorIndex, minorIndex);
	}

	@Override public UltimatoeState play(Random random) {
		if (Integer.bitCount(possibilities) == 1) {
			final int i = Integer.numberOfTrailingZeros(possibilities);
			return play(i, random);
		}
		int countdown = childrenCount();
		for (int i=0; i<9; ++i) {
			if (!isPlayable(i)) continue;
			countdown -= Integer.bitCount(boards[i].possibilities());
			if (countdown<=0) return play(i, random);
		}
		throw new RuntimeException("impossible");
	}

	private UltimatoeState play(int i, Random random) {
		while (true) {
			final int j = random.nextInt(9);
			final UltimatoeState result = play(i, j);
			if (result!=null) return result;
		}
	}

	private int childrenCount() {
		if (Integer.bitCount(possibilities) == 1) {
			final int i = Integer.numberOfTrailingZeros(possibilities);
			return Integer.bitCount(boards[i].possibilities());
		}
		if (isFinished()) return 0;
		int result = 0;
		for (int i=0; i<9; ++i) {
			if (!isPlayable(i)) continue;
			result += Integer.bitCount(boards[i].possibilities());
		}
		return result;
	}

	@Nullable UltimatoeState play(int majorIndex, int minorIndex) {
		if (!isPlayable(majorIndex)) return null;
		if (minorIndex >= 9) return null;
		final UltimatoeBoard oldBoard = boards[majorIndex];
		final UltimatoeBoard newBoard = oldBoard.play(minorIndex, playerOnTurn());
		if (newBoard==null) return null;
		final UltimatoeBoard[] newBoards = boards.clone();
		newBoards[majorIndex] = newBoard;

		final UltimatoePlayer newWinner = oldBoard.winner() == newBoard.winner() ? UltimatoePlayer.NONE : computeWinner(newBoards);
		final int newMovesBitmask = computeMovesBitmask(minorIndex, newBoards, newWinner);
		return new UltimatoeState(turn+1, newMovesBitmask, newWinner, newBoards);
	}

	private static int computeMovesBitmask(int lastMinorIndex, UltimatoeBoard[] boards, UltimatoePlayer winner) {
		if (!winner.isNone()) return 0;
		if (!boards[lastMinorIndex].isFinished()) return 1 << lastMinorIndex;
		int result = 0;
		for (int i=0; i<9; ++i) {
			if (!boards[i].isFinished()) result |= 1 << i;
		}
		return result;
	}

	private static final UltimatoePlayer computeWinner(UltimatoeBoard[] boards) {
		for (final int[] winningSet : UltimatoeBoard.WINNING_SETS) {
			final UltimatoePlayer player = boards[winningSet[0]].winner();
			if (player.isNone()) continue;
			if (player != boards[winningSet[1]].winner()) continue;
			if (player != boards[winningSet[2]].winner()) continue;
			return player;
		}
		return UltimatoePlayer.NONE;
	}

	boolean isFinished() {
		return possibilities == 0;
	}

	boolean isPlayable(int majorIndex) {
		return ((possibilities>>majorIndex) & 1) != 0;
	}

	UltimatoePlayer playerOnTurn() {
		return (turn&1) == 0 ? UltimatoePlayer.X : UltimatoePlayer.O;
	}

	static final UltimatoeState EMPTY_STATE = new UltimatoeState(0, (1<<9) - 1, UltimatoePlayer.NONE, new UltimatoeBoard[] {
		UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
		UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
		UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
	});

	private final int turn;
	private final int possibilities;
	@Getter @NonNull private final UltimatoePlayer winner;
	private final UltimatoeBoard[] boards;
}
