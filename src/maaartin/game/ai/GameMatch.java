package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.RequiredArgsConstructor;

import maaartin.game.Game;
import maaartin.game.GameActor;

@RequiredArgsConstructor public class GameMatch<G extends Game<G>> implements Runnable {
	@Override @SuppressWarnings("boxing") public void run() {
		final GameActor advancedActor = new GameMonteCarloActor();
		//		final GameActor<G> normalActor = new GameMonteCarloActor<>(initialGame);
		final GameActor normalActor = new GameRandomActor();
		advancedActor.parameters().isExperimental(true);
		int score = 0;
		for (int i=0; i<NUM_GAMES; ++i) score += matchTwice(advancedActor, normalActor);
		System.out.format("Score: %s (range: %s to %s)\n", score, -2*NUM_GAMES, +2*NUM_GAMES);
	}

	private double matchTwice(GameActor actor0, GameActor actor1) {
		return matchOnce(actor0, actor1) - matchOnce(actor1, actor0);
	}

	private double matchOnce(GameActor firstActor, GameActor secondActor) {
		G game = initialGame;
		while (true) {
			if (game.isFinished()) break;
			final GameActor actor = game.playerOnTurn().ordinal() == 0 ? firstActor : secondActor;
			final String move = actor.selectMove(game);
			checkNotNull(move);
			game = game.play(move);
		}
		return game.score();
	}

	private static final int NUM_GAMES = 50;
	private final G initialGame;
}
