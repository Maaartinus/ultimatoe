package maaartin.game.ai.neural;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;


public final class Neuralwork {
	private static final class Neuron {
		Neuron(int axon, Random random, int arity) {
			checkArgument(arity >= 2);
			checkArgument(axon >= arity);
			checkNotNull(random);
			this.axon = axon;
			weights = new float[arity];
			for (int i=0; i<arity; ++i) weights[i] = 2 * random.nextFloat() - 1;
			synapses = new int[arity];
			for (int i=0; i<arity; ) {
				final int n = random.nextInt(axon);
				if (synapsesBitSet.get(n)) continue;
				synapsesBitSet.set(n);
				synapses[i++] = n;
			}
		}

		void eval(float[] values) {
			values[axon] = sigmoid(linear(values));
		}

		void learn(float[] values, float[] deltas) {
			final float d = sigmoidDerivedY(values[axon]) * deltas[axon];
			addend += d;
			for (int i=0; i<weights.length; ++i) {
				final int j = synapses[i];
				deltas[j] += d * weights[i];
				weights[i] += d * values[j];
			}
		}

		private float linear(float[] values) {
			float result = addend;
			for (int i=0; i<weights.length; ++i) result += weights[i] * values[synapses[i]];
			return result;
		}

		private float sigmoid(float x) {
			final float q = 1 + Math.abs(x);
			return LIMES * x / q;
		}

		private float sigmoidDerived(float x) {
			final float q = 1 + Math.abs(x);
			return LIMES / (q*q);
		}

		private float sigmoidDerivedY(float y) {
			final float y2 = LIMES - Math.abs(y);
			return RECIPROCAL * y2 * y2;
		}

		private static final float LIMES = 1.5f;
		private static final float RECIPROCAL = 1 / LIMES;

		private final int axon;
		private float addend;
		private final float[] weights;
		private final int[] synapses;
		private final BitSet synapsesBitSet = new BitSet();
	}

	public Neuralwork(int inLength, int hiddenLength, int outLength, Random random, int arity) {
		checkArgument(inLength > 0);
		checkArgument(hiddenLength >= 0);
		checkArgument(outLength > 0);
		checkNotNull(random);
		checkArgument(arity>=3);
		this.inLength = inLength;
		this.hiddenLength = hiddenLength;
		this.outLength = outLength;
		neurons = new Neuron[hiddenLength + outLength];
		for (int i=0, j=inLength; i<neurons.length; ++i, ++j) neurons[i] = new Neuron(j, random, Math.min(j, arity));
		final int length = inLength + hiddenLength + outLength;
		values = new float[length];
		deltas = new float[length];
	}

	public void input(float[] input) {
		checkArgument(input.length == inLength);
		System.arraycopy(input, 0, values, 0, inLength);
		for (int i=0; i<neurons.length; ++i) neurons[i].eval(values);
	}

	public void output(float[] output) {
		checkArgument(output.length == outLength);
		System.arraycopy(values, inLength + hiddenLength, output, 0, outLength);
	}

	public void learn(float[] desired, float rate) {
		checkArgument(desired.length == outLength);
		Arrays.fill(deltas, 0);
		for (int i=deltas.length-1, j=outLength-1; j>=0; --i, --j) deltas[i] = rate * (desired[j] - values[i]);
		for (int i=neurons.length-1; i>=0; --i) neurons[i].learn(values, deltas);
	}

	private final int inLength;
	private final int hiddenLength;
	private final int outLength;

	private final Neuron[] neurons;
	private final float[] values;
	private final float[] deltas;
}
