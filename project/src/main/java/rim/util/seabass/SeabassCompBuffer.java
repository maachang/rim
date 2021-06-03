package rim.util.seabass;

/**
 * SeabassCompBuffer.
 */
public class SeabassCompBuffer {
	private byte[] data;
	private int limit;

	/**
	 * コンストラクタ.
	 */
	public SeabassCompBuffer() {
	}

	/**
	 * コンストラクタ.
	 *
	 * @param capacity
	 */
	public SeabassCompBuffer(int capacity) {
		data = new byte[capacity];
	}
	
	/**
	 * サイズを指定してバッファをクリア.
	 *
	 * @param length
	 *            再利用時のバイナリ長を設定します.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompBuffer clear(int length) {
		if (data == null || data.length < length) {
			data = new byte[length];
		}
		return this;
	}
	
	/**
	 * 圧縮元のデータ数を指定してバッファをクリア.
	 * 
	 * @param length 圧縮元のデータ数を設定します.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompBuffer clearByMaxCompress(int length) {
		return clear(SeabassComp.calcMaxCompressLength(length));
	}
	
	/**
	 * 現在のバッファ長に合わせてバイナリを合わせる.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompBuffer smart() {
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
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompBuffer setLimit(int limit) {
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
