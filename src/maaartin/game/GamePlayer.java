package maaartin.game;

import javax.annotation.concurrent.Immutable;

/**
 * A representation of an abstract player (like "black" or "white" in chess).
 *
 * <p>Not to be confused with {@link GameActor}}.
 */
@Immutable public interface GamePlayer {
	/** Return an int uniquely identifiying {@code this}. */
	int ordinal();

	/**
	 * Return true if {@code this} represents a dummy (e.g., no winner) rather than
	 * a real player (e.g., "X" or "O" in tic-tac-toe).
	 */
	boolean isDummy();
}
