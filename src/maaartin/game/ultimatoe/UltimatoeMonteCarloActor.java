package maaartin.game.ultimatoe;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import javax.annotation.Nullable;

import lombok.Getter;

import com.google.common.collect.ImmutableList;

import maaartin.game.IGameActor;

public final class UltimatoeMonteCarloActor implements IGameActor<UltimatoeState> {
	private final class Evaluator implements Comparator<Evaluator> {
		Evaluator(UltimatoeState state) {
			this.state = state;
			isMinimizing = state.playerOnTurn() == UltimatoePlayer.O;
		}

		@Override public int compare(Evaluator a, Evaluator b) {
			return Double.compare(a.score(), b.score()) * (isMinimizing ? -1 : +1);
		}

		Evaluator evaluate() {
			if (evaluators==null) initEvaluators();
			final int length = evaluators.length;
			final int reps = 100/length;

			for (int n=1; n<=2*length; ++n) {
				for (int i=0; i<length/n; ++i) {
					for (int j=0; j<reps; ++j) evaluators[i].evalOnce();
				}
				Arrays.sort(evaluators, this);
			}

			final Evaluator bestEvaluator = evaluators[getBestIndex()];
			bestChild = bestEvaluator.state;
			scoreSum = bestEvaluator.scoreSum;
			scoreCount = bestEvaluator.scoreCount;
			return this;
		}

		private void evalOnce() {
			scoreSum += nextScore();
			++scoreCount;
		}

		private double nextScore() {
			switch (nextWinner()) {
				case NONE: return 0;
				case O: return -1;
				case X: return +1;
			}
			throw new RuntimeException("impossible");
		}

		private UltimatoePlayer nextWinner() {
			for (UltimatoeState state=this.state; ; state=state.play(random)) {
				if (state.isFinished()) return state.winner();
			}
		}

		private void initEvaluators() {
			final ImmutableList<UltimatoeState> children = state.children().keySet().asList();
			evaluators = new Evaluator[children.size()];
			for (int i=0; i<evaluators.length; ++i) evaluators[i] = new Evaluator(children.get(i));
		}

		private int getBestIndex() {
			int result = 0;
			for (int i=0; i<evaluators.length; ++i) {
				if ((evaluators[i].score() > evaluators[result].score()) ^ isMinimizing) result = i;
			}
			return result;
		}

		private double score() {
			return scoreSum / scoreCount;
		}

		private final UltimatoeState state;
		private final boolean isMinimizing;

		private Evaluator[] evaluators;

		@Getter private UltimatoeState bestChild;
		private double scoreSum;
		private int scoreCount;
	}

	@Override @Nullable public String selectMove(UltimatoeState state) {
		final ImmutableList<UltimatoeState> children = state.children().keySet().asList();
		if (children.isEmpty()) return null;
		return state.children().get(new Evaluator(state).evaluate().bestChild());
	}

	private final Random random = new Random();
}
