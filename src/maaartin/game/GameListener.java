package maaartin.game;

public interface GameListener<G extends Game<G>> {
	void setState(G game);
}
