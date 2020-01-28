package maaartin.game.ultimatoe;

import com.google.common.collect.ImmutableList;

import maaartin.game.GameActor;
import maaartin.game.ai.GameMatch;
import maaartin.game.ai.GameMonteCarloActor;

public class UltimatoeMatch extends GameMatch<Ultimatoe> {
	private UltimatoeMatch() {
		super(Ultimatoe.INITIAL_GAME, actors());
	}

	private static ImmutableList<GameActor> actors() {
		final ImmutableList.Builder<GameActor> result = ImmutableList.builder();
		final int budget = 1000;

		final GameActor a0 = new GameMonteCarloActor();
		a0.parameters().budget(budget);
		a0.parameters().isExperimental(true);
		result.add(a0);

		final GameActor a1 = new GameMonteCarloActor();
		a1.parameters().budget(budget);
		result.add(a1);

		return result.build();
	}

	public static void main(String[] args) {
		System.out.println("Running two Monte Carlos against each other (endless loop).");
		new UltimatoeMatch().run();
	}
}
