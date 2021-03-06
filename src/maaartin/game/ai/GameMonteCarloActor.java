package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import de.grajcar.dout.Dout;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;

@RequiredArgsConstructor public final class GameMonteCarloActor implements GameActor {
	public GameMonteCarloActor() {
		this(new GameAIParameters());
	}

	@RequiredArgsConstructor private static final class EvaluatorStats {
		void add(Game<?> game) {
			//			if (game==root) return;
			if (visited.add(game)) {
				++nUnique;
			} else {
				++nRepeated;
			}
		}

		private final Game<?> root;
		private final Set<Game<?>> visited = Sets.newHashSet();
		@Getter private int nUnique;
		@Getter private int nRepeated;
	}

	private static final class Evaluator {
		private static final class ScoreFunction implements Function<Evaluator, Double> {
			ScoreFunction(boolean isMinimizing, double uncertaintyWeight) {
				this.uncertaintyWeight = uncertaintyWeight;
				this.factor = isMinimizing ? -1 : +1;
			}

			@Override public Double apply(Evaluator input) {
				return Double.valueOf(factor * input.score(uncertaintyWeight));
			}

			private final double factor;
			private final double uncertaintyWeight;
		}

		private Evaluator(Game<?> game, GameAIParameters parameters, long seed, EvaluatorStats stats) {
			this.game = game;
			this.parameters = parameters;
			random = new Random(seed ^ (seed>>32));
			this.stats = stats;
			ownSum = game.score();
			ownCount = 1;
		}

		Evaluator spend(int budget) {
			spendInternal(budget);
			propagate();
			return this;
		}

		private void spendInternal(int budget) {
			if (evaluators==null) {
				if (budget<=1) {
					if (budget>0) evalOnce();
					return;
				}
				initEvaluators();
			}
			if (evaluators.isEmpty()) return;

			if (parameters.isExperimental()) {
				spendInternalExperimental(budget);
			} else {
				spendInternalNormal(budget);
			}
		}

		private void spendInternalNormal(int budget) {
			while (true) {
				for (final Evaluator e : evaluators) {
					if (budget-- <= 0) return;
					e.spend(1);
				}
			}
		}

		private void spendInternalExperimental(int budget) {
			spendInternalExperimental4(budget);
		}

		private void spendInternalExperimental5(int budget) {
			final int length = evaluators.size();
			while (true) {
				for (final Evaluator e : evaluators) {
					if (budget-- <= 0) return;
					e.spend(1);
				}
				if (isMinimizing()) continue;
				double avg = 0;
				for (final Evaluator e : evaluators) avg += e.score(10);
				avg /= length;
				for (final Evaluator e : evaluators) {
					if (e.score(10) < avg) continue;
					if (budget-- <= 0) return;
					e.spend(1);
				}
			}
		}

		private void spendInternalExperimental4(int budget) {
			final int length = evaluators.size();
			while (true) {
				for (int i=0; i<length; ++i, --budget) {
					if (budget<=0) return;
					evaluators.get(i).spend(1);
				}
				final ScoreFunction function = new ScoreFunction(isMinimizing(), 5);
				final Ordering<Evaluator> ordering = Ordering.natural().onResultOf(function);
				Collections.sort(evaluators, ordering);
				for (int i=length/2; i<length; ++i, --budget) {
					if (budget<=0) return;
					evaluators.get(i).spend(1);
				}
			}
		}

		private void spendInternalExperimental1(int budget) {
			final int length = evaluators.size();
			final ScoreFunction function = new ScoreFunction(isMinimizing(), 1);
			final Ordering<Evaluator> ordering = Ordering.natural().onResultOf(function).reverse();
			while (true) {
				for (int n=1; n<=2; ++n) {
					final int limit = length / n;
					final boolean recurse = false && n==1 && budget > 10*limit;
					final int b = recurse ? 10 : 1;
					for (int i=0; i<limit; ++i, budget-=b) {
						if (budget<=0) return;
						evaluators.get(i).spend(b);
					}
					Collections.sort(evaluators, ordering);
					if (length<=6) break;
				}
			}
		}

		private void spendInternalExperimental2(int budget) {
			final int length = evaluators.size();
			final ScoreFunction function = new ScoreFunction(isMinimizing(), 5);
			final Ordering<Evaluator> ordering = Ordering.natural().onResultOf(function).reverse();
			while (true) {
				for (int n=1; n<=2*length; ++n) {
					final int limit = length / n;
					final boolean recurse = n==1 && budget > 10*limit;
					final int b = recurse ? 10 : 1;
					for (int i=0; i<limit; ++i, budget-=b) {
						if (budget<=0) return;
						evaluators.get(i).spend(b);
					}
					Collections.sort(evaluators, ordering);
				}
			}
		}

		private void spendInternalExperimental3(int budget) {
			final int length = evaluators.size();
			while (true) {
				final boolean recurse = budget > 10*length;
				final int b = recurse ? 10 : 1;
				for (int i=0; i<length; ++i, budget-=b) {
					if (budget<=0) return;
					evaluators.get(i).spend(b);
				}
			}
		}

		private void propagate() {
			if (evaluators==null) return;
			if (evaluators.isEmpty()) return;
			final Evaluator bestEvaluator = evaluators.get(getBestIndex());
			bestChild = bestEvaluator.game;
			// TODO This is sort of minimax, which makes little sense here.
			propagatedSum = bestEvaluator.sum();
			propagatedCount = bestEvaluator.count();
		}

		private boolean isMinimizing() {
			return game.playerOnTurn().ordinal() == 1;
		}

		private void evalOnce() {
			ownSum += nextScore();
			++ownCount;
		}

		private double nextScore() {
			for (Game<?> game=this.game; ; game=game.play(random)) {
				stats.add(game);
				if (game.isFinished()) return game.score();
			}
		}

		private void initEvaluators() {
			@SuppressWarnings("unchecked")
			final ImmutableList<Game<?>> children = (ImmutableList<Game<?>>) game.children().keySet().asList();
			evaluators = Lists.newArrayList();
			for (final Game<?> game : children) {
				evaluators.add(new Evaluator(game, parameters, random.nextLong(), stats));
			}
			Collections.shuffle(Arrays.asList(evaluators), random);
		}

		private int getBestIndex() {
			final boolean isMinimizing = isMinimizing();
			int result = 0;
			for (int i=0; i<evaluators.size(); ++i) {
				if ((evaluators.get(i).score(0) > evaluators.get(result).score(0)) ^ isMinimizing) result = i;
			}
			return result;
		}

		private double score(double uncertaintyWeight) {
			return (sum() + uncertaintyWeight) / count();
		}

		private double sum() {
			return ownSum + propagatedSum;
		}

		private double count() {
			return ownCount + propagatedCount;
		}

		private final Game<?> game;
		private final GameAIParameters parameters;
		private final Random random;
		@Getter private final EvaluatorStats stats;

		private List<Evaluator> evaluators;

		@Getter private Game<?> bestChild;
		private double ownSum;
		private double ownCount;
		private double propagatedSum;
		private double propagatedCount;
	}

	@Override public String selectMove(Game<?> game) {
		@SuppressWarnings("unchecked")
		final ImmutableList<Game<?>> children = (ImmutableList<Game<?>>) game.children().keySet().asList();
		checkArgument(!children.isEmpty());
		final EvaluatorStats stats = new EvaluatorStats(game);
		final Evaluator evaluator = new Evaluator(game, parameters, random.nextLong(), stats);
		Game<?> bestChild = evaluator.spend(parameters.budget()).bestChild();
		Dout.a(stats.nUnique(), stats.nRepeated());
		if (bestChild==null) bestChild = game.play(random);
		checkNotNull(bestChild);
		final String result = game.children().get(bestChild);
		checkNotNull(result);
		return result;
	}

	@Getter private final GameAIParameters parameters;
	private final Random random = new Random();
}
