package maaartin.game;

/** Represents a real player (unlike {@link GamePlayer}) of game {@code G}, e.g., an AI player. */
public interface GameActor<G extends Game<G>> {
	/** Return the initial state of {@code G}, like e.g. an empty tic-tac-toe board. */
	G initialGame();

	/** Return the "best" move for the given game state. */
	String selectMove(G game);

	GameAIParameters parameters();
}
