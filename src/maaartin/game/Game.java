package maaartin.game;

import java.util.Random;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

/** An immutable representation of a game (state). */
@Immutable public interface Game<G extends Game<G>> {
	/**
	 * Return the list of all players, including dummies (like e.g., a player representing "no winner").
	 *
	 * <p>The starting player is the first element, other real players follow, dummies are placed last.
	 */
	ImmutableList<GamePlayer> players();

	/** Return the zero-based turn (i.e., the number of moves already played). */
	int turn();

	/**
	 * Return the player to go, unless the game has already finished.
	 *
	 * <p>In case {@link #isFinished()} returns true, the result is undefined.
	 */
	GamePlayer playerOnTurn();

	/** Return the winner. If there's none, a special dummy gets returned. */
	GamePlayer winner();

	/**
	 * Return the score for the initial player (the bigger the better for them).
	 * The result lies between -1 and +1.
	 *
	 * <p>Note that there's no heuristics involved, the score reflects the rules only.
	 *
	 * <p>If applicable for the given game, the result is<ul>
	 * <li>{@code +1.0}, when the {@link #winner()} is {@code players().get(0)}
	 * <li>{@code -1.0}, when the {@link #winner()} is {@code players().get(1)}
	 * <li>{@code 0.0}, otherwise.</ul>
	 */
	double score();

	/** Return true if the game has been finished. */
	boolean isFinished();

	/** Return a map of all directly reachable states to the corresponding moves. */
	ImmutableBiMap<G, String> children();

	/**
	 * Return a game state resulting from applying a move represented by the argument string.
	 * The set of all possible moves may be obtained via {@code children().values()}.
	 *
	 * @throws IllegalArgumentException if the game has already finished or the move is illegal or malformed.
	 */
	G play(String move);

	/**
	 * Return a game state resulting from applying a randomly chosen move.
	 *
	 * @throws IllegalArgumentException if the game has already finished.
	 */
	G play(Random random);

	/**
	 * Return a unique string representation suitable for automated processing and allowing to recreate the game.
	 *
	 * <p>Using this method rather than {@link toString} allows the latter
	 * to omit some information or add redundant text to get mare human-friendly.
	 */
	String asString();
}
