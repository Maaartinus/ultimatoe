package maaartin.game;

import java.util.Random;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

@Immutable public interface Game<G extends Game<G>> {
	/** Return the list of all players. */
	ImmutableList<GamePlayer<G>> players();

	/** Return the zero-based turn (i.e., the number of moves already played). */
	int turn();

	/** Return the player to go. */
	GamePlayer<G> playerOnTurn();

	/** Return the score for the initial player (the bigger the better for them). The result lies between -1 and +1. */
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
}
