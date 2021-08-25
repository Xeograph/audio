package audio;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WaveEncoder
{
	byte bits;
	String filename;

	public WaveEncoder(final byte bits, final String filename)
	{
		this.bits = bits;
		this.filename = filename;
	}

	public void encode(final WaveData in) throws Exception
	{
		int fileSize = 44;
		if (in.stereo())
		{
			fileSize += 2 * (bits / 8) * in.samples();
		}
		else
		{
			fileSize += bits / 8 * in.samples();
		}

		final OutputStream outputStream = new FileOutputStream(filename);
		final byte[] data = new byte[fileSize];
		final ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		bb.position(0);
		bb.put((byte) 'R');
		bb.put((byte) 'I');
		bb.put((byte) 'F');
		bb.put((byte) 'F');
		// 4

		bb.putInt(fileSize - 8);
		// 8

		bb.put((byte) 'W');
		bb.put((byte) 'A');
		bb.put((byte) 'V');
		bb.put((byte) 'E');
		bb.put((byte) 'f');
		bb.put((byte) 'm');
		bb.put((byte) 't');
		bb.put((byte) ' ');
		// 16

		bb.putInt(16);
		// 20
		bb.putShort((short) 1);
		// 22

		if (in.stereo())
		{
			bb.putShort((short) 2);
		}
		else
		{
			bb.putShort((short) 1);
		}
		// 24

		bb.putInt(in.rate());
		// 28

		int byteRate = in.rate() * (bits / 8);
		if (in.stereo())
		{
			byteRate *= 2;
		}

		bb.putInt(byteRate);
		// 32

		short alignment = (short) (bits / 8);
		if (in.stereo())
		{
			alignment *= 2;
		}

		bb.putShort(alignment);
		// 34
		bb.putShort(bits);
		// 36

		bb.put((byte) 'd');
		bb.put((byte) 'a');
		bb.put((byte) 't');
		bb.put((byte) 'a');
		// 40

		int dataSize = in.samples() * (bits / 8);
		if (in.stereo())
		{
			dataSize *= 2;
		}

		bb.putInt(dataSize);
		// 44

		// Data starts here
		if (bits == 8)
		{
			if (!in.stereo())
			{
				mono8Bit(in, bb);
			}
			else
			{
				stereo8Bit(in, bb);
			}
		}
		else if (bits == 16)
		{
			if (!in.stereo())
			{
				mono16Bit(in, bb);
			}
			else
			{
				stereo16Bit(in, bb);
			}
		}
		else if (bits == 32)
		{
			if (!in.stereo())
			{
				mono32Bit(in, bb);
			}
			else
			{
				stereo32Bit(in, bb);
			}
		}
		else if (!in.stereo())
		{
			mono64Bit(in, bb);
		}
		else
		{
			stereo64Bit(in, bb);
		}

		outputStream.write(data);
		outputStream.close();
	}

	private void mono16Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			int val = (int) Math.round(in.channel1()[i] * 32768);
			if (val < -32768)
			{
				val = -32768;
			}

			if (val > 32767)
			{
				val = 32767;
			}

			bb.putShort((short) val);
		}
	}

	private void mono32Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			long val = Math.round(in.channel1()[i] * 2147483648.0);
			if (val < -2147483648l)
			{
				val = 2147483648l;
			}

			if (val > 2147483647l)
			{
				val = 2147483647l;
			}

			bb.putInt((int) val);
		}
	}

	private void mono64Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			final long val = Math.round(in.channel1()[i] * 9223372036854775808.0);
			bb.putLong(val);
		}
	}

	private void mono8Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			int val = (int) Math.round((in.channel1()[i] + 1.0) * 127.5);
			if (val < 0)
			{
				val = 0;
			}

			if (val > 255)
			{
				val = 255;
			}

			bb.put((byte) val);
		}
	}

	private void stereo16Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			int val = (int) Math.round(in.channel1()[i] * 32768);
			if (val < -32768)
			{
				val = -32768;
			}

			if (val > 32767)
			{
				val = 32767;
			}

			bb.putShort((short) val);

			val = (int) Math.round(in.channel2()[i] * 32768);
			if (val < -32768)
			{
				val = -32768;
			}

			if (val > 32767)
			{
				val = 32767;
			}

			bb.putShort((short) val);
		}
	}

	private void stereo32Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			long val = Math.round(in.channel1()[i] * 2147483648.0);
			if (val < -2147483648l)
			{
				val = 2147483648l;
			}

			if (val > 2147483647l)
			{
				val = 2147483647l;
			}

			bb.putInt((int) val);

			val = Math.round(in.channel2()[i] * 2147483648.0);
			if (val < -2147483648l)
			{
				val = 2147483648l;
			}

			if (val > 2147483647l)
			{
				val = 2147483647l;
			}

			bb.putInt((int) val);
		}
	}

	private void stereo64Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			long val = Math.round(in.channel1()[i] * 9223372036854775808.0);
			bb.putLong(val);

			val = Math.round(in.channel2()[i] * 9223372036854775808.0);
			bb.putLong(val);
		}
	}

	private void stereo8Bit(final WaveData in, final ByteBuffer bb)
	{
		for (int i = 0; i < in.samples(); i++)
		{
			int val = (int) Math.round((in.channel1()[i] + 1.0) * 127.5);
			if (val < 0)
			{
				val = 0;
			}

			if (val > 255)
			{
				val = 255;
			}

			bb.put((byte) val);

			val = (int) Math.round((in.channel2()[i] + 1.0) * 127.5);
			if (val < 0)
			{
				val = 0;
			}

			if (val > 255)
			{
				val = 255;
			}

			bb.put((byte) val);
		}
	}
}
