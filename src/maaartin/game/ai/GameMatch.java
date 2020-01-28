package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import maaartin.game.Game;
import maaartin.game.GameActor;

public abstract class GameMatch<G extends Game<G>> implements Runnable {
	private static final class ActorStats {
		void record(double score) {
			++nGames;
			sum += score;
			if (score >= +1) {
				++nWins;
			} else if (score <= -1){
				++nLosses;
			} else {
				++nDraws;
			}
		}

		@SuppressWarnings("boxing") @Override public String toString() {
			final double percentage = 100.0 * sum / nGames;
			return String.format("Score: %+.1f%% (games: %s, w: %d, l: %d, d: %d)",
					percentage, nGames, nWins, nLosses, nDraws);
		}

		private int nGames;
		private int nWins;
		private int nLosses;
		private int nDraws;
		private double sum;
	}

	protected GameMatch(G initialGame, List<GameActor> actors) {
		this(initialGame, actors, null);
	}

	protected GameMatch(G initialGame, List<GameActor> actors, @Nullable Boolean alwaysStart) {
		this.initialGame = checkNotNull(initialGame);
		this.actors = ImmutableList.copyOf(checkNotNull(actors));
		checkArgument(actors.size() >= 2);
		this.alwaysStart = alwaysStart;
		for (final GameActor a : actors) stats.put(a, new ActorStats());
	}

	@Override public final void run() {
		while (true) {
			for (final GameActor a0 : actors) {
				if (alwaysStart!=null && alwaysStart.booleanValue() && a0 != actors.get(0)) continue;
				for (final GameActor a1 : actors) {
					if (alwaysStart!=null && !alwaysStart.booleanValue() && a1 == actors.get(0)) continue;
					if (a0==a1) continue;
					final double score = matchOnce(a0, a1);
					stats.get(a0).record(+score);
					stats.get(a1).record(-score);
				}
			}
			System.out.println(stats.get(actors.get(0)));
		}
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

	private final G initialGame;
	private final ImmutableList<GameActor> actors;
	@Nullable private final Boolean alwaysStart;
	private final Map<GameActor, ActorStats> stats = Maps.newHashMap();
}
