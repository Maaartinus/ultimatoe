package maaartin.game.ai;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableMap;

import maaartin.game.Game;
import maaartin.game.GameAIParameters;
import maaartin.game.GameActor;

@RequiredArgsConstructor public final class GameRandomActor<G extends Game<G>> implements GameActor<G> {
	public GameRandomActor(G initialGame) {
		this(initialGame, new GameAIParameters());
	}

	@Override @Nullable public String selectMove(G game) {
		final ImmutableMap<G, String> children = game.children();
		checkArgument(!children.isEmpty());
		final G child = game.play(random);
		final String result = game.children().get(child);
		checkNotNull(result);
		return result;
	}

	@Getter private final G initialGame;
	@Getter private final GameAIParameters parameters;
	private final Random random = new Random();
}
