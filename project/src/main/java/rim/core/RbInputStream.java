package rim.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * 読み込み専用バイナリInputStream.
 * (ReadBinaryInputStream).
 */
public class RbInputStream extends InputStream {
	private byte[] binary;
	private int offset;
	private int length;
	
	private int position;
	
	/**
	 * コンストラクタ.
	 * @param b 対象のバイナリを設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象の長さを設定します.
	 */
	public RbInputStream(byte[] b, int off, int len) {
		binary = b;
		offset = off;
		length = len;
		position = 0;
	}
	
	@Override
	public void close() throws IOException {
		binary = null;
		offset = 0;
		length = 0;
		position = 0;
	}
	
	// クローズチェック.
	private final void checkClose() throws IOException {
		if(binary == null) {
			throw new IOException("It's already closed.");
		}
	}

	@Override
	public int read() throws IOException {
		checkClose();
		if(position >= length) {
			return -1;
		}
		return binary[offset + position++] & 0x000000ff;
	}
	
	@Override
	public int read(byte[] out) throws IOException {
		return read(out, 0, out.length);
	}
	
	@Override
	public int read(byte[] out, int off, int len)
		throws IOException {
		checkClose();
		final int p = offset + position;
		int ret = length - p;
		if(ret <= 0) {
			return -1;
		} else if(ret > len) {
			ret = len;
		}
		System.arraycopy(binary, p, out, off, ret);
		position += ret;
		return ret;
	}
	
	@Override
	public long skip(long n) throws IOException {
		checkClose();
		int ret = length - (offset + position);
		if(ret <= 0) {
			return -1;
		} else if(ret > n) {
			ret = (int)n;
		}
		position += ret;
		return (long)ret;
	}
	
	@Override
	public int available() throws IOException {
		checkClose();
		return length - (offset + position);
	}
	
	@Override
	public String toString() {
		return new StringBuilder("offset: ")
			.append(offset).append(" length: ")
			.append(length).append(" position: ")
			.append(position).toString();
	}
}
