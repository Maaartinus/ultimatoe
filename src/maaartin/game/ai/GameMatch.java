package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.RequiredArgsConstructor;

import maaartin.game.Game;
import maaartin.game.GameActor;

@RequiredArgsConstructor public class GameMatch<G extends Game<G>> implements Runnable {
	@Override @SuppressWarnings("boxing") public void run() {
		final GameActor experimentalActor = new GameMonteCarloActor();
		final GameActor normalActor = new GameMonteCarloActor();
		//		final GameActor normalActor = new GameRandomActor();
		normalActor.parameters().budget(1000);
		experimentalActor.parameters().budget(1000);
		experimentalActor.parameters().isExperimental(true);
		int score = 0;
		int games = 0;
		while (true) {
			games += 2 * NUM_GAMES;
			for (int i=0; i<NUM_GAMES; ++i) score += matchTwice(experimentalActor, normalActor);
			System.out.format("Score: %+.1f%% (games: %s)\n", 100.0 * score / games, games);
		}
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

	private static final int NUM_GAMES = 5;
	private final G initialGame;
}
