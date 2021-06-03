package rim;

/**
 * Rim-Util.
 */
public class RimUtil {
	private RimUtil() {}

	/**
	 * 長さからその内容に収まるバイト数を取得.
	 * @param len 長さを設定します.
	 * @return 長さの範囲に収まるバイト数が返却されます.
	 */
	public static final int intLengthByByte1_4Length(int len) {
		if(len > 0x00ffffff) {
			return 4;
		} else if(len > 0x0000ffff) {
			return 3;
		} else if(len > 0x000000ff) {
			return 2;
		}
		return 1;
	}
	
	/**
	 * StringをUTF8変換する時に使う一時バッファサイズをサイズ調整します.
	 * @param buf byte[]内容が格納されてるオブジェクト配列を設定します.
	 * @param len 新しいバッファサイズを設定します.
	 * @return byte[] 一時バッファ情報が返却されます.
	 */
	public static final byte[] getStringByteArray(Object[] buf, int len) {
		byte[] b = (byte[])buf[0];
		if(b.length < len) {
			b = new byte[len];
			buf[0] = b;
		}
		return b;
	}
	
	/**
	 * BinaryからUTF8文字列変換する時に使う一時バッファをサイズ調整します.
	 * @param buf char[]内容が格納されてるオブジェクト配列を設定します.
	 * @param len 新しいバッファサイズを設定します.
	 * @return byte[] 一時バッファ情報が返却されます.
	 */
	public static final char[] getStringCharArray(Object[] buf, int len) {
		char[] c = (char[])buf[1];
		if(c.length < len) {
			c = new char[len];
			buf[1] = c;
		}
		return c;
	}

}
