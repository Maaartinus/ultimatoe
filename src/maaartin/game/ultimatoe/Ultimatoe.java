package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Random;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import maaartin.game.GamePlayer;

import maaartin.game.Game;

@RequiredArgsConstructor(access=AccessLevel.PRIVATE) @EqualsAndHashCode
public final class Ultimatoe implements Game<Ultimatoe> {
	static final class ToStringHelper {
		ToStringHelper(Ultimatoe game) {
			final char[][] result = new char[11][11];
			for (int i=0; i<11; ++i) Arrays.fill(result[i], UltimatoeUtils.BORDER);

			for (int y1=0; y1<3; ++y1) {
				for (int x1=0; x1<3; ++x1) {
					final int majorIndex = x1 + 3*y1;
					final UltimatoeBoard board = game.boards[majorIndex];
					for (int y0=0; y0<3; ++y0) {
						for (int x0=0; x0<3; ++x0) {
							final UltimatoePlayer player = board.get(x0 + 3*y0);
							final char c = computeChar(game, majorIndex, player);
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

		private char computeChar(Ultimatoe game, int majorIndex, UltimatoePlayer player) {
			if (!player.isNone()) return player.toChar();
			return game.isPlayable(majorIndex) ? UltimatoeUtils.PLAYABLE : UltimatoeUtils.NON_PLAYABLE;
		}

		private final String toString;
	}

	@Override public String toString() {
		return new ToStringHelper(this).toString;
	}

	@Override public ImmutableList<GamePlayer<Ultimatoe>> players() {
		return UltimatoeUtils.PLAYERS;
	}

	@Override public double score() {
		switch (winner) {
			case NONE: return 0;
			case O: return +1;
			case X: return -1;
		}
		throw new RuntimeException("impossible");
	}

	@Override public ImmutableBiMap<Ultimatoe, String> children() {
		final ImmutableBiMap.Builder<Ultimatoe, String> result = ImmutableBiMap.builder();
		for (int i=0; i<9; ++i) {
			if (!isPlayable(i)) continue;
			final UltimatoeBoard b = boards[i];
			for (int j=0; j<9; ++j) {
				if (!b.isPlayable(j)) continue;
				result.put(play(i, j), UltimatoeUtils.indexesToMoveString(i, j));
			}
		}
		return result.build();
	}

	@Override public Ultimatoe play(String move) {
		checkNotNull(move);
		final int majorIndex = UltimatoeUtils.stringToMajorIndex(move);
		final int minorIndex = UltimatoeUtils.stringToMinorIndex(move);
		return play(majorIndex, minorIndex);
	}

	@Override public Ultimatoe play(Random random) {
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

	private Ultimatoe play(int i, Random random) {
		final int possibilities = boards[i].possibilities();
		while (true) {
			final int j = random.nextInt(9);
			if ((possibilities & (1<<j)) == 0) continue;
			return checkNotNull(play(i, j));
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

	private Ultimatoe play(int majorIndex, int minorIndex) {
		checkArgument(isPlayable(majorIndex));
		final UltimatoeBoard oldBoard = boards[majorIndex];
		final UltimatoeBoard newBoard = oldBoard.play(minorIndex, playerOnTurn());
		checkNotNull(newBoard);
		final UltimatoeBoard[] newBoards = boards.clone();
		newBoards[majorIndex] = newBoard;

		final boolean sameWinner = oldBoard.winner() == newBoard.winner();
		final UltimatoePlayer newWinner = sameWinner ? UltimatoePlayer.NONE : computeWinner(newBoards);
		final int newMovesBitmask = computeMovesBitmask(minorIndex, newBoards, newWinner);
		return new Ultimatoe(turn+1, newMovesBitmask, newWinner, newBoards);
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

	@Override public boolean isFinished() {
		return possibilities == 0;
	}

	@Override public GamePlayer<Ultimatoe> playerOnTurn() {
		return UltimatoeUtils.PLAYERS.get(turn & 1);
	}

	boolean isPlayable(int majorIndex) {
		return ((possibilities>>majorIndex) & 1) != 0;
	}

	public static final Ultimatoe INITIAL_GAME = new Ultimatoe(
			0, (1<<9) - 1, UltimatoePlayer.NONE, new UltimatoeBoard[] {
				UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
				UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
				UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD, UltimatoeBoard.EMPTY_BOARD,
			});

	@Getter private final int turn;
	private final int possibilities;
	@NonNull private final UltimatoePlayer winner; //TODO
	private final UltimatoeBoard[] boards;
}