package rim.util;

/**
 * UTF8I/O.
 * 
 * new String(binary, "UTF8") や "xxxx".getBytes("UTF8") を利用しない
 * メモリの使いまわしを考慮したUTF8変換処理です.
 */
public class UTF8IO {
	private UTF8IO() {
	}
	
	/**
	 * UTF8文字列のバイナリ変換長を取得.
	 * @param value 対象の文字列を設定します.
	 * @return int バイナリ変換される文字列の長さを設定します.
	 */
	public static final int length(final String value) {
		if(value == null || value.isEmpty()) {
			return 0;
		}
		return length(value, 0, value.length());
	}

	/**
	 * UTF8文字列のバイナリ変換長を取得.
	 * @param value 対象の文字列を設定します.
	 * @param off 文字列のオフセット値を設定します.
	 * @param len 文字列の長さを設定します.
	 * @return int バイナリ変換される文字列の長さを設定します.
	 */
	public static final int length(final String value, final int off,
		final int len) {
		if(value == null || value.length() == 0) {
			return 0;
		}
		int c;
		int ret = 0;
		for (int i = 0; i < len; i++) {
			c = (int) value.charAt(off + i);
			// サロゲートペア処理.
			if (c >= 0xd800 && c <= 0xdbff) {
				c = 0x10000 + (((c - 0xd800) << 10) |
					((int) value.charAt(off + i + 1) - 0xdc00));
				i ++;
			}
			if ((c & 0xffffff80) == 0) {
				ret += 1;
			} else if (c < 0x800) {
				ret += 2;
			} else if (c < 0x10000) {
				ret += 3;
			} else {
				ret += 4;
			}
		}
		return ret;
	}
	
	/**
	 * UTF8文字列変換.
	 * @param out 出力先のバイナリを設定します.
	 *            バイナリの長さは変換する文字列の４倍の値を設定
	 *            する必要があります.
	 * @param value 対象の文字列を設定します.
	 * @return int 変換された文字列のバイナリ長が返却されます.
	 */
	public static final int encode(final byte[] out, final String value) {
		if(value == null || value.isEmpty()) {
			return 0;
		}
		return encode(out, 0, value, 0, value.length());
	}
	
	/**
	 * UTF8文字列変換.
	 * @param out 出力先のバイナリを設定します.
	 *            バイナリの長さは変換する文字列の４倍の値を設定
	 *            する必要があります.
	 * @param value 対象の文字列を設定します.
	 * @param off 文字列のオフセット値を設定します.
	 * @param len 文字列の長さを設定します.
	 * @return int 変換された文字列のバイナリ長が返却されます.
	 */
	public static final int encode(final byte[] out, final String value,
		final int off, final int len) {
		return encode(out, 0, value, off, len);
	}
	
	/**
	 * UTF8文字列変換.
	 * @param out 変換結果のbyteが格納されるbyte配列を設定します.
	 *            バイナリの長さは変換する文字列の４倍の値を設定
	 *            する必要があります.
	 * @param boff outの設定開始位置を設定します.
	 * @param value 対象の文字列を設定します.
	 * @param off 文字列のオフセット値を設定します.
	 * @param len 文字列の長さを設定します.
	 * @return int 変換されたbyte長が返却されます.
	 */
	public static final int encode(final byte[] out, final int boff,
		final String value, final int off, final int len) {
		if (value == null || len == 0) {
			return 0;
		}
		int c;
		int cnt = boff;
		for (int i = 0; i < len; i++) {
			c = (int) value.charAt(off + i) & 0x0000ffff;

			// サロゲートペア処理.
			if (c >= 0x0d800 && c <= 0x0dbff) {
				c = 0x010000 + (((c - 0x0d800) << 10) |
					((int) value.charAt(off + i + 1) - 0x0dc00));
				i ++;
			}

			if ((c & 0xffffff80) == 0) {
				out[cnt++] = (byte) c;
			} else if (c < 0x0800) {
				out[cnt++] = (byte) ((c >> 6) | 0x0c0);
				out[cnt++] = (byte) ((c & 0x03f) | 0x080);
			} else if (c < 0x010000) {
				out[cnt++] = (byte) ((c >> 12) | 0x0e0);
				out[cnt++] = (byte) (((c >> 6) & 0x03f) | 0x080);
				out[cnt++] = (byte) ((c & 0x03f) | 0x080);
			} else {
				out[cnt++] = (byte) ((c >> 18) | 0x0f0);
				out[cnt++] = (byte) (((c >> 12) & 0x03f) | 0x080);
				out[cnt++] = (byte) (((c >> 6) & 0x03f) | 0x080);
				out[cnt++] = (byte) ((c & 0x03f) | 0x080);
			}
		}
		return cnt - boff;
	}

	/**
	 * UTF8文字列取得.
	 * 
	 * @param b 対象のバイナリを設定します.
	 * @param off 対象のバイナリ位置を設定します.
	 * @param len バイナリの長さを設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String decode(final byte[] b) {
		return decode(b, 0, b.length);
	}
	
	/**
	 * UTF8文字列取得.
	 * 
	 * @param b 対象のバイナリを設定します.
	 * @param off 対象のバイナリ位置を設定します.
	 * @param len バイナリの長さを設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String decode(final byte[] b, final int off, final int len) {
		final char[] out = new char[len];
		final int dLen = decode(out, 0, b, off, len);
		if(dLen == 0) {
			return "";
		}
		return new String(out, 0, dLen);
	}

	/**
	 * UTF8文字列取得.
	 * 
	 * @param out 変換結果のcharが格納されるchar配列を設定します.
	 *            この長さは変換元のバイナリ長と同じ長さにする必要があります.
	 * @param b 対象のバイナリを設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String decode(final char[] out, final byte[] b) {
		return decode(out, b, 0, b.length);
	}
	
	/**
	 * UTF8文字列取得.
	 * 
	 * @param out 変換結果のcharが格納されるchar配列を設定します.
	 *            この長さは変換元のバイナリ長と同じ長さにする必要があります.
	 * @param b 対象のバイナリを設定します.
	 * @param off 対象のバイナリ位置を設定します.
	 * @param len バイナリの長さを設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String decode(final char[] out, final byte[] b,
		final int off, final int len) {
		final int dLen = decode(out, 0, b, off, len);
		if(dLen == 0) {
			return "";
		}
		return new String(out, 0, dLen);
	}

	
	/**
	 * UTF8文字列取得.
	 * 
	 * @param out 変換結果のcharが格納されるchar配列を設定します.
	 *            この長さは変換元のバイナリ長と同じ長さにする必要があります.
	 * @param coff outの設定開始位置を設定します.
	 * @param b 対象のバイナリを設定します.
	 * @param off 対象のバイナリ位置を設定します.
	 * @param len バイナリの長さを設定します.
	 * @return int 変換されたchar長が返却されます.
	 */
	public static final int decode(final char[] out, final int coff,
		final byte[] b, final int off, final int len) {
		if (len == 0) {
			return 0;
		}
		int c, n;
		int cnt = coff;
		int p = off;
		for (int i = 0; i < len; i++) {
			if (((c = (int) (b[p] & 0x000000ff)) & 0x080) == 0) {
				n = (int) (c & 0x0ff);
				p += 1;
			} else if ((c >> 5) == 0x06) {
				n = (int) (((c & 0x01f) << 6) | (b[p + 1] & 0x03f));
				p += 2;
				i += 1;
			} else if ((c >> 4) == 0x0e) {
				n = (int) (((c & 0x0f) << 12)
					| (((b[p + 1]) & 0x03f) << 6) | ((b[p + 2]) & 0x03f));
				p += 3;
				i += 2;
			} else {
				n = (int) (((c & 0x07) << 18)
					| (((b[p + 1]) & 0x03f) << 12)
					| (((b[p + 2]) & 0x03f) << 6) | ((b[p + 3]) & 0x03f));
				p += 4;
				i += 3;
			}

			// サロゲートペア.
			if ((n & 0xffff0000) != 0) {
				n -= 0x010000;
				out[cnt ++] = (char) (0x0d800 | (n >> 10));
				out[cnt ++] = (char) (0x0dc00 | (n & 0x03ff));
			} else {
				out[cnt++] = (char) n;
			}
		}
		return cnt - coff;
	}
}
