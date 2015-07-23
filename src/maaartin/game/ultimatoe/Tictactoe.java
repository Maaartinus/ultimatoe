package maaartin.game.ultimatoe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import maaartin.game.GamePlayer;
import maaartin.game.StandardGame;
import maaartin.game.StandardPlayer;

/**
 * Representation of a 3x3 board of {@link Ultimatoe}.
 *
 * <p>As there are just a few thousands reachable states, they all get pre-created with all necessary data
 * in order to make moves as fast as possible.
 */
@Immutable public final class Tictactoe extends StandardGame<Tictactoe> {
	@Override public Tictactoe play(String move) {
		return checkNotNull(children.inverse().get(move));
	}

	@Override public Tictactoe play(Random random) {
		checkNotNull(random);
		final int size = children.size();
		checkState(size>0);
		return children.keySet().asList().get(random.nextInt(size));
	}

	/**
	 * Create a new board described by the given fields and also recursively create
	 * all its ancestors unless found in the repository.
	 *
	 * @param fields the effectivelly immutable list containing state of the nine fields
	 * @param repository a map filled with already created boards
	 */
	private Tictactoe(int turn, List<StandardPlayer> fields, Map<List<StandardPlayer>, Tictactoe> repository) {
		this.turn = turn;
		checkArgument(fields.size() == AREA);
		this.fields = fields;
		winner = computeWinner();
		int possibilities = 0;
		final ImmutableBiMap.Builder<Tictactoe, String> children = ImmutableBiMap.builder();
		if (winner.isDummy()) {
			for (final GamePlayer player : StandardPlayer.PLAYERS) {
				if (player.isDummy()) continue;
				for (int i=0; i<AREA; ++i) {
					if (!getPlayerOnField(i).isDummy()) continue;
					final Tictactoe child = getOrCreateChild(i, (StandardPlayer) player, repository);
					childArray[childIndex(i, player)] = child;
					if (player == playerOnTurn()) children.put(child, String.valueOf(i));
					possibilities |= 1 << i;
				}
			}
		}
		this.possibilities = possibilities;
		this.children = children.build();
	}

	/** Get or create a child by executing a move of the given player on a field given by the index. */
	private Tictactoe getOrCreateChild(
			int index, StandardPlayer player, Map<List<StandardPlayer>, Tictactoe> repository) {
		final List<StandardPlayer> childFields = Lists.newArrayList(this.fields);
		childFields.set(index, player);
		Tictactoe result = repository.get(childFields);
		if (result!=null) return result;
		result = new Tictactoe(turn+1, childFields, repository);
		repository.put(childFields, result);
		return result;
	}

	/** Create the empty board. Must be called just once. */
	private static Tictactoe createEmptyBoard() {
		final List<StandardPlayer> emptyFields = Lists.newArrayList();
		for (int i=0; i<AREA; ++i) emptyFields.add(StandardPlayer.NOBODY);
		final Map<List<StandardPlayer>, Tictactoe> repository = Maps.newHashMap();
		return new Tictactoe(0, emptyFields, repository);
	}

	private StandardPlayer computeWinner() {
		for (final int[] winningSet : UltimatoeUtils.WINNING_SETS) {
			final StandardPlayer player = getPlayerOnField(winningSet[0]);
			if (player.isDummy()) continue;
			if (player != getPlayerOnField(winningSet[1])) continue;
			if (player != getPlayerOnField(winningSet[2])) continue;
			return player;
		}
		return StandardPlayer.NOBODY;
	}

	@Override public String asString() {
		final StringBuilder result = new StringBuilder();
		for (int i=0; i<AREA; ++i) {
			if (i%WIDTH == 0) result.append("\n");
			result.append(getPlayerOnField(i).toChar());
		}
		return result.substring(1);
	}

	/** Return a child corresponding to executing a move of the given player on a field given by the index. */
	@Nullable Tictactoe play(int index, GamePlayer player) {
		return childArray[childIndex(index, player)];
	}

	StandardPlayer getPlayerOnField(int index) {
		return fields.get(index);
	}

	/**
	 * Return true if it's possible to play at field with the given index,
	 * i.e., if the field is empty and the board hasn't been decided yet.
	 */
	boolean isPlayable(int index) {
		return (possibilities & (1<<index)) != 0;
	}

	/** Return true if this board has been finished. */
	@Override public boolean isFinished() {
		return possibilities == 0;
	}

	private int childIndex(int index, GamePlayer player) {
		return 2*index + player.ordinal();
	}

	private static final int WIDTH = 3;
	private static final int HEIGHT = 3;
	private static final int AREA = WIDTH * HEIGHT;

	static final Tictactoe INITIAL_GAME = createEmptyBoard();

	/** The number of turns made on this board. */
	@Getter private final int turn;

	/** The 9 fields of the board, from left to right, then top to bottom. Effectively immutable. */
	private final List<StandardPlayer> fields;

	/** The player who has won, otherwise {@link StandardPlayer#NOBODY}. */
	@Getter @NonNull private final StandardPlayer winner;

	/**
	 * A bitmask determining which moves are allowed, i.e.,<ul>
	 * <li>which fields are empty, if the board hasn't been decided yet.
	 * <li>zero, otherwise.
	 *
	 * <p>For details, see the code of {@link Tictactoe#isPlayable(int)}.
	 */
	@Getter(AccessLevel.PROTECTED) private final int possibilities;

	/**
	 * An array of all child boards, i.e., boards resulting from one player placing one piece on an empty field.
	 *
	 * <p>For details, see the code of {@link Tictactoe#childIndex(int, GamePlayer)}.
	 * A 1D arrays gets used for maximum speed (better memory locality).
	 * This may or may not be a premature optimization.
	 */
	private final Tictactoe[] childArray = new Tictactoe[2*AREA];

	@Getter private final ImmutableBiMap<Tictactoe, String> children;
}
