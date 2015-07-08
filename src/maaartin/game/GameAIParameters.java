package maaartin.game;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter public final class GameAIParameters {
	private int budget = 100_000;
	private boolean isExperimental;
}
