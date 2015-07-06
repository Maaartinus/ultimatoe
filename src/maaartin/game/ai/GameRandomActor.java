package maaartin.game.ai;

import java.util.Random;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableMap;

import maaartin.game.GameAIParameters;

import maaartin.game.GameActor;
import maaartin.game.Game;

@RequiredArgsConstructor public final class GameRandomActor<G extends Game<G>> implements GameActor<G> {
	public GameRandomActor(G initialGame) {
		this(initialGame, new GameAIParameters());
	}

	@Override @Nullable public String selectMove(G game) {
		final ImmutableMap<G, String> children = game.children();
		if (children.isEmpty()) return null;
		return children.get(game.play(random));
	}

	@Getter private final G initialGame;
	@Getter private final GameAIParameters parameters;
	private final Random random = new Random();
}
