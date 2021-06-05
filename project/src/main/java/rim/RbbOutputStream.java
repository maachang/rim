package rim;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 再利用可能なOutputStream.
 * (ReByteBufferOutputStream).
 */
public class RbbOutputStream extends OutputStream {
	// 最小バッファサイズ.
	private static final int MIN_BUFFER = 32;
	
	// 最小バッファ増加係数.
	private static final double MIN_BUFFER_APPEND_COEFFICIENT = 0.5d;
	
	// 初期バッファ定義値.
	private int initSize;
	// バッファ増加係数.
	private double bufferAppendCoefficient ;
	// 書き込み情報.
	private byte[] buffer;
	// 現在の書き込み位置.
	private int position;
	
	// 総書き込みサイズ.
	private long allLength = 0L;
	// リセット回数.
	private long resetCount = 0L;
	
	/**
	 * コンストラクタ.
	 */
	public RbbOutputStream() {
		this(1024);
	}
	
	/**
	 * コンストラクタ.
	 * @param len 初期バッファサイズを設定します.
	 */
	public RbbOutputStream(int len) {
		this(len, 0.65d);
	}

	/**
	 * コンストラクタ.
	 * @param len 初期バッファサイズを設定します.
	 * @param addCode バッファ増加係数を設定します.
	 *                2を設定した場合、現在の容量が超えた場合は
	 *                その容量の２倍でバッファを再生成します.
	 */
	public RbbOutputStream(int len, double appendCoefficient) {
		if(len < MIN_BUFFER) {
			len =MIN_BUFFER;
		}
		if(appendCoefficient < MIN_BUFFER_APPEND_COEFFICIENT) {
			appendCoefficient = MIN_BUFFER_APPEND_COEFFICIENT;
		}
		initSize = len;
		bufferAppendCoefficient = appendCoefficient;
		buffer = new byte[len];
		position = 0;
	}
	
	// 破棄チェック.
	private void checkDestroy() throws IOException {
		if(buffer == null) {
			throw new IOException("It's already destroy.");
		}
	}
	
	/**
	 * オブジェクト破棄.
	 */
	public void destroy() {
		buffer = null;
		position = 0;
	}
	
	/**
	 * クローズ処理.
	 * 
	 * ※ この処理ではクローズ処理は行いません.
	 * @exception IOException I/O例外.
	 */
	@Override
	public void close() throws IOException {
		// 何もしない.
	}
	
	/**
	 * 情報クリア.
	 * @return ReBinaryBufferOutputStream このオブジェクトが返却されます.
	 * @exception IOException I/O例外.
	 */
	public RbbOutputStream clear() throws IOException {
		checkDestroy();
		buffer = new byte[initSize];
		position = 0;
		return this;
	}
	
	// 現在の容量が足らない場合は容量を増やす.
	private void increaseBuffer(int len) throws IOException {
		checkDestroy();
		final int bufLen = buffer.length;
		if(position + len > bufLen) {
			byte[] b = new byte[position + len +
				(int)((double)bufLen * bufferAppendCoefficient)];
			System.arraycopy(buffer, 0, b, 0, bufLen);
			buffer = b;
		}
	}

	@Override
	public void write(int b) throws IOException {
		increaseBuffer(1);
		buffer[position ++] = (byte)(b & 0x000000ff);
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		increaseBuffer(len);
		System.arraycopy(b, off, buffer, position, len);
		position += len;
	}
	
	/**
	 * 再利用処理.
	 * 現在までに書き込まれたバッファ位置をクリアして
	 * 再利用します.
	 * @return ReBinaryBufferOutputStream このオブジェクトが返却されます.
	 * @exception IOException I/O例外.
	 */
	public RbbOutputStream reset() throws IOException {
		checkDestroy();
		// 現在書き込みデータが存在する場合は、現状バイナリ長が
		// 大きすぎる場合は縮小する.
		if(position > 0) {
			// 書き込み条件と回数を登録.
			resetCount ++;
			allLength += position;
			
			// 平均値を取得.
			final int avgLength = (int)(allLength / resetCount);
			
			// 今回のバイナリ長を取得.
			final int bufLen = buffer.length;
			
			// 平均長とバイナリ長を比較してバイナリ長が平均値より2.5倍大きい場合.
			if(bufLen > avgLength && bufLen > avgLength * 2.5) {
				// バイナリサイズを平均値にして再作成する.
				byte[] b = new byte[avgLength];
				buffer = b;
			}
		}
		position = 0;
		return this;
	}
	
	/**
	 * 現在の書き込み位置にバッファサイズを合わせる.
	 * @return ReBinaryBufferOutputStream このオブジェクトが返却されます.
	 * @throws IOException I/O例外.
	 */
	public RbbOutputStream smart() throws IOException {
		checkDestroy();
		int len = position;
		if(len < MIN_BUFFER) {
			len =MIN_BUFFER;
		}
		if(len != buffer.length) {
			byte[] b = new byte[len];
			System.arraycopy(buffer, 0, b, 0, position);
			buffer = b;
		}
		return this;
	}
	
	/**
	 * 現在書き込みされている有効なデータ長を取得します.
	 * @return int 有効なデータ長が返却されます.
	 * @exception IOException I/O例外.
	 */
	public int getLength() throws IOException {
		checkDestroy();
		return position;
	}
	
	/**
	 * 書き込み先バッファ情報を取得.
	 * @return byte[] バッファ情報を取得します.
	 * @exception IOException I/O例外.
	 */
	public byte[] getRawBuffer() throws IOException {
		checkDestroy();
		return buffer;
	}
	
	/**
	 * 現在書き込みされている有効なデータ長を取得します.
	 * @return int 有効なデータ長が返却されます.
	 * @exception IOException I/O例外.
	 */
	public int getRawLength() throws IOException {
		checkDestroy();
		return buffer == null ? 0 : buffer.length;
	}
}
