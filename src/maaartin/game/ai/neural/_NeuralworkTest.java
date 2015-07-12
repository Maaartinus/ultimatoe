package maaartin.game.ai.neural;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import junit.framework.TestCase;

public class _NeuralworkTest extends TestCase {
	public void testAnd() {
		check(10000, AND_PATTERNS, 2);
	}

	public void testXor() {
		check(10000, XOR_PATTERNS, 4);
	}

	public void testBinary() {
		check(10000, BINARY_PATTERNS, 5);
	}

	public void testTernary() {
		check(10000, TERNARY_PATTERNS, 7);
	}

	private void check(int limit, float[][][] patterns, int hiddenLength) {
		check(limit, patterns, hiddenLength, Integer.MAX_VALUE);
	}

	private void check(int limit, float[][][] patterns, int hiddenLength, int arity) {
		patterns = patterns.clone();
		final Random random = new Random(0);
		for (int m=0; m<1000; ++m) {
			Collections.shuffle(Arrays.asList(patterns), random);
			final int inLength = patterns[0][0].length;
			final int outLength = patterns[0][1].length;
			final Neuralwork neuralwork = new Neuralwork(inLength, hiddenLength, outLength, random, arity);
			final float[] output = new float[outLength];
			for (int n=0; ; ++n) {
				boolean success = true;
				for (final float[][] io : patterns) {
					final float[] input = io[0];
					final float[] desired = io[1];
					neuralwork.input(input);
					neuralwork.output(output);
					boolean valid = true;
					for (int i=0; i<output.length; ++i) valid &= Math.abs(desired[i] - output[i]) < 0.25;
					if (valid) continue;
					success = false;
					neuralwork.learn(desired, 0.2f);
				}
				if (success) break;
				assertTrue("too many iterations", n<limit);
			}
		}
	}

	private static final float[][][] AND_PATTERNS = {
		{{0, 0}, {0}},
		{{0, 1}, {0}},
		{{1, 0}, {0}},
		{{1, 1}, {1}},
	};

	private static final float[][][] XOR_PATTERNS = {
		{{0, 0}, {0}},
		{{0, 1}, {1}},
		{{1, 0}, {1}},
		{{1, 1}, {0}},
	};

	// implication and nxor
	private static final float[][][] BINARY_PATTERNS = {
		{{0, 0}, {1, 1}},
		{{0, 1}, {1, 0}},
		{{1, 0}, {0, 0}},
		{{1, 1}, {1, 1}},
	};

	// majority and xor
	private static final float[][][] TERNARY_PATTERNS = {
		{{0, 0, 0}, {0, 0}},
		{{0, 0, 1}, {0, 1}},
		{{0, 1, 0}, {0, 1}},
		{{0, 1, 1}, {1, 0}},
		{{1, 0, 0}, {0, 1}},
		{{1, 0, 1}, {1, 0}},
		{{1, 1, 0}, {1, 0}},
		{{1, 1, 1}, {1, 1}},
	};
}
