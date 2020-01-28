package maaartin.game;

/** Represents a real player (unlike {@link GamePlayer}) of game {@code G}, e.g., an AI player. */
public interface GameActor {
	/** Return the "best" move for the given game state. */
	String selectMove(Game<?> game);

	GameAIParameters parameters();
}
