package rim.util;

/**
 * アルファベット半角全角チェック処理.
 */
public class Alphabet {

	/** char文字のチェックを行う配列. **/
	protected static final byte[] CHECK_CHAR = new byte[65536];
	static {
		// スペース系は１.
		// ドットは２.
		// 数字の終端文字は３.
		CHECK_CHAR[' '] = 1;
		CHECK_CHAR['\t'] = 1;
		CHECK_CHAR['\r'] = 1;
		CHECK_CHAR['\n'] = 1;
		CHECK_CHAR['.'] = 2;
		CHECK_CHAR['L'] = 3;
		CHECK_CHAR['l'] = 3;
		CHECK_CHAR['F'] = 3;
		CHECK_CHAR['f'] = 3;
		CHECK_CHAR['D'] = 3;
		CHECK_CHAR['d'] = 3;
	};

	/** アルファベットの半角全角変換値. **/
	protected static final char[] CHECK_ALPHABET = new char[65536];
	static {
		int len = CHECK_ALPHABET.length;
		for (int i = 0; i < len; i++) {
			CHECK_ALPHABET[i] = (char) i;
		}
		int code = (int) 'a';
		int alpha = (int) ('z' - 'a') + 1;
		for (int i = 0; i < alpha; i++) {
			CHECK_ALPHABET[i + code] = (char) (code + i);
		}
		int target = (int) 'A';
		for (int i = 0; i < alpha; i++) {
			CHECK_ALPHABET[i + target] = (char) (code + i);
		}
	}

	/**
	 * アルファベットの半角全角変換値を取得します.
	 * @return
	 */
	public static final char[] getCheckAlphabet() {
		return CHECK_ALPHABET;
	}

	/**
	 * 英字の大文字小文字を区別せずにチェック.
	 *
	 * @param src
	 *            比較元文字を設定します.
	 * @param dest
	 *            比較先文字を設定します.
	 * @return boolean [true]の場合、一致します.
	 */
	public static final boolean eq(String src, String dest) {
		if (src == null || dest == null) {
			return false;
		}
		int len = src.length();
		if (len == dest.length()) {
			for (int i = 0; i < len; i++) {
				if (CHECK_ALPHABET[src.charAt(i)] != CHECK_ALPHABET[dest.charAt(i)]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 英字の大文字小文字を区別せずにチェック.
	 *
	 * @param src
	 *            比較元文字を設定します.
	 * @param off
	 *            srcのオフセット値を設定します.
	 * @param len
	 *            srcのlength値を設定します.
	 * @param dest
	 *            比較先文字を設定します.
	 * @return boolean [true]の場合、一致します.
	 */
	public static final boolean eq(String src, int off, int len, String dest) {
		if (src == null || dest == null || len != dest.length()) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (CHECK_ALPHABET[src.charAt(i + off)] != CHECK_ALPHABET[dest.charAt(i)]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 英字の大文字小文字を区別せずにチェック.
	 *
	 * @param src
	 *            比較元文字を設定します.
	 * @param dests
	 *            比較先文字を設定します.
	 * @return int [-1]の場合不一致です.
	 */
	public static final int eqArray(String src, String... dests) {
		int len;
		if (src == null || dests == null || (len = dests.length) == 0) {
			return -1;
		}
		int j;
		String n;
		boolean eq;
		int lenJ = src.length();
		for(int i = 0; i < len; i ++) {
			if (lenJ == (n = dests[i]).length()) {
				eq = true;
				for (j = 0; j < lenJ; j++) {
					if (CHECK_ALPHABET[src.charAt(j)] != CHECK_ALPHABET[n.charAt(j)]) {
						eq = false;
						break;
					}
				}
				if(eq) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * 英字の大文字小文字を区別しない、バイトチェック.
	 *
	 * @param s
	 *            比較の文字を設定します.
	 * @param d
	 *            比較の文字を設定します.
	 * @return boolean [true]の場合、一致します.
	 */
	public static final boolean oneEq(char s, char d) {
		return CHECK_ALPHABET[s] == CHECK_ALPHABET[d];
	}

	/**
	 * 英字の大文字小文字を区別しない、文字indexOf.
	 *
	 * @param buf
	 *            設定対象の文字情報を設定します.
	 * @param chk
	 *            チェック対象の文字情報を設定します.
	 * @param off
	 *            設定対象のオフセット値を設定します.
	 * @return int マッチする位置が返却されます. [-1]の場合は情報は存在しません.
	 */
	public static final int indexOf(final String buf, final String chk) {
		return indexOf(buf, chk, 0);
	}

	/**
	 * 英字の大文字小文字を区別しない、文字indexOf.
	 *
	 * @param buf
	 *            設定対象の文字情報を設定します.
	 * @param chk
	 *            チェック対象の文字情報を設定します.
	 * @param off
	 *            設定対象のオフセット値を設定します.
	 * @return int マッチする位置が返却されます. [-1]の場合は情報は存在しません.
	 */
	public static final int indexOf(final String buf, final String chk, final int off) {
		final int len = chk.length();
		// 単数文字検索.
		if (len == 1) {
			final int vLen = buf.length();
			if(vLen > off) {
				int i = off;
				final char first = chk.charAt(0);
				if (!oneEq(first, buf.charAt(i))) {
					while (++i < vLen && !oneEq(first, buf.charAt(i)))
						;
					if (vLen != i) {
						return i;
					}
				} else {
					return i;
				}
			}
		}
		// 複数文字検索.
		else {
			int j, k, next;
			final char first = chk.charAt(0);
			final int vLen = buf.length() - (len - 1);
			for (int i = off; i < vLen; i++) {
				if (!oneEq(first, buf.charAt(i))) {
					while (++i < vLen && !oneEq(first, buf.charAt(i)))
						;
				}
				if (i < vLen) {
					for (next = i + len, j = i + 1, k = 1; j < next && oneEq(buf.charAt(j), chk.charAt(k)); j++, k++)
						;
					if (j == next) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * Hashコード生成.
	 *
	 * @param n
	 *            対象の文字列を設定します.
	 * @return int ハッシュコードが返却されます.
	 */
	public static final int hash(String n) {
		int ret = 0;
		final int len = n.length();
		for (int i = 0; i < len; i++) {
			ret = 31 * ret + (int) (CHECK_ALPHABET[n.charAt(i)]);
		}
		return ret;
	}

	/**
	 * 小文字変換.
	 *
	 * @param s
	 *            対象の文字列を設定します.
	 * @return String 変換された情報が返却されます.
	 */
	public static final String toLowerCase(String s) {
		char[] c = s.toCharArray();
		int len = c.length;
		for (int i = 0; i < len; i++) {
			c[i] = CHECK_ALPHABET[c[i]];
		}
		return new String(c);
	}

	/**
	 * 比較処理.
	 *
	 * @param s
	 *            比較の文字を設定します.
	 * @param d
	 *            比較の文字を設定します.
	 * @return int 数字が返却されます. [マイナス]の場合、sのほうが小さい. [プラス]の場合は、sのほうが大きい.
	 *         [0]の場合は、sとdは同一.
	 */
	public static final int compareTo(String s, String d) {
		if (s == d) {
			return 0;
		} else if (s == null) {
			return -1;
		} else if (d == null) {
			return 1;
		}
		int n, len, sLen, dLen;
		sLen = s.length();
		dLen = d.length();
		len = (sLen > dLen) ? dLen : sLen;
		for (int i = 0; i < len; i++) {
			if ((n = CHECK_ALPHABET[s.charAt(i)] - CHECK_ALPHABET[d.charAt(i)]) > 0) {
				return 1;
			} else if (n < 0) {
				return -1;
			}
		}
		if (sLen > dLen) {
			return 1;
		} else if (sLen < dLen) {
			return -1;
		}
		return 0;
	}
}
