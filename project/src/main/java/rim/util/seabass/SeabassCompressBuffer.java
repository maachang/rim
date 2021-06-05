package rim.util.seabass;

/**
 * SeabassCompBuffer.
 */
public class SeabassCompressBuffer {
	// バッファデーター.
	private byte[] data;
	// リミットサイズ.
	private int limit;
	
	// 総書き込みサイズ.
	private long allLength = 0L;
	// リセット回数.
	private long resetCount = 0L;

	/**
	 * コンストラクタ.
	 */
	public SeabassCompressBuffer() {
	}

	/**
	 * コンストラクタ.
	 *
	 * @param capacity
	 */
	public SeabassCompressBuffer(int capacity) {
		data = new byte[capacity];
	}
	
	/**
	 * サイズを指定してバッファをクリア.
	 *
	 * @param length
	 *            再利用時のバイナリ長を設定します.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompressBuffer clear(int length) {
		// データが存在しないか前回より長い
		if (data == null || data.length < length) {
			data = new byte[length];
			
			// 書き込み条件と回数を登録.
			resetCount ++;
			allLength += length;

		// 範囲内のデーターの場合.
		} else {
			// 現在書き込みデータが存在する場合は、現状バイナリ長が
			// 大きすぎる場合は縮小する.
			
			// 書き込み条件と回数を登録.
			resetCount ++;
			allLength += length;
			
			// 平均値を取得.
			final int avgLength = (int)(allLength / resetCount);
			
			// 今回のバイナリ長を取得.
			final int bufLen = data.length;
			
			// 平均長とバイナリ長を比較してバイナリ長が平均値より2.5倍大きい場合.
			if(bufLen > avgLength && bufLen > avgLength * 2.5) {
				// ただし、今回設定した"length"より小さくなる場合は
				// 処理しない.
				if(length < avgLength) {
					// バイナリサイズを平均値にして再作成する.
					byte[] b = new byte[avgLength];
					data = b;
				}
			}
		}
		limit = 0;
		return this;
	}
	
	/**
	 * 圧縮元のデータ数を指定してバッファをクリア.
	 * 
	 * @param length 圧縮元のデータ数を設定します.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompressBuffer clearByMaxCompress(int length) {
		return clear(SeabassCompress.maxCompressLength(length));
	}
	
	/**
	 * 現在のバッファ長に合わせてバイナリを合わせる.
	 * @return SeabassCompBuffer このオブジェクトが返却されます.
	 */
	public SeabassCompressBuffer smart() {
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
	public SeabassCompressBuffer setLimit(int limit) {
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
