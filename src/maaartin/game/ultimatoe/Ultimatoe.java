package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Random;

import javax.annotation.concurrent.Immutable;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import maaartin.game.Game;
import maaartin.game.GamePlayer;
import maaartin.game.StandardPlayer;

/**
 * An immutable representation of (the state of)
 * <a href="http://mathwithbaddrawings.com/2013/06/16/ultimate-tic-tac-toe">Ultimate Tic-Tac-Toe</a>.
 */
@RequiredArgsConstructor(access=AccessLevel.PRIVATE) @EqualsAndHashCode @Immutable
public final class Ultimatoe implements Game<Ultimatoe> {
	private static final class ToStringHelper {
		ToStringHelper(Ultimatoe game) {
			final char[][] result = new char[11][11];
			for (int i=0; i<11; ++i) Arrays.fill(result[i], UltimatoeUtils.BORDER);

			for (int y1=0; y1<3; ++y1) {
				for (int x1=0; x1<3; ++x1) {
					final int majorIndex = x1 + 3*y1;
					final Tictactoe board = game.boards[majorIndex];
					for (int y0=0; y0<3; ++y0) {
						for (int x0=0; x0<3; ++x0) {
							final StandardPlayer player = board.getPlayerOnField(x0 + 3*y0);
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

		private char computeChar(Ultimatoe game, int majorIndex, StandardPlayer player) {
			if (!player.isDummy()) return player.toChar();
			return game.isPlayable(majorIndex) ? UltimatoeUtils.PLAYABLE : UltimatoeUtils.NON_PLAYABLE;
		}

		private final String toString;
	}

	@Override public String toString() {
		return asString();
	}

	@Override public String asString() {
		return new ToStringHelper(this).toString;
	}

	@Override public ImmutableList<GamePlayer> players() {
		return StandardPlayer.PLAYERS;
	}

	@Override public double score() {
		switch (winner) {
			case NOBODY: return 0;
			case PLAYER_X: return +1;
			case PLAYER_O: return -1;
		}
		throw new RuntimeException("impossible");
	}

	@Override public ImmutableBiMap<Ultimatoe, String> children() {
		final ImmutableBiMap.Builder<Ultimatoe, String> result = ImmutableBiMap.builder();
		for (int i=0; i<N_BOARDS; ++i) {
			if (!isPlayable(i)) continue;
			final Tictactoe b = boards[i];
			for (int j=0; j<N_FIELDS_PER_BOARD; ++j) {
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
		checkNotNull(random);
		if (Integer.bitCount(possibilities) == 1) {
			final int i = Integer.numberOfTrailingZeros(possibilities);
			return play(i, random);
		}
		int countdown = childrenCount();
		for (int i=0; i<N_BOARDS; ++i) {
			if (!isPlayable(i)) continue;
			countdown -= Integer.bitCount(boards[i].possibilities());
			if (countdown<=0) return play(i, random);
		}
		throw new RuntimeException("impossible");
	}

	/** Return the game state resulting from applying a random move on the board given by the argument. */
	private Ultimatoe play(int majorIndex, Random random) {
		final int possibilities = boards[majorIndex].possibilities();
		while (true) {
			final int j = random.nextInt(N_FIELDS_PER_BOARD);
			if ((possibilities & (1<<j)) == 0) continue;
			return checkNotNull(play(majorIndex, j));
		}
	}

	private int childrenCount() {
		int result = 0;
		for (final Tictactoe b : boards) result += Integer.bitCount(b.possibilities());
		return result;
	}

	/**
	 * Return the game state resulting from playing on the position given by the arguments.
	 *
	 * @param majorIndex the index of the board, must be between 0 and 8
	 * @param minorIndex the index of field of the board, must be between 0 and 8
	 */
	private Ultimatoe play(int majorIndex, int minorIndex) {
		final Tictactoe oldBoard = boards[majorIndex];
		final Tictactoe newBoard = oldBoard.play(minorIndex, playerOnTurn());
		checkNotNull(newBoard);
		final Tictactoe[] newBoards = boards.clone();
		newBoards[majorIndex] = newBoard;

		final boolean sameWinner = oldBoard.winner() == newBoard.winner();
		final StandardPlayer newWinner = sameWinner ? StandardPlayer.NOBODY : computeWinner(newBoards);
		final int newMovesBitmask = computeMovesBitmask(minorIndex, newBoards, newWinner);
		return new Ultimatoe(turn+1, newMovesBitmask, newWinner, newBoards);
	}

	private static int computeMovesBitmask(int lastMinorIndex, Tictactoe[] boards, StandardPlayer winner) {
		if (!winner.isDummy()) return 0;
		if (!boards[lastMinorIndex].isFinished()) return 1 << lastMinorIndex;
		int result = 0;
		for (int i=0; i<N_BOARDS; ++i) {
			if (!boards[i].isFinished()) result |= 1 << i;
		}
		return result;
	}

	private static final StandardPlayer computeWinner(Tictactoe[] boards) {
		for (final int[] winningSet : UltimatoeUtils.WINNING_SETS) {
			final StandardPlayer player = boards[winningSet[0]].winner();
			if (player.isDummy()) continue;
			if (player != boards[winningSet[1]].winner()) continue;
			if (player != boards[winningSet[2]].winner()) continue;
			return player;
		}
		return StandardPlayer.NOBODY;
	}

	@Override public boolean isFinished() {
		return possibilities == 0;
	}

	@Override public GamePlayer playerOnTurn() {
		return StandardPlayer.PLAYERS.get(turn & 1);
	}

	/** Return true if the player on turn can play on the board given by the argument. */
	private boolean isPlayable(int majorIndex) {
		return ((possibilities>>majorIndex) & 1) != 0;
	}

	private static final int N_BOARDS = 9;
	private static final int N_FIELDS_PER_BOARD = 9;

	public static final Ultimatoe INITIAL_GAME = new Ultimatoe(
			0, (1<<N_BOARDS) - 1, StandardPlayer.NOBODY, new Tictactoe[] {
				Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME,
				Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME,
				Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME, Tictactoe.INITIAL_GAME,
			});

	@Getter private final int turn;

	/** Contains one bit per board. See {@link #isPlayable(int)}*/
	private final int possibilities;

	@Getter @NonNull private final StandardPlayer winner;

	/** The 9 boards of the game, from left to right, then top to bottom. */
	private final Tictactoe[] boards;
}
