package maaartin.game.ultimatoe;

import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import maaartin.game.IGameActor;

public final class UltimatoeRandomActor implements IGameActor<UltimatoeState> {
	@Override @Nullable public String selectMove(UltimatoeState state) {
		final ImmutableMap<UltimatoeState, String> children = state.children();
		if (children.isEmpty()) return null;
		return children.get(state.play(random));
	}

	private final Random random = new Random();
}
