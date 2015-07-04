package maaartin.game;

import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableBiMap;

public interface IGameState<S extends IGameState<S>> {
	ImmutableBiMap<S, String> children();
	@Nullable S play(String selector);
	@Nullable S play(Random random);
}
