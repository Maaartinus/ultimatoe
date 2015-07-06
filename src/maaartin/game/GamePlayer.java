package maaartin.game;

public interface GamePlayer<G extends Game<G>> {
	/** Return an int uniquely identifiying {@code this}. */
	int ordinal();

	boolean isNone();
}
