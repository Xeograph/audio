package audio;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SquareRecoder
{
	class WorkThread implements Runnable
	{
		WaveData in;
		double[] target;
		double[] source;
		AtomicInteger blocks;
		int index;
		int numBlocks;

		public WorkThread(final WaveData in, final double[] target, final double[] source, final AtomicInteger blocks, final int index, final int numBlocks)
		{
			this.in = in;
			this.target = target;
			this.source = source;
			this.blocks = blocks;
			this.index = index;
			this.numBlocks = numBlocks;
		}

		@Override
		public void run()
		{
			final int ampSteps = (int) Math.round((Math.pow(2.0, squareChannelBits) - 2) / 2);
			final int numSamples = in.samples();
			final int beginI = numSamples / synthModRate / numThreads * synthModRate * index;
			final int endI = numSamples / synthModRate / numThreads * synthModRate * (index + 1);

			for (int i = beginI; i + synthModRate <= endI; i += synthModRate)
			{
				double minError = 0;
				for (int j = i; j < i + synthModRate; j++)
				{
					minError += Math.abs(source[j]);
				}

				int minErrorFreq = synthMinFreq;
				int minErrorStep = 0;

				// Sweep through frequencies
				for (int z = synthMinFreq; z <= synthMaxFreq; z++)
				{
					// Sweep through positive amplitudes
					for (int a = 1; a <= ampSteps; a++)
					{
						// Calculate error over this block with these settings
						final double error = calcError(source, i, z, a, ampSteps);
						if (error < minError)
						{
							minErrorFreq = z;
							minErrorStep = a;
							minError = error;
						}
					}

					// Sweep through negative amplitudes
					for (int a = 1; a <= ampSteps; a++)
					{
						// Calculate error over this block with these settings
						final double error = calcError(source, i, z, a, ampSteps);
						if (error < minError)
						{
							minErrorFreq = z;
							minErrorStep = -a;
							minError = error;
						}
					}
				}

				// Generate the data based on best fit for this block
				for (int j = i; j < i + synthModRate; j++)
				{
					target[j] = getLookup(minErrorFreq, minErrorStep, j - i, ampSteps);
					sumAbsoluteError += Math.abs(target[j] - in.channel2()[j]);
					count.incrementAndGet();
				}

				final int count = blocks.incrementAndGet();
				System.out.println(count + "/" + numBlocks);
			}
		}
	}

	private final int squareChannelBits;
	private final int synthModRate;
	private final int synthMinFreq;
	private final int synthMaxFreq;
	volatile double sumAbsoluteError = 0.0;
	AtomicLong count = new AtomicLong(0);
	double[][][] lookup;
	AtomicInteger blocks;
	int numBlocks;
	int numThreads;

	public SquareRecoder(final int squareChannelBits, final int synthModRate, final int synthMinFreq, final int synthMaxFreq, final AtomicInteger blocks, final int numBlocks, final int numThreads)
	{
		this.squareChannelBits = squareChannelBits;
		this.synthModRate = synthModRate;
		this.synthMinFreq = synthMinFreq;
		this.synthMaxFreq = synthMaxFreq;
		this.blocks = blocks;
		this.numBlocks = numBlocks;
		this.numThreads = numThreads;
	}

	public double averageError()
	{
		return sumAbsoluteError / count.get();
	}

	private double calcError(final double[] data, final int start, final int f, final int step, final int numSteps)
	{
		double error = 0;
		for (int i = start; i < start + synthModRate; i++)
		{
			error += Math.abs(data[i] - getLookup(f, step, i - start, numSteps));
		}

		return error;
	}

	double getLookup(final int f, final int step, final int sampleNum, final int numSteps)
	{
		if (step == 0)
		{
			return 0;
		}

		if (step > 0)
		{
			return lookup[f - synthMinFreq][step - 1][sampleNum];
		}

		return lookup[f - synthMinFreq][step + numSteps - 1][sampleNum];
	}

	public WaveData recode(final WaveData in)
	{
		// Build lookup
		final int numFreqs = synthMaxFreq - synthMinFreq + 1;
		final int ampSteps = (int) Math.round((Math.pow(2.0, squareChannelBits) - 2) / 2);
		lookup = new double[numFreqs][ampSteps * 2][synthModRate];
		for (int z = synthMinFreq; z <= synthMaxFreq; z++)
		{
			for (int a = 1; a <= ampSteps; a++)
			{
				for (int s = 0; s < synthModRate; s++)
				{
					lookup[z - synthMinFreq][a - 1][s] = square(z, a * 1.0 / ampSteps, s * 1.0 / in.rate());
				}
			}

			for (int a = 1; a <= ampSteps; a++)
			{
				for (int s = 0; s < synthModRate; s++)
				{
					lookup[z - synthMinFreq][a + ampSteps - 1][s] = square(z, a * -1.0 / ampSteps, s * 1.0 / in.rate());
				}
			}
		}

		// Do left channel first as it always exists
		final double[] left = new double[in.samples()];
		final ArrayList<Thread> leftThreads = new ArrayList<>();
		final ArrayList<Thread> rightThreads = new ArrayList<>();
		for (int i = 0; i < numThreads; i++)
		{
			final WorkThread runner = new WorkThread(in, left, in.channel1(), blocks, i, numBlocks);
			final Thread thread = new Thread(runner);
			thread.start();
			leftThreads.add(thread);
		}

		if (!in.stereo())
		{
			for (final Thread thread : leftThreads)
			{
				while (true)
				{
					try
					{
						thread.join();
						break;
					}
					catch (final Exception e)
					{
					}
				}
			}

			return new WaveData(left, in.rate());
		}

		final double[] right = new double[in.samples()];
		for (int i = 0; i < numThreads; i++)
		{
			final WorkThread runner = new WorkThread(in, right, in.channel2(), blocks, i, numBlocks);
			final Thread thread = new Thread(runner);
			thread.start();
			rightThreads.add(thread);
		}

		for (final Thread thread : leftThreads)
		{
			while (true)
			{
				try
				{
					thread.join();
					break;
				}
				catch (final Exception e)
				{
				}
			}
		}

		for (final Thread thread : rightThreads)
		{
			while (true)
			{
				try
				{
					thread.join();
					break;
				}
				catch (final Exception e)
				{
				}
			}
		}

		return new WaveData(left, right, in.rate());
	}

	private double square(final int f, final double a, final double t)
	{
		final double retval = a * Math.signum(Math.sin(2.0 * Math.PI * f * t));
		return retval;
	}
}
