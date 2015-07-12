package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableMap;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;

@RequiredArgsConstructor public final class GameRandomActor implements GameActor {
	public GameRandomActor() {
		this(new GameAIParameters());
	}

	@Override public String selectMove(Game<?> game) {
		@SuppressWarnings("unchecked")
		final ImmutableMap<Game<?>, String> children = (ImmutableMap<Game<?>, String>) game.children();
		checkArgument(!children.isEmpty());
		final Game<?> child = game.play(random);
		final String result = game.children().get(child);
		checkNotNull(result);
		return result;
	}

	@Getter private final GameAIParameters parameters;
	private final Random random = new Random();
}
