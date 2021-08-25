package audio;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class DPCM
{
	public static void main(final String[] args) throws Exception
	{
		if (args.length == 0)
		{
			System.out.println("Usage: DPCM <properties file filename>");
			System.exit(0);
		}

		final String fn = args[0];
		final Properties prop = new Properties();
		final InputStream inputStream = new FileInputStream(fn);
		prop.load(inputStream);
		inputStream.close();

		// Properties file is key-value pairs, one per line
		// inputFile=myfile.wav
		// stereo=true
		final String inputFile = prop.getProperty("inputFile");
		if (inputFile == null)
		{
			System.out.println("No input file was provided");
			System.exit(0);
		}

		WaveData data = WaveDecoder.decode(inputFile);
		if (data.stereo())
		{
			System.out.println("Input data is in stereo");
		}
		else
		{
			System.out.println("Input data is mono");
		}

		System.out.println("Sampling rate is " + data.rate());
		System.out.println("Samples are " + data.srcBits() + " bits");

		// If stereo is true, force stereo
		// If stereo is false, force mono
		// Otherwise, keep same as source
		final String stereo = prop.getProperty("stereo");
		if (stereo != null)
		{
			if (stereo.equals("true"))
			{
				data = data.convertToStereo();
			}
			else if (stereo.equals("false"))
			{
				data = data.convertToMono();
			}
		}

		// sampleRate is the output sample rate
		final String sr = prop.getProperty("sampleRate");
		if (sr == null)
		{
			System.out.println("No output sample rate was specified");
			System.exit(0);
		}

		final int rate = Integer.parseInt(sr);
		if (rate <= 0)
		{
			System.out.println("An invalid output sample rate was specified");
			System.exit(0);
		}

		data = data.resample(rate);
		final WaveData originalData = data.resample(rate);
		data.clear();

		String pcmChan = prop.getProperty("pcmChannels");
		if (pcmChan == null)
		{
			pcmChan = "0";
		}

		final int pcmChannels = Integer.parseInt(pcmChan);

		if (pcmChannels > 0)
		{
			// deltaBits and output bits are the properties needed by the DPCM recoder
			final String db = prop.getProperty("deltaBits");
			final String ob = prop.getProperty("outputBits");
			if (db == null || ob == null)
			{
				System.out.println("Both outputBits and deltaBits must be specified");
				System.exit(0);
			}

			final byte deltaBits = Byte.parseByte(db);
			final byte outputBits = Byte.parseByte(ob);
			if (deltaBits <= 0 || outputBits <= 0)
			{
				System.out.println("Both outputBits and deltaBits must be positive");
				System.exit(0);
			}

			DPCMRecoder coder = new DPCMRecoder(deltaBits, outputBits);
			WaveData remainder = originalData.substract(data);
			remainder = coder.recode(remainder);
			data = data.add(remainder);

			System.out.println("Average absolute error in DPCM encoding was: " + coder.averageError());

			for (int i = 1; i < pcmChannels; i++)
			{
				remainder = originalData.substract(data);
				coder = new DPCMRecoder(deltaBits, outputBits);
				remainder = coder.recode(remainder);
				System.out.println("Average absolute error in DPCM encoding was: " + coder.averageError());

				data = data.add(remainder);
			}
		}

		String th = prop.getProperty("threads");
		if (th == null)
		{
			th = "1";
		}

		final int threads = Integer.parseInt(th);

		String smr = prop.getProperty("synthBlockSize"); // In samples at output sample rate
		if (smr == null)
		{
			smr = "128"; // About 240Hz at 32khz
		}

		final int synthModRate = Integer.parseInt(smr);

		String smf = prop.getProperty("synthMinFreq");
		if (smf == null)
		{
			smf = "22";
		}

		final int synthMinFreq = Integer.parseInt(smf);

		smf = prop.getProperty("synthMaxFreq");
		if (smf == null)
		{
			smf = "12428";
		}

		final int synthMaxFreq = Integer.parseInt(smf);

		String squareChan = prop.getProperty("squareChannels");
		if (squareChan == null)
		{
			squareChan = "0";
		}

		final int squareChannels = Integer.parseInt(squareChan);

		String sawtoothChan = prop.getProperty("sawtoothChannels");
		if (sawtoothChan == null)
		{
			sawtoothChan = "0";
		}

		final int sawtoothChannels = Integer.parseInt(sawtoothChan);

		String sineChan = prop.getProperty("sineChannels");
		if (sineChan == null)
		{
			sineChan = "0";
		}

		final int sineChannels = Integer.parseInt(sineChan);

		String triangleChan = prop.getProperty("triangleChannels");
		if (triangleChan == null)
		{
			triangleChan = "0";
		}

		final int triangleChannels = Integer.parseInt(triangleChan);

		final AtomicInteger blocks = new AtomicInteger(0);
		int numBlocks = data.samples() / synthModRate;
		final int numChannels = squareChannels + sawtoothChannels + sineChannels + triangleChannels;
		numBlocks *= numChannels;
		if (data.stereo())
		{
			numBlocks *= 2;
		}

		if (squareChannels > 0)
		{
			// squareChannelBits is a needed property at this point
			// frequency is always assumed to be integer for simplicity
			// bits represents allowed peak points
			final String scb = prop.getProperty("squareChannelBits");
			if (scb == null)
			{
				System.out.println("Square channel bits must be specified");
				System.exit(0);
			}

			final byte squareChannelBits = Byte.parseByte(scb);
			if (squareChannelBits <= 0)
			{
				System.out.println("Square channel bits must be positive");
				System.exit(0);
			}

			SquareRecoder coder = new SquareRecoder(squareChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
			WaveData remainder = originalData.substract(data);
			remainder = coder.recode(remainder);
			data = data.add(remainder);

			System.out.println("Average absolute error in square channel encoding was: " + coder.averageError());

			for (int i = 1; i < squareChannels; i++)
			{
				remainder = originalData.substract(data);
				coder = new SquareRecoder(squareChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
				remainder = coder.recode(remainder);
				System.out.println("Average absolute error in square channel encoding was: " + coder.averageError());

				data = data.add(remainder);
			}
		}

		if (sawtoothChannels > 0)
		{
			// sawtoothChannelBits is a needed property at this point
			// frequency is always assumed to be integer for simplicity
			final String scb = prop.getProperty("sawtoothChannelBits");
			if (scb == null)
			{
				System.out.println("Sawthooth channel bits must be specified");
				System.exit(0);
			}

			final byte sawtoothChannelBits = Byte.parseByte(scb);
			if (sawtoothChannelBits <= 0)
			{
				System.out.println("Sawtooth channel bits must be positive");
				System.exit(0);
			}

			SawtoothRecoder coder = new SawtoothRecoder(sawtoothChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
			WaveData remainder = originalData.substract(data);
			remainder = coder.recode(remainder);
			data = data.add(remainder);

			System.out.println("Average absolute error in sawtooth channel encoding was: " + coder.averageError());

			for (int i = 1; i < sawtoothChannels; i++)
			{
				remainder = originalData.substract(data);
				coder = new SawtoothRecoder(sawtoothChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
				remainder = coder.recode(remainder);
				System.out.println("Average absolute error in sawtooth channel encoding was: " + coder.averageError());

				data = data.add(remainder);
			}
		}

		if (sineChannels > 0)
		{
			// sineChannelBits is a needed property at this point
			// frequency is always assumed to be integer for simplicity
			final String scb = prop.getProperty("sineChannelBits");
			if (scb == null)
			{
				System.out.println("Sine channel bits must be specified");
				System.exit(0);
			}

			final byte sineChannelBits = Byte.parseByte(scb);
			if (sineChannelBits <= 0)
			{
				System.out.println("Sine channel bits must be positive");
				System.exit(0);
			}

			SineRecoder coder = new SineRecoder(sineChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
			WaveData remainder = originalData.substract(data);
			remainder = coder.recode(remainder);
			data = data.add(remainder);

			System.out.println("Average absolute error in sine channel encoding was: " + coder.averageError());

			for (int i = 1; i < sineChannels; i++)
			{
				remainder = originalData.substract(data);
				coder = new SineRecoder(sineChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
				remainder = coder.recode(remainder);
				System.out.println("Average absolute error in sine channel encoding was: " + coder.averageError());

				data = data.add(remainder);
			}
		}

		if (triangleChannels > 0)
		{
			// triangleChannelBits is a needed property at this point
			// frequency is always assumed to be integer for simplicity
			final String scb = prop.getProperty("triangleChannelBits");
			if (scb == null)
			{
				System.out.println("Triangle channel bits must be specified");
				System.exit(0);
			}

			final byte triangleChannelBits = Byte.parseByte(scb);
			if (triangleChannelBits <= 0)
			{
				System.out.println("Triangle channel bits must be positive");
				System.exit(0);
			}

			TriangleRecoder coder = new TriangleRecoder(triangleChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
			WaveData remainder = originalData.substract(data);
			remainder = coder.recode(remainder);
			data = data.add(remainder);

			System.out.println("Average absolute error in triangle channel encoding was: " + coder.averageError());

			for (int i = 1; i < triangleChannels; i++)
			{
				remainder = originalData.substract(data);
				coder = new TriangleRecoder(triangleChannelBits, synthModRate, synthMinFreq, synthMaxFreq, blocks, numBlocks, threads);
				remainder = coder.recode(remainder);
				System.out.println("Average absolute error in triangle channel encoding was: " + coder.averageError());

				data = data.add(remainder);
			}
		}

		// Lastly we need an outputFile and resultBits (outputBits is DPCM resolution,
		// not final wav resolution)
		final String outFn = prop.getProperty("outputFile");
		if (outFn == null)
		{
			System.out.println("An output file was not specified");
			System.exit(0);
		}

		final String rb = prop.getProperty("resultBits");
		if (rb == null)
		{
			System.out.println("The resultBits property was not specified");
			System.exit(0);
		}

		final byte resultBits = Byte.parseByte(rb);
		final WaveEncoder encoder = new WaveEncoder(resultBits, outFn);
		encoder.encode(data);
	}
}
