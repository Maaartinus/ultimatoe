package maaartin.game.fivedown;

import java.util.Random;

import com.google.common.collect.ImmutableBiMap;

import junit.framework.TestCase;

public class _FivedownTest extends TestCase {
	public void testPlay_Random() {
		final Random random = new Random(111);
		final Fivedown f0 = Fivedown.INITIAL_GAME;
		final ImmutableBiMap<Fivedown, String> children = f0.children();
		for (int i=0; i<100; ++i) {
			final Fivedown f1 = f0.play(random);
			assertTrue(children.keySet().contains(f1));
		}
	}
}
