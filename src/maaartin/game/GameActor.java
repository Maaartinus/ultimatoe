package maaartin.game;


public interface GameActor<G extends Game<G>> {
	G initialGame();
	String selectMove(G game);
	GameAIParameters parameters();
}
