package audio;

import java.util.Arrays;

public class WaveData
{
	private final boolean stereo;
	private final int rate;
	private final int samples;
	private final double[] left;
	private final double[] right;
	private int srcBits = 0;

	public WaveData(final double[] left, final double[] right, final int rate)
	{
		stereo = true;
		this.rate = rate;
		samples = Math.min(left.length, right.length);
		this.left = left;
		this.right = right;
	}

	public WaveData(final double[] data, final int rate)
	{
		stereo = false;
		this.rate = rate;
		samples = data.length;
		left = data;
		right = null;
	}

	public WaveData add(final WaveData r) throws Exception
	{
		WaveData lhs = this;
		WaveData rhs = r;
		if (lhs.rate != rhs.rate)
		{
			throw new Exception("Rates must be equal to add wave data");
		}

		final int numSamples = Math.min(samples, rhs.samples);
		if (!stereo && r.stereo)
		{
			lhs = lhs.convertToStereo();
		}

		if (stereo && !r.stereo)
		{
			rhs = rhs.convertToStereo();
		}

		final double[] left = new double[numSamples];
		if (lhs.stereo)
		{
			final double[] right = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				left[i] = lhs.left[i] + rhs.left[i];
				right[i] = lhs.right[i] + rhs.right[i];
			}

			return new WaveData(left, right, lhs.rate);
		}
		else
		{
			for (int i = 0; i < numSamples; i++)
			{
				left[i] = lhs.left[i] + rhs.left[i];
			}

			return new WaveData(left, lhs.rate);
		}
	}

	private double atTime(final double t, final double[] data)
	{
		final double delta = 1.0 / (rate * 1.0);
		final double index = t / delta;
		final double ceil = Math.ceil(index);
		final double floor = Math.floor(index);

		if (ceil == floor)
		{
			return data[(int) index];
		}

		if ((int) ceil >= data.length)
		{
			return data[(int) floor];
		}

		final double rightWeight = (index - floor) / (ceil - floor);
		final double leftWeight = 1.0 - rightWeight;
		return data[(int) floor] * leftWeight + data[(int) ceil] * rightWeight;
	}

	private double atTime(final double t, final int channel)
	{
		if (channel == 1)
		{
			return atTime(t, left);
		}

		return atTime(t, right);
	}

	public double[] channel1()
	{
		return left;
	}

	public double[] channel2()
	{
		return right;
	}

	public void clear()
	{
		Arrays.fill(left, 0);

		if (right != null)
		{
			Arrays.fill(right, 0);
		}
	}

	public WaveData convertToMono()
	{
		if (!stereo)
		{
			return this;
		}

		final double[] data = new double[samples];
		for (int i = 0; i < samples; i++)
		{
			data[i] = (left[i] + right[i]) / 2.0;
		}

		return new WaveData(data, rate);
	}

	public WaveData convertToStereo()
	{
		if (stereo)
		{
			return this;
		}

		return new WaveData(left, left, rate);
	}

	public int rate()
	{
		return rate;
	}

	public WaveData resample(final int newRate)
	{
		final double length = samples * 1.0 / (rate * 1.0);
		final int newSamples = (int) (length * newRate);
		final double[] left = new double[newSamples];
		double t = 0;
		final double delta = 1.0 / (newRate * 1.0);
		if (!stereo)
		{
			for (int i = 0; i < newSamples; i++)
			{
				left[i] = atTime(t, 1);
				t += delta;
			}

			return new WaveData(left, newRate);
		}

		final double[] right = new double[newSamples];
		for (int i = 0; i < newSamples; i++)
		{
			left[i] = atTime(t, 1);
			right[i] = atTime(t, 2);
			t += delta;
		}

		return new WaveData(left, right, newRate);
	}

	public int samples()
	{
		return samples;
	}

	public void setSrcBits(final int bits)
	{
		srcBits = bits;
	}

	public int srcBits()
	{
		return srcBits;
	}

	public boolean stereo()
	{
		return stereo;
	}

	public WaveData substract(final WaveData r) throws Exception
	{
		WaveData lhs = this;
		WaveData rhs = r;
		if (lhs.rate != rhs.rate)
		{
			throw new Exception("Rates must be equal to subtract wave data");
		}

		final int numSamples = Math.min(samples, rhs.samples);
		if (!stereo && r.stereo)
		{
			lhs = lhs.convertToStereo();
		}

		if (stereo && !r.stereo)
		{
			rhs = rhs.convertToStereo();
		}

		final double[] left = new double[numSamples];
		if (lhs.stereo)
		{
			final double[] right = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				left[i] = lhs.left[i] - rhs.left[i];
				right[i] = lhs.right[i] - rhs.right[i];
			}

			return new WaveData(left, right, lhs.rate);
		}
		else
		{
			for (int i = 0; i < numSamples; i++)
			{
				left[i] = lhs.left[i] - rhs.left[i];
			}

			return new WaveData(left, lhs.rate);
		}
	}
}
