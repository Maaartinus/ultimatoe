package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import lombok.Getter;
import lombok.NonNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import maaartin.game.GamePlayer;

/**
 * Representation of a 3x3 board of {@link Ultimatoe}.
 *
 * <p>As there are just a few thousands reachable states, they all get pre-created with all necessary data
 * in order to make moves as fast as possible.
 */
@Immutable final class UltimatoeBoard {
	/**
	 * Create a new board described by the given fields and also recursively create
	 * all its ancestors unless found in the repository.
	 *
	 * @param fields the effectivelly immutable list containing state of the nine fields
	 * @param repository a map filled with already created boards
	 */
	private UltimatoeBoard(List<UltimatoePlayer> fields, Map<List<UltimatoePlayer>, UltimatoeBoard> repository) {
		checkArgument(fields.size() == AREA);
		this.fields = fields;
		winner = computeWinner();
		int possibilities = 0;
		if (winner.isDummy()) {
			for (final GamePlayer<Ultimatoe> player : UltimatoeUtils.PLAYERS) {
				if (player.isDummy()) continue;
				for (int i=0; i<AREA; ++i) {
					if (!getPlayerOnField(i).isDummy()) continue;
					children[childIndex(i, player)] = getOrCreateChild(i, (UltimatoePlayer) player, repository);
					possibilities |= 1 << i;
				}
			}
		}
		this.possibilities = possibilities;
	}

	/** Get or create a child by executing a move of the given player on a field given by the index. */
	private UltimatoeBoard getOrCreateChild(
			int index, UltimatoePlayer player, Map<List<UltimatoePlayer>, UltimatoeBoard> repository) {
		final List<UltimatoePlayer> childFields = Lists.newArrayList(this.fields);
		childFields.set(index, player);
		UltimatoeBoard result = repository.get(childFields);
		if (result!=null) return result;
		result = new UltimatoeBoard(childFields, repository);
		repository.put(childFields, result);
		return result;
	}

	/** Create the empty board. Must be called just once. */
	private static UltimatoeBoard createEmptyBoard() {
		final List<UltimatoePlayer> emptyFields = Lists.newArrayList();
		for (int i=0; i<AREA; ++i) emptyFields.add(UltimatoePlayer.NOBODY);
		final Map<List<UltimatoePlayer>, UltimatoeBoard> repository = Maps.newHashMap();
		return new UltimatoeBoard(emptyFields, repository);
	}

	private UltimatoePlayer computeWinner() {
		for (final int[] winningSet : UltimatoeUtils.WINNING_SETS) {
			final UltimatoePlayer player = getPlayerOnField(winningSet[0]);
			if (player.isDummy()) continue;
			if (player != getPlayerOnField(winningSet[1])) continue;
			if (player != getPlayerOnField(winningSet[2])) continue;
			return player;
		}
		return UltimatoePlayer.NOBODY;
	}

	@Override public String toString() {
		final StringBuilder result = new StringBuilder();
		for (int i=0; i<AREA; ++i) {
			if (i%WIDTH == 0) result.append("\n");
			result.append(getPlayerOnField(i).toChar());
		}
		return result.substring(1);
	}

	/** Return a child corresponding to executing a move of the given player on a field given by the index. */
	@Nullable UltimatoeBoard play(int index, GamePlayer<Ultimatoe> player) {
		return children[childIndex(index, player)];
	}

	UltimatoePlayer getPlayerOnField(int index) {
		return fields.get(index);
	}

	/**
	 * Return true if iy's possible to play at field with the given index,
	 * i.e., if the field is empty and the board hasn't been decided yet.
	 */
	boolean isPlayable(int index) {
		return (possibilities & (1<<index)) != 0;
	}

	/** Return true if this board has been finished. */
	boolean isFinished() {
		return possibilities == 0;
	}

	private int childIndex(int index, GamePlayer<Ultimatoe> player) {
		return 2*index + player.ordinal();
	}

	private static final int WIDTH = 3;
	private static final int HEIGHT = 3;
	private static final int AREA = WIDTH * HEIGHT;

	static final UltimatoeBoard EMPTY_BOARD = createEmptyBoard();

	/** The 9 fields of the board, from left to right, then top to bottom. Effectively immutable. */
	private final List<UltimatoePlayer> fields;

	/** The player who has won, otherwise {@link UltimatoePlayer#NOBODY}. */
	@Getter @NonNull private final UltimatoePlayer winner;

	/**
	 * A bitmask determining which moves are allowed, i.e.,<ul>
	 * <li>which fields are empty, if the board hasn't been decided yet.
	 * <li>zero, otherwise.
	 *
	 * <p>For details, see the code of {@link UltimatoeBoard#isPlayable(int)}.
	 */
	@Getter private final int possibilities;

	/**
	 * An array of all child boards, i.e., boards resulting from one player placing one piece on an empty field.
	 *
	 * <p>For details, see the code of {@link UltimatoeBoard#childIndex(int, GamePlayer)}.
	 * A 1D arrays gets used for maximum speed (better memory locality).
	 * This may or mey not be a premature optimization.
	 */
	private final UltimatoeBoard[] children = new UltimatoeBoard[2*AREA];
}
