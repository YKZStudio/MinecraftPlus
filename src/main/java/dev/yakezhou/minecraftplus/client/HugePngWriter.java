package dev.yakezhou.minecraftplus.client;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

final class HugePngWriter implements AutoCloseable {
	private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
	private final int width;
	private final DataOutputStream output;
	private final IdatOutputStream idat;
	private final Deflater deflater;
	private final DeflaterOutputStream compressed;
	private int writtenRows;
	private boolean closed;

	HugePngWriter(Path path, int width, int height) throws IOException {
		this.width = width;
		this.output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
		this.output.write(SIGNATURE);
		byte[] header = {
			(byte) (width >>> 24), (byte) (width >>> 16), (byte) (width >>> 8), (byte) width,
			(byte) (height >>> 24), (byte) (height >>> 16), (byte) (height >>> 8), (byte) height,
			8, 2, 0, 0, 0
		};
		writeChunk("IHDR", header, 0, header.length);
		this.idat = new IdatOutputStream(this.output);
		this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
		this.compressed = new DeflaterOutputStream(this.idat, this.deflater, 64 * 1024);
	}

	void writeRows(byte[] rgb, int rows) throws IOException {
		if (closed || rows < 0 || rgb.length < Math.multiplyExact(Math.multiplyExact(width, rows), 3)) {
			throw new IllegalArgumentException("Invalid PNG row buffer");
		}
		int stride = width * 3;
		for (int row = 0; row < rows; row++) {
			this.compressed.write(0);
			this.compressed.write(rgb, row * stride, stride);
		}
		this.writtenRows += rows;
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		try {
			this.compressed.finish();
			this.idat.finishChunk();
			writeChunk("IEND", new byte[0], 0, 0);
		} finally {
			try {
				this.output.close();
			} finally {
				this.deflater.end();
			}
		}
	}

	int writtenRows() {
		return this.writtenRows;
	}

	private void writeChunk(String type, byte[] data, int offset, int length) throws IOException {
		byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		CRC32 crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data, offset, length);
		this.output.writeInt(length);
		this.output.write(typeBytes);
		this.output.write(data, offset, length);
		this.output.writeInt((int) crc.getValue());
	}

	private static final class IdatOutputStream extends OutputStream {
		private static final int CHUNK_SIZE = 64 * 1024;
		private final DataOutputStream output;
		private final byte[] buffer = new byte[CHUNK_SIZE];
		private int count;

		private IdatOutputStream(DataOutputStream output) {
			this.output = output;
		}

		@Override
		public void write(int value) throws IOException {
			if (this.count == this.buffer.length) {
				finishChunk();
			}
			this.buffer[this.count++] = (byte) value;
		}

		@Override
		public void write(byte[] data, int offset, int length) throws IOException {
			while (length > 0) {
				int copied = Math.min(length, this.buffer.length - this.count);
				System.arraycopy(data, offset, this.buffer, this.count, copied);
				this.count += copied;
				offset += copied;
				length -= copied;
				if (this.count == this.buffer.length) {
					finishChunk();
				}
			}
		}

		private void finishChunk() throws IOException {
			if (this.count == 0) {
				return;
			}
			byte[] type = {'I', 'D', 'A', 'T'};
			CRC32 crc = new CRC32();
			crc.update(type);
			crc.update(this.buffer, 0, this.count);
			this.output.writeInt(this.count);
			this.output.write(type);
			this.output.write(this.buffer, 0, this.count);
			this.output.writeInt((int) crc.getValue());
			this.count = 0;
		}
	}
}
