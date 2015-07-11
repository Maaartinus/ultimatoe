package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;

@RequiredArgsConstructor public final class GameMonteCarloActor<G extends Game<G>> implements GameActor<G> {
	public GameMonteCarloActor(G initialGame) {
		this(initialGame, new GameAIParameters());
	}

	private static final class Evaluator<G extends Game<G>> {
		private static final class ScoreFunction<G extends Game<G>> implements Function<Evaluator<G>, Double> {
			ScoreFunction(boolean isMinimizing, double uncertaintyWeight) {
				this.uncertaintyWeight = uncertaintyWeight;
				this.factor = isMinimizing ? -1 : +1;
			}

			@Override public Double apply(Evaluator<G> input) {
				return Double.valueOf(factor * input.score(uncertaintyWeight));
			}

			private final double factor;
			private final double uncertaintyWeight;
		}

		Evaluator(G game, GameAIParameters parameters, long seed) {
			this.game = game;
			this.parameters = parameters;
			random = new Random(seed ^ (seed>>32));
			ownSum = game.score();
			ownCount = 1;
		}

		Evaluator<G> spend(int budget) {
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
			final int length = evaluators.size();
			while (true) {
				for (int i=0; i<length; ++i, --budget) {
					if (budget<=0) return;
					evaluators.get(i).spend(1);
				}
			}
		}

		private void spendInternalExperimental(int budget) {
			spendInternalExperimental2(budget);
		}


		private void spendInternalExperimental1(int budget) {
			final int length = evaluators.size();
			final ScoreFunction<G> function = new ScoreFunction<G>(isMinimizing(), 1);
			final Ordering<Evaluator<G>> ordering = Ordering.natural().onResultOf(function).reverse();
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
			final ScoreFunction<G> function = new ScoreFunction<G>(isMinimizing(), 2);
			final Ordering<Evaluator<G>> ordering = Ordering.natural().onResultOf(function).reverse();
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
		private void propagate() {
			if (evaluators==null) return;
			if (evaluators.isEmpty()) return;
			final Evaluator<G> bestEvaluator = evaluators.get(getBestIndex());
			bestChild = bestEvaluator.game;
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
			for (G game=this.game; ; game=game.play(random)) {
				if (game.isFinished()) return game.score();
			}
		}

		private void initEvaluators() {
			final ImmutableList<G> children = game.children().keySet().asList();
			evaluators = Lists.newArrayList();
			for (final G game : children) evaluators.add(new Evaluator<>(game, parameters, random.nextLong()));
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

		private final G game;
		private final GameAIParameters parameters;
		private final Random random;

		private List<Evaluator<G>> evaluators;

		@Getter private G bestChild;
		private double ownSum;
		private double ownCount;
		private double propagatedSum;
		private double propagatedCount;
	}

	@Override public String selectMove(G game) {
		final ImmutableList<G> children = game.children().keySet().asList();
		checkArgument(!children.isEmpty());
		final Evaluator<G> evaluator = new Evaluator<>(game, parameters, random.nextLong());
		G bestChild = evaluator.spend(parameters.budget()).bestChild();
		if (bestChild==null) bestChild = game.play(random);
		checkNotNull(bestChild);
		final String result = game.children().get(bestChild);
		checkNotNull(result);
		return result;
	}

	@Getter private final G initialGame;
	@Getter private final GameAIParameters parameters;
	private final Random random = new Random();
}
