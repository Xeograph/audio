package audio;

public class DPCMRecoder
{
	byte deltaBits;
	byte outputBits;
	double sumAbsoluteError = 0.0;
	long count = 0;

	public DPCMRecoder(final byte deltaBits, final byte outputBits)
	{
		this.deltaBits = deltaBits;
		this.outputBits = outputBits;
	}

	public double averageError()
	{
		return sumAbsoluteError / count;
	}

	public WaveData recode(final WaveData in)
	{
		if (deltaBits == 1)
		{
			// Has no concept of staying unchanged - it's weird
			// This is straight up DPCM
			return recode1Bit(in);
		}

		if (deltaBits == 2)
		{
			// Just do 2 bit DPCM
			return recode2Bit(in);
		}

		// There's so many variations on ADPCM, let's just pick one that's as good as
		// any other
		final int numSamples = in.samples();
		double val = 0.0;
		final double delta = 2.0 / (Math.pow(2.0, outputBits) - 1);
		final int maxDeltas = (int) (Math.pow(2.0, deltaBits - 1) - 2) / 2;
		final int minDeltas = -maxDeltas;
		int scale = 1;
		final double[] left = new double[numSamples];
		if (in.stereo())
		{
			final double[] right = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel1()[i] - val) / delta);
				if (deltas >= maxDeltas * scale)
				{
					deltas = maxDeltas * scale;
					scale *= 2;
				}
				else if (deltas <= minDeltas * scale)
				{
					deltas = minDeltas * scale;
					scale *= 2;
				}
				else if (scale > 1)
				{
					scale /= 2;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			val = 0.0;
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel2()[i] - val) / delta);
				if (deltas >= maxDeltas * scale)
				{
					deltas = maxDeltas * scale;
					scale *= 2;
				}
				else if (deltas <= minDeltas * scale)
				{
					deltas = minDeltas * scale;
					scale *= 2;
				}
				else if (scale > 1)
				{
					scale /= 2;
				}

				val += deltas * delta;
				right[i] = val;
				sumAbsoluteError += Math.abs(right[i] - in.channel2()[i]);
				++count;
			}

			return new WaveData(left, right, in.rate());
		}
		else
		{
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel1()[i] - val) / delta);
				if (deltas >= maxDeltas * scale)
				{
					deltas = maxDeltas * scale;
					scale *= 2;
				}
				else if (deltas <= minDeltas * scale)
				{
					deltas = minDeltas * scale;
					scale *= 2;
				}
				else if (scale > 1)
				{
					scale /= 2;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			return new WaveData(left, in.rate());
		}
	}

	private WaveData recode1Bit(final WaveData in)
	{
		final int numSamples = in.samples();
		double val = 0.0;
		final double delta = 2.0 / (Math.pow(2.0, outputBits) - 1);
		final double[] left = new double[numSamples];
		if (in.stereo())
		{
			final double[] right = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				double deltas = (in.channel1()[i] - val) / delta;
				if (deltas >= 0.0)
				{
					deltas = 1;
				}
				else
				{
					deltas = -1.0;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			val = 0.0;
			for (int i = 0; i < numSamples; i++)
			{
				double deltas = (in.channel2()[i] - val) / delta;
				if (deltas >= 0.0)
				{
					deltas = 1;
				}
				else
				{
					deltas = -1.0;
				}

				val += deltas * delta;
				right[i] = val;
				sumAbsoluteError += Math.abs(right[i] - in.channel1()[i]);
				++count;
			}

			return new WaveData(left, right, in.rate());
		}
		else
		{
			for (int i = 0; i < numSamples; i++)
			{
				double deltas = (in.channel1()[i] - val) / delta;
				if (deltas >= 0.0)
				{
					deltas = 1;
				}
				else
				{
					deltas = -1.0;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			return new WaveData(left, in.rate());
		}
	}

	private WaveData recode2Bit(final WaveData in)
	{
		final int numSamples = in.samples();
		double val = 0.0;
		final double delta = 2.0 / (Math.pow(2.0, outputBits) - 1);
		final int maxDeltas = (int) (Math.pow(2.0, deltaBits) - 2) / 2;
		final int minDeltas = -maxDeltas;
		final double[] left = new double[numSamples];
		if (in.stereo())
		{
			final double[] right = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel1()[i] - val) / delta);
				if (deltas > maxDeltas)
				{
					deltas = maxDeltas;
				}

				if (deltas < minDeltas)
				{
					deltas = minDeltas;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			val = 0.0;
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel2()[i] - val) / delta);
				if (deltas > maxDeltas)
				{
					deltas = maxDeltas;
				}

				if (deltas < minDeltas)
				{
					deltas = minDeltas;
				}

				val += deltas * delta;
				right[i] = val;
				sumAbsoluteError += Math.abs(right[i] - in.channel2()[i]);
				++count;
			}

			return new WaveData(left, right, in.rate());
		}
		else
		{
			for (int i = 0; i < numSamples; i++)
			{
				int deltas = (int) Math.round((in.channel1()[i] - val) / delta);
				if (deltas > maxDeltas)
				{
					deltas = maxDeltas;
				}

				if (deltas < minDeltas)
				{
					deltas = minDeltas;
				}

				val += deltas * delta;
				left[i] = val;
				sumAbsoluteError += Math.abs(left[i] - in.channel1()[i]);
				++count;
			}

			return new WaveData(left, in.rate());
		}
	}
}
