package audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WaveDecoder
{
	static public WaveData decode(final String filename) throws Exception
	{
		final InputStream inputStream = new FileInputStream(filename);
		final long fileSize = new File(filename).length();
		final byte[] allBytes = new byte[(int) fileSize];
		inputStream.read(allBytes);
		inputStream.close();

		final ByteBuffer bb = ByteBuffer.wrap(allBytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		String format = new String(allBytes, 0, 4);
		if (!format.equals("RIFF"))
		{
			throw new Exception("File is not a wav file");
		}

		bb.position(4);
		final long expectedSize = bb.getInt() + 8;
		if (fileSize != expectedSize)
		{
			throw new Exception("File is corrupted");
		}

		format = new String(allBytes, 8, 4);
		if (!format.equals("WAVE"))
		{
			throw new Exception("File is not a wav file");
		}

		// Don't check last byte here, as there seems to have been some debate over the
		// years
		// as to whether its null or space (0x00 or 0x20)
		format = new String(allBytes, 12, 3);
		if (!format.equals("fmt"))
		{
			throw new Exception("File is not a wav file");
		}

		bb.position(16);
		if (bb.getInt() != 16)
		{
			throw new Exception("Only uncompressed WAV files are supported");
		}

		if (bb.getShort() != 1)
		{
			throw new Exception("Only uncompressed WAV files are supported");
		}

		final short channels = bb.getShort();
		if (channels != 1 && channels != 2)
		{
			throw new Exception("Only mono and stereo files are supported");
		}

		final int rate = bb.getInt();
		if (rate <= 0)
		{
			throw new Exception("Unsupported sampling rate");
		}

		final int byteRate = bb.getInt();
		if (byteRate <= 0)
		{
			throw new Exception("Unsupported byte rate");
		}

		final short alignment = bb.getShort();
		if (alignment <= 0)
		{
			throw new Exception("Unsupported alignment");
		}

		final short bits = bb.getShort();
		if (bits != 8 && bits != 16 && bits != 32 && bits != 64)
		{
			throw new Exception("Unsupported number of bits per sample");
		}

		final int expectedByteRate = rate * channels * (bits / 8);
		if (expectedByteRate != byteRate)
		{
			throw new Exception("File is corrupted");
		}

		final short expectedAlignment = (short) (channels * (bits / 8));
		if (expectedAlignment != alignment)
		{
			throw new Exception("File is corrupted");
		}

		int dataOffset = 36;
		while (dataOffset < fileSize - 4)
		{
			format = new String(allBytes, dataOffset, 4);
			if (format.equals("data"))
			{
				break;
			}

			++dataOffset;
		}

		if (dataOffset == fileSize - 4)
		{
			throw new Exception("File has no data section");
		}

		bb.position(dataOffset + 4);
		final int samples = bb.getInt() / (channels * (bits / 8));

		if (bits == 8)
		{
			if (channels == 1)
			{
				return mono8Bit(bb, rate, dataOffset + 8, samples);
			}

			if (channels == 2)
			{
				return stereo8Bit(bb, rate, dataOffset + 8, samples);
			}
		}

		if (bits == 16)
		{
			if (channels == 1)
			{
				return mono16Bit(bb, rate, dataOffset + 8, samples);
			}

			if (channels == 2)
			{
				return stereo16Bit(bb, rate, dataOffset + 8, samples);
			}
		}

		if (bits == 32)
		{
			if (channels == 1)
			{
				return mono32Bit(bb, rate, dataOffset + 8, samples);
			}

			if (channels == 2)
			{
				return stereo32Bit(bb, rate, dataOffset + 8, samples);
			}
		}

		if (channels == 1)
		{
			return mono64Bit(bb, rate, dataOffset + 8, samples);
		}

		return stereo64Bit(bb, rate, dataOffset + 8, samples);
	}

	private static WaveData mono16Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] data = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			final short val = bb.getShort();
			data[sampleNum] = val / 32768.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(data, rate);
		retval.setSrcBits(16);
		return retval;
	}

	private static WaveData mono32Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] data = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			final int val = bb.getInt();
			data[sampleNum] = val / 2147483648.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(data, rate);
		retval.setSrcBits(132);
		return retval;
	}

	private static WaveData mono64Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] data = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			final long val = bb.getLong();
			data[sampleNum] = val / 9223372036854775808.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(data, rate);
		retval.setSrcBits(64);
		return retval;
	}

	private static WaveData mono8Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] data = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			final int val = bb.get() & 0xff;
			data[sampleNum] = val * 1.0 / 127.5 - 1.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(data, rate);
		retval.setSrcBits(8);
		return retval;
	}

	private static WaveData stereo16Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] left = new double[samples];
		final double[] right = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			short val = bb.getShort();
			left[sampleNum] = val / 32768.0;

			val = bb.getShort();
			right[sampleNum] = val / 32768.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(left, right, rate);
		retval.setSrcBits(16);
		return retval;
	}

	private static WaveData stereo32Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] left = new double[samples];
		final double[] right = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			int val = bb.getInt();
			left[sampleNum] = val / 2147483648.0;

			val = bb.getInt();
			right[sampleNum] = val / 2147483648.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(left, right, rate);
		retval.setSrcBits(32);
		return retval;
	}

	private static WaveData stereo64Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] left = new double[samples];
		final double[] right = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			long val = bb.getLong();
			left[sampleNum] = val / 9223372036854775808.0;

			val = bb.getLong();
			right[sampleNum] = val / 9223372036854775808.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(left, right, rate);
		retval.setSrcBits(64);
		return retval;
	}

	private static WaveData stereo8Bit(final ByteBuffer bb, final int rate, final int dataStart, final int samples)
	{
		final double[] left = new double[samples];
		final double[] right = new double[samples];
		int sampleNum = 0;
		bb.position(dataStart);
		while (sampleNum < samples)
		{
			int val = bb.get() & 0xff;
			left[sampleNum] = val * 1.0 / 127.5 - 1.0;

			val = bb.get() & 0xff;
			right[sampleNum] = val * 1.0 / 127.5 - 1.0;
			++sampleNum;
		}

		final WaveData retval = new WaveData(left, right, rate);
		retval.setSrcBits(8);
		return retval;
	}
}
