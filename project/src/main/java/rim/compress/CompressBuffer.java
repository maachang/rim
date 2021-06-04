package rim.compress;

/**
 * CompressBuffer.
 */
public class CompressBuffer {
	private byte[] data;
	private int limit;

	/**
	 * コンストラクタ.
	 */
	public CompressBuffer() {
	}

	/**
	 * コンストラクタ.
	 *
	 * @param capacity
	 */
	public CompressBuffer(int capacity) {
		data = new byte[capacity];
	}
	
	/**
	 * サイズを指定してバッファをクリア.
	 *
	 * @param length
	 *            再利用時のバイナリ長を設定します.
	 * @return CompressBuffer このオブジェクトが返却されます.
	 */
	public CompressBuffer clear(int length) {
		if (data == null || data.length < length) {
			data = new byte[length];
		}
		limit = 0;
		return this;
	}
	
	/**
	 * 現在のバッファ長に合わせてバイナリを合わせる.
	 * @return CompressBuffer このオブジェクトが返却されます.
	 */
	public CompressBuffer smart() {
		if(data.length != limit) {
			byte[] b = new byte[limit];
			System.arraycopy(data, 0, b, 0, limit);
			data = b;
		}
		return this;
	}

	/**
	 * 内部で持つ生バッファを取得.
	 *
	 * @return
	 */
	public byte[] getRawBuffer() {
		return data;
	}
	
	/**
	 * 内部で持つ生バッファ長を取得.
	 *
	 * @return
	 */
	public int getRawBufferLength() {
		return data == null ? 0 : data.length;
	}

	/**
	 * データ終端を取得.
	 *
	 * @return
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * データー終端を設定.
	 *
	 * @param length このデーターの終端を設定します.
	 * @return CompressBuffer このオブジェクトが返却されます.
	 */
	public CompressBuffer setLimit(int limit) {
		this.limit = limit;
		return this;
	}

	/**
	 * 現在のバイナリデータを取得.
	 *
	 * @return
	 */
	public byte[] toByteArray() {
		byte[] res = new byte[limit];
		System.arraycopy(data, 0, res, 0, limit);
		return res;
	}
}
