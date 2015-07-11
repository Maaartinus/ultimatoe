package maaartin.game;

import com.google.common.collect.ImmutableList;


public abstract class StandardGame<G extends Game<G>> implements Game<G> {
	@Override public final ImmutableList<GamePlayer> players() {
		return StandardPlayer.PLAYERS;
	}

	@Override public final GamePlayer playerOnTurn() {
		return StandardPlayer.PLAYERS.get(turn() & 1);
	}

	@Override public final double score() {
		switch (winner().ordinal()) {
			case 0: return +1;
			case 1: return -1;
		}
		return 0;
	}

	@Override public String toString() {
		return asString();
	}
}
