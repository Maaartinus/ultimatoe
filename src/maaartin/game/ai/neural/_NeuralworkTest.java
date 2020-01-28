package maaartin.game.ai.neural;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import junit.framework.TestCase;

public class _NeuralworkTest extends TestCase {
	public void testAnd() {
		check(2000, AND_PATTERNS, 2);
	}

	public void testXor() {
		check(2000, XOR_PATTERNS, 4);
	}

	public void testTwoInputPatterns() {
		check(3000, TWO_INPUT_PATTERNS, 5);
	}

	public void testThreeInputPatters() {
		check(3000, THREE_INPUT_PATTERNS, 7);
	}

	public void testXor3() {
		check(3000, makeXor(3), 7);
	}

	public void testXor4() {
		check(8000, makeXor(4), 9);
	}

	public void testXor5() {
		check(6000, makeXor(5), 20);
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

	private static final float[][][] makeXor(int inLength) {
		final int count = 1<<inLength;
		final float[][][] result = new float[count][2][];
		for (int i=0; i<count; ++i) {
			final float[] input = new float[inLength];
			int xor = 0;
			for (int j=0; j<input.length; ++j) {
				final int x = (i>>j) & 1;
				xor ^= x;
				input[j] = x;
			}
			final float output[] = {xor};
			result[i][0] = input;
			result[i][1] = output;
		}
		return result;
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
	private static final float[][][] TWO_INPUT_PATTERNS = {
		{{0, 0}, {1, 1}},
		{{0, 1}, {1, 0}},
		{{1, 0}, {0, 0}},
		{{1, 1}, {1, 1}},
	};

	// majority and xor
	private static final float[][][] THREE_INPUT_PATTERNS = {
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
