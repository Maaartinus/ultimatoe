package maaartin.game.ultimatoe;

import maaartin.game.IGameActor;

public class UltimatoeMatch {
	public static void main(String[] args) {
		System.out.println("Running Monte Carlo against Random (endless loop).");
		while (true) new UltimatoeMatch().go();
	}

	@SuppressWarnings("boxing") private void go() {
		final IGameActor<UltimatoeState> monteCarloActor = new UltimatoeMonteCarloActor();
		final IGameActor<UltimatoeState> randomActor = new UltimatoeRandomActor();
		int score = 0;
		for (int i=0; i<NUM_GAMES; ++i) score += matchTwice(monteCarloActor, randomActor);
		System.out.format("Score: %s (range: %s to %s)\n", score, -2*NUM_GAMES, +2*NUM_GAMES);
	}

	private int matchTwice(IGameActor<UltimatoeState> actor0, IGameActor<UltimatoeState> actor1) {
		return matchOnce(actor0, actor1) - matchOnce(actor1, actor0);
	}

	private int matchOnce(IGameActor<UltimatoeState> actorO, IGameActor<UltimatoeState> actorX) {
		UltimatoeState state = UltimatoeState.EMPTY_STATE;
		while (true) {
			if (state.isFinished()) break;
			final IGameActor<UltimatoeState> actor = state.playerOnTurn() == UltimatoePlayer.O ? actorO : actorX;
			state = state.play(actor.selectMove(state));
		}
		switch (state.winner()) {
			case NONE: return 0;
			case O: return +1;
			case X: return -1;
		}
		throw new RuntimeException("impossible");
	}

	private static final int NUM_GAMES = 50;
}
