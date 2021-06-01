package rim.util.seabass;

/**
 * SeabassCompBuffer.
 */
public class SeabassCompBuffer {
	private byte[] data;
	private int length;

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
	 * オブジェクト再利用.
	 *
	 * @param length
	 *            再利用時のバイナリ長を設定します.
	 */
	public void clear(int length) {
		if (data == null || data.length < length) {
			data = new byte[length];
		}
	}

	/**
	 * データ配列が足りない場合は、指定長を元に増やす.
	 * 
	 * @param length
	 *            この長さのデーター長が必要な場合の設定を行います.
	 * @return
	 */
	public SeabassCompBuffer increaseIfLarger(int length) {
		if(data == null || data.length < length) {
			byte[] b = new byte[length + (int)((length * 0.5d) + 32)];
			if(data != null) {
				System.arraycopy(data, 0, b, 0, data.length);
			}
			data = b;
		}
		return this;
	}

	/**
	 * データ取得.
	 *
	 * @return
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * 現在のデータ分のバイナリ長を取得.
	 *
	 * @return
	 */
	public int getDataLength() {
		return data == null ? 0 : data.length;
	}

	/**
	 * データ長取得.
	 *
	 * @return
	 */
	public int getLength() {
		return length;
	}

	/**
	 * データ長設定.
	 *
	 * @param length
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * 現在のバイナリデータを取得.
	 *
	 * @return
	 */
	public byte[] toByteArray() {
		byte[] res = new byte[length];
		System.arraycopy(data, 0, res, 0, length);
		return res;
	}
}
