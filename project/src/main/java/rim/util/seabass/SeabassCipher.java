package rim.util.seabass;

import rim.util.Xor128;

/**
 * SeabassCipher 暗号 & 複合処理.
 */
public class SeabassCipher {
	// カスタムbase64.
	private static final class CBase64 {
		private static final char EQ = '=';
		private static final String ENC_CD = "0123456789+abcdefghijklmnopqrstuvwxyz/ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		private static final int[] DEC_CD;
		static {
			final int len = ENC_CD.length();
			int[] dest = new int[256];
			for (int i = 0; i < len; i++) {
				dest[ENC_CD.charAt(i)] = i;
			}
			DEC_CD = dest;
		}

		public static final String encode(final byte[] bin) {
			int i, j;
			final int allLen = bin.length;
			final int etc = (allLen % 3);
			final int len = (allLen / 3);
			final StringBuilder ret = new StringBuilder();
			for (i = 0, j = 0; i < len; i++, j += 3) {
				ret.append(ENC_CD.charAt(((bin[j] & 0x000000fc) >> 2)))
						.append(ENC_CD.charAt((((bin[j] & 0x00000003) << 4) | ((bin[j + 1] & 0x000000f0) >> 4))))
						.append(ENC_CD.charAt((((bin[j + 1] & 0x0000000f) << 2) | ((bin[j + 2] & 0x000000c0) >> 6))))
						.append(ENC_CD.charAt((bin[j + 2] & 0x0000003f)));
			}
			switch (etc) {
			case 1:
				j = len * 3;
				ret.append(ENC_CD.charAt(((bin[j] & 0x000000fc) >> 2)))
						.append(ENC_CD.charAt(((bin[j] & 0x00000003) << 4))).append(EQ).append(EQ);
				break;
			case 2:
				j = len * 3;
				ret.append(ENC_CD.charAt(((bin[j] & 0x000000fc) >> 2)))
						.append(ENC_CD.charAt((((bin[j] & 0x00000003) << 4) | ((bin[j + 1] & 0x000000f0) >> 4))))
						.append(ENC_CD.charAt((((bin[j + 1] & 0x0000000f) << 2)))).append(EQ);
				break;
			}
			return ret.toString();
		}

		public static final byte[] decode(final String base64) {
			int i, j, k;
			int etc = 0;
			final int allLen = base64.length();
			for (i = allLen - 1; i >= 0; i--) {
				if (base64.charAt(i) == EQ) {
					etc++;
				} else {
					break;
				}
			}
			int len = (allLen >> 2);
			final byte[] ret = new byte[(len * 3) - etc];
			len -= 1;
			for (i = 0, j = 0, k = 0; i < len; i++, j += 4, k += 3) {
				ret[k] = (byte) (((DEC_CD[base64.charAt(j)] & 0x0000003f) << 2)
						| ((DEC_CD[base64.charAt(j + 1)] & 0x00000030) >> 4));
				ret[k + 1] = (byte) (((DEC_CD[base64.charAt(j + 1)] & 0x0000000f) << 4)
						| ((DEC_CD[base64.charAt(j + 2)] & 0x0000003c) >> 2));
				ret[k + 2] = (byte) (((DEC_CD[base64.charAt(j + 2)] & 0x00000003) << 6)
						| (DEC_CD[base64.charAt(j + 3)] & 0x0000003f));
			}
			switch (etc) {
			case 0:
				j = len * 4;
				k = len * 3;
				ret[k] = (byte) (((DEC_CD[base64.charAt(j)] & 0x0000003f) << 2)
						| ((DEC_CD[base64.charAt(j + 1)] & 0x00000030) >> 4));
				ret[k + 1] = (byte) (((DEC_CD[base64.charAt(j + 1)] & 0x0000000f) << 4)
						| ((DEC_CD[base64.charAt(j + 2)] & 0x0000003c) >> 2));
				ret[k + 2] = (byte) (((DEC_CD[base64.charAt(j + 2)] & 0x00000003) << 6)
						| (DEC_CD[base64.charAt(j + 3)] & 0x0000003f));
				break;
			case 1:
				j = len * 4;
				k = len * 3;
				ret[k] = (byte) (((DEC_CD[base64.charAt(j)] & 0x0000003f) << 2)
						| ((DEC_CD[base64.charAt(j + 1)] & 0x00000030) >> 4));
				ret[k + 1] = (byte) (((DEC_CD[base64.charAt(j + 1)] & 0x0000000f) << 4)
						| ((DEC_CD[base64.charAt(j + 2)] & 0x0000003c) >> 2));
				break;
			case 2:
				j = len * 4;
				k = len * 3;
				ret[k] = (byte) (((DEC_CD[base64.charAt(j)] & 0x0000003f) << 2)
						| ((DEC_CD[base64.charAt(j + 1)] & 0x00000030) >> 4));
				break;
			}
			return ret;
		}
	}

	// コードフリップ.
	private static final int _flip(int pause, int step) {
		switch (step & 0x00000007) {
		case 1:
			return ((((pause & 0x00000003) << 6) & 0x000000c0) | (((pause & 0x000000fc) >> 2) & 0x0000003f))
					& 0x000000ff;
		case 2:
			return ((((pause & 0x0000003f) << 2) & 0x000000fc) | (((pause & 0x000000c0) >> 6) & 0x00000003))
					& 0x000000ff;
		case 3:
			return ((((pause & 0x00000001) << 7) & 0x00000080) | (((pause & 0x000000fe) >> 1) & 0x0000007f))
					& 0x000000ff;
		case 4:
			return ((((pause & 0x0000000f) << 4) & 0x000000f0) | (((pause & 0x000000f0) >> 4) & 0x0000000f))
					& 0x000000ff;
		case 5:
			return ((((pause & 0x0000007f) << 1) & 0x000000fe) | (((pause & 0x00000080) >> 7) & 0x00000001))
					& 0x000000ff;
		case 6:
			return ((((pause & 0x00000007) << 5) & 0x000000e0) | (((pause & 0x000000f8) >> 3) & 0x0000001f))
					& 0x000000ff;
		case 7:
			return ((((pause & 0x0000001f) << 3) & 0x000000f8) | (((pause & 0x000000e0) >> 5) & 0x00000007))
					& 0x000000ff;
		}
		return pause & 0x000000ff;
	}

	// コードnフリップ.
	private static final int _nflip(int pause, int step) {
		switch (step & 0x00000007) {
		case 1:
			return ((((pause & 0x0000003f) << 2) & 0x000000fc) | (((pause & 0x000000c0) >> 6) & 0x00000003))
					& 0x000000ff;
		case 2:
			return ((((pause & 0x00000003) << 6) & 0x000000c0) | (((pause & 0x000000fc) >> 2) & 0x0000003f))
					& 0x000000ff;
		case 3:
			return ((((pause & 0x0000007f) << 1) & 0x000000fe) | (((pause & 0x00000080) >> 7) & 0x00000001))
					& 0x000000ff;
		case 4:
			return ((((pause & 0x0000000f) << 4) & 0x000000f0) | (((pause & 0x000000f0) >> 4) & 0x0000000f))
					& 0x000000ff;
		case 5:
			return ((((pause & 0x00000001) << 7) & 0x00000080) | (((pause & 0x000000fe) >> 1) & 0x0000007f))
					& 0x000000ff;
		case 6:
			return ((((pause & 0x0000001f) << 3) & 0x000000f8) | (((pause & 0x000000e0) >> 5) & 0x00000007))
					& 0x000000ff;
		case 7:
			return ((((pause & 0x00000007) << 5) & 0x000000e0) | (((pause & 0x000000f8) >> 3) & 0x0000001f))
					& 0x000000ff;
		}
		return pause & 0x000000ff;
	}

	// ハッシュ計算.
	private static final int[] hash_raw(final byte[] bin) {
		int o;
		int[] n = new int[] { 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xCA62C1D6 };
		final int len = bin.length;
		for (int i = 0; i < len; i++) {
			if (((o = (bin[i] & 0x000000ff)) & 1) == 1) {
				o = _flip(o, o);
			} else {
				o = _nflip(o, o);
			}
			if ((i & 1) == 1) {
				n[0] = n[0] + o;
				n[1] = n[1] - (o << 8);
				n[2] = n[2] + (o << 16);
				n[3] = n[3] - (o << 24);
				n[3] = n[3] ^ (o);
				n[2] = n[2] ^ (o << 8);
				n[1] = n[1] ^ (o << 16);
				n[0] = n[0] ^ (o << 24);
				n[0] = (n[3] + 1) + (n[0]);
				n[1] = (n[2] - 1) + (n[1]);
				n[2] = (n[1] + 1) + (n[2]);
				n[3] = (n[0] - 1) + (n[3]);
			} else {
				n[3] = n[3] + o;
				n[2] = n[2] - (o << 8);
				n[1] = n[1] + (o << 16);
				n[0] = n[0] - (o << 24);
				n[0] = n[0] ^ (o);
				n[1] = n[1] ^ (o << 8);
				n[2] = n[2] ^ (o << 16);
				n[3] = n[3] ^ (o << 24);
				n[0] = (n[3] + 1) - (n[0]);
				n[1] = (n[2] - 1) - (n[1]);
				n[2] = (n[1] + 1) - (n[2]);
				n[3] = (n[0] - 1) - (n[3]);
			}
			n[3] = (n[0] + 1) ^ (~n[3]);
			n[2] = (n[1] - 1) ^ (~n[2]);
			n[1] = (n[2] + 1) ^ (~n[1]);
			n[0] = (n[3] - 1) ^ (~n[0]);
		}
		return n;
	}

	/**
	 * cb64でバイナリ変換.
	 * @param b バイナリを設定します.
	 * @return String cb64が返却されます.
	 */
	public static final String cb64_enc(byte[] b) {
		return CBase64.encode(b);
	}

	/**
	 * cb64の文字列を復元.
	 * @param s cb64で変換された文字列を設定します.
	 * @return byte[] バイナリが返却されます.
	 */
	public static final byte[] cb64_dec(String s) {
		return CBase64.decode(s);
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param bin
	 *            バイナリを設定します.
	 * @return byte[] 16byteのハッシュ内容が返却されます.
	 */
	public static final byte[] hash(final byte[] bin) {
		final int[] n = hash_raw(bin);

		// バイナリで返却.
		return new byte[] {
			(byte) (n[0] & 0x000000ff),
			(byte) ((n[0] & 0x0000ff00) >> 8),
			(byte) ((n[0] & 0x00ff0000) >> 16),
			(byte) (((n[0] & 0xff000000) >> 24) & 0x00ff),
			(byte) (n[1] & 0x000000ff),
			(byte) ((n[1] & 0x0000ff00) >> 8),
			(byte) ((n[1] & 0x00ff0000) >> 16),
			(byte) (((n[1] & 0xff000000) >> 24) & 0x00ff),
			(byte) (n[2] & 0x000000ff),
			(byte) ((n[2] & 0x0000ff00) >> 8),
			(byte) ((n[2] & 0x00ff0000) >> 16),
			(byte) (((n[2] & 0xff000000) >> 24) & 0x00ff),
			(byte) (n[3] & 0x000000ff),
			(byte) ((n[3] & 0x0000ff00) >> 8),
			(byte) ((n[3] & 0x00ff0000) >> 16),
			(byte) (((n[3] & 0xff000000) >> 24) & 0x00ff) };
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param s
	 *            文字列を設定します.
	 * @return byte[] 16byteのハッシュ内容が返却されます.
	 */
	public static final byte[] hash(String s) {
		try {
			return hash(s.getBytes("UTF8"));
		} catch (Exception e) {
			throw new SeabassCipherException(e);
		}
	}

	// 右端文字のイコールを削除.
	private static final String cutEndEq(final String s) {
		for (int i = s.length() - 1; i >= 0; i--) {
			if (s.charAt(i) != '=') {
				return s.substring(0, i + 1);
			}
		}
		return "";
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param s
	 *            文字列を設定します.
	 * @return String ハッシュ内容が返却されます.
	 */
	public static final String hash_s(String s) {
		return cutEndEq(CBase64.encode(hash(s)));
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param bin
	 *            バイナリを設定します.
	 * @return String ハッシュ内容が返却されます.
	 */
	public static final String hash_s(final byte[] bin) {
		return cutEndEq(CBase64.encode(hash(bin)));
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param s
	 *            文字列を設定します.
	 * @return String ハッシュ内容が返却されます.
	 */
	public static final String hashUuid(String s) {
		try {
			return byte16ToUUID(hash_raw(s.getBytes("UTF8")));
		} catch (Exception e) {
			throw new SeabassCipherException(e);
		}
	}

	/**
	 * ハッシュ計算.
	 *
	 * @param bin
	 *            バイナリを設定します.
	 * @return String ハッシュ内容が返却されます.
	 */
	public static final String hashUuid(final byte[] bin) {
		return byte16ToUUID(hash_raw(bin));
	}

	// fcipherヘッダ文字列.
	private String _head = null;
	// ランダムオブジェクト.
	private Xor128 _rand = null;

	/**
	 * コンストラクタ.
	 */
	public SeabassCipher() {
		this(null, null);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param rand
	 */
	public SeabassCipher(Xor128 rand) {
		this(rand, null);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param rand
	 * @param h
	 */
	public SeabassCipher(Xor128 rand, String h) {
		this._rand = (rand == null) ? new Xor128(System.nanoTime()) : rand;
		this._head = h;
	}

	/**
	 * ヘッダデータをセット.
	 *
	 * @param h
	 */
	public SeabassCipher setHead(String h) {
		this._head = h;
		return this;
	}

	/**
	 * ヘッダデータを取得.
	 *
	 * @return
	 */
	public String getHead() {
		return this._head;
	}

	/**
	 * ランダムオブジェクトを設定.
	 *
	 * @param rand
	 * @return
	 */
	public SeabassCipher setRand(Xor128 rand) {
		this._rand = (rand == null) ? new Xor128(System.nanoTime()) : rand;
		return this;
	}

	/**
	 * ランダムオブジェクトを取得.
	 *
	 * @return
	 */
	public Xor128 getRand() {
		return this._rand;
	}

	/**
	 * 暗号化・復号化のpKeyを生成.
	 *
	 * @param word パスフレーズを設定.
	 * @return
	 */
	public byte[] pKey(String word) {
		return pKey(word, null);
	}

	/**
	 * 暗号化・復号化のpKeyを生成.
	 *
	 * @param word パスフレーズを設定.
	 * @param src 拡張条件を設定.
	 * @return
	 */
	public byte[] pKey(String word, String src) {
		if (src == null || src.isEmpty()) {
			src = "hello@seabass.com";
		}
		byte[] srcBin = code16(src);
		byte[] wordBin = code16(word);
		byte[] ret = new byte[32];
		System.arraycopy(srcBin, 0, ret, 0, 16);
		System.arraycopy(wordBin, 0, ret, 16, 16);
		for (int i = 0; i < 16; i++) {
			ret[i] = _convert(ret, i, wordBin[i]);
		}
		for (int i = 15, j = 0; i >= 0; i--, j++) {
			ret[i + 16] = _convert(ret, i + 16, srcBin[j]);
		}
		return ret;
	}

	/**
	 * エンコード.
	 *
	 * @param value
	 * @param pKey
	 * @return
	 */
	public String enc(String value, byte[] pKey) {
		return enc(value, pKey, null);
	}

	/**
	 * エンコード.
	 *
	 * @param value
	 * @param pKey
	 * @param head
	 * @return
	 */
	public String enc(String value, byte[] pKey, String head) {
		byte[] b;
		try {
			b = value.getBytes("UTF8");
		} catch (Exception e) {
			throw new SeabassCipherException(e);
		}
		return benc(b, pKey, head);
	}

	/**
	 * バイナリエンコード.
	 *
	 * @param bin
	 * @param pKey
	 * @param head
	 * @return
	 */
	public String benc(byte[] bin, byte[] pKey) {
		return benc(bin, pKey, null);
	}

	/**
	 * バイナリエンコード.
	 *
	 * @param bin
	 * @param pKey
	 * @return
	 */
	public byte[] benc_b(byte[] bin, byte[] pKey) {
		byte[] pubKey = _randKey();
		byte[] key256 = _key256(_convertKey(pKey, pubKey));
		int stepNo = _getStepNo(pKey, bin) & 0x0000007f;
		int nowStep = _convert256To(key256, pubKey, stepNo);
		_ed(true, bin, key256, nowStep);
		byte[] eb = new byte[34 + bin.length];
		eb[0] = (byte) (_rand.nextInt() & 0x000000ff);
		eb[1] = (byte) (~(stepNo ^ eb[0]));
		System.arraycopy(pubKey, 0, eb, 2, 32);
		System.arraycopy(bin, 0, eb, 34, bin.length);
		return eb;
	}

	/**
	 * バイナリエンコード.
	 *
	 * @param bin
	 * @param pKey
	 * @param head
	 * @return
	 */
	public String benc(byte[] bin, byte[] pKey, String head) {
		head = head == null ? ((_head == null) ? "" : _head) : head;
		byte[] eb = benc_b(bin, pKey);
		return head + CBase64.encode(eb);
	}

	/**
	 * デコード.
	 *
	 * @param value
	 * @param pKey
	 * @param head
	 * @return
	 */
	public String dec(String value, byte[] pKey) {
		return dec(value, pKey, null);
	}

	/**
	 * デコード.
	 *
	 * @param value
	 * @param pKey
	 * @param head
	 * @return
	 */
	public String dec(String value, byte[] pKey, String head) {
		byte[] b = bdec(value, pKey, head);
		try {
			return new String(b, "UTF8");
		} catch (Exception e) {
			throw new SeabassCipherException(e);
		}
	}

	/**
	 * バイナリデコード.
	 *
	 * @param value
	 * @param pKey
	 * @param head
	 * @return
	 */
	public byte[] bdec(String value, byte[] pKey) {
		return bdec(value, pKey, null);
	}

	/**
	 * バイナリデコード.
	 *
	 * @param value
	 * @param pKey
	 * @param head
	 * @return
	 */
	public byte[] bdec(String value, byte[] pKey, String head) {
		head = head == null ? ((_head == null) ? "" : _head) : head;
		return bdec_b(CBase64.decode(value.substring(head.length())), pKey);
	}

	/**
	 * バイナリデコード.
	 *
	 * @param value
	 * @param pKey
	 * @return
	 */
	public byte[] bdec_b(byte[] value, byte[] pKey) {
		if (value.length <= 34) {
			throw new SeabassCipherException("decode:Invalid binary length.");
		}
		int stepNo = ((~(value[1] ^ value[0])) & 0x0000007f);
		byte[] pubKey = new byte[32];
		System.arraycopy(value, 2, pubKey, 0, 32);
		int bodyLen = value.length - 34;
		byte[] body = new byte[bodyLen];
		System.arraycopy(value, 34, body, 0, bodyLen);
		value = null;
		byte[] key256 = _key256(_convertKey(pKey, pubKey));
		int nowStep = _convert256To(key256, pubKey, stepNo);
		_ed(false, body, key256, nowStep);
		int destStepNo = _getStepNo(pKey, body) & 0x0000007f;
		if (destStepNo != stepNo) {
			throw new SeabassCipherException("decode:Decryption process failed.");
		}
		return body;
	}

	// ランダムキーを生成.
	private byte[] _randKey() {
		byte[] bin = new byte[32];
		for (int i = 0; i < 32; i++) {
			bin[i] = (byte) (_rand.nextInt() & 0x000000ff);
		}
		return bin;
	}

	// コード16データを作成.
	// s 文字コード.
	private static final byte[] code16(String s) {
		int n, i, j;
		byte[] ret = new byte[] { (byte) 177, (byte) 75, (byte) 163,
			(byte) 143, (byte) 73, (byte) 49, (byte) 207,
			(byte) 40, (byte) 87, (byte) 41, (byte) 169,
			(byte) 91, (byte) 184, (byte) 67, (byte) 254, (byte) 89 };
		int len = s.length();
		for (i = 0; i < len; i++) {
			n = s.charAt(i) & 0x0000ffff;
			if ((i & 0x00000001) == 0) {
				for (j = 0; j < 16; j += 2) {
					ret[j] = (byte) (ret[j] ^ (n - (i + j)));
				}
				for (j = 1; j < 16; j += 1) {
					ret[j] = (byte) (ret[j] ^ ~(n - (i + j)));
				}
			} else {
				for (j = 1; j < 16; j += 1) {
					ret[j] = (byte) (ret[j] ^ (n - (i + j)));
				}
				for (j = 0; j < 16; j += 2) {
					ret[j] = (byte) (ret[j] ^ ~(n - (i + j)));
				}
			}
		}
		return hash(ret);
	}

	// コード16データを作成.
	// s バイナリ.
	private static final byte[] code16(byte[] s) {
		int n, i, j;
		byte[] ret = new byte[] { (byte) 87, (byte) 41, (byte) 169,
				(byte) 91, (byte) 184, (byte) 67, (byte) 254,
				(byte) 89, (byte) 177, (byte) 75, (byte) 163,
				(byte) 143, (byte) 73, (byte) 49, (byte) 207, (byte) 40 };
		int len = s.length;
		for (i = 0; i < len; i++) {
			n = s[i] & 0x000000ff;
			if ((i & 0x00000001) == 0) {
				for (j = 0; j < 16; j += 2) {
					ret[j] = (byte) (ret[j] ^ (n - (i + j)));
				}
				for (j = 1; j < 16; j += 1) {
					ret[j] = (byte) (ret[j] ^ ~(n - (i + j)));
				}
			} else {
				for (j = 1; j < 16; j += 1) {
					ret[j] = (byte) (ret[j] ^ (n - (i + j)));
				}
				for (j = 0; j < 16; j += 2) {
					ret[j] = (byte) (ret[j] ^ ~(n - (i + j)));
				}
			}
		}
		return hash(ret);
	}

	/// 変換処理.
	private static final byte _convert(byte[] key, int no, int pause) {
		switch ((no & 0x00000001)) {
		case 0:
			return (byte) (((pause ^ key[no])) & 0x000000ff);
		case 1:
			return (byte) (~(pause ^ key[no]) & 0x000000ff);
		}
		return (byte) 0;
	}

	private final byte[] _convertKey(byte[] pKey, byte[] key) {
		byte[] low = code16(pKey);
		byte[] hight = code16(key);
		byte[] ret = new byte[32];
		for (int i = 0, j = 0, k = 15; i < 16; i++, j += 2, k--) {
			ret[j] = _convert(low, i, key[j]);
			ret[j + 1] = _convert(hight, i, low[k]);
		}
		return ret;
	}

	private static final byte[] _key256(byte[] key32) {
		int s, e;
		int n = 0;
		byte[] ret = new byte[256];
		byte[] b = new byte[4];
		for (int i = 0, j = 0; i < 31; i += 2, j += 16) {
			s = (key32[i] & 0x000000ff);
			e = (key32[i + 1] & 0x000000ff);
			if ((n & 0x00000001) != 0) {
				n += s ^ (~e);
			} else {
				n -= (~s) ^ e;
			}
			b[0] = (byte) (n & 0x000000ff);
			b[1] = (byte) (((n & 0x0000ff00) >> 8) & 0x000000ff);
			b[2] = (byte) (((n & 0x00ff0000) >> 16) & 0x000000ff);
			b[3] = (byte) (((n & 0xff000000) >> 24) & 0x000000ff);
			System.arraycopy(code16(b), 0, ret, j, 16);
		}
		return ret;
	}

	private static final int _getStepNo(byte[] pubKey, byte[] binary) {
		int i, j, bin;
		int ret = 0;
		int len = binary.length;
		int addCd = (pubKey[(binary[len >> 1] & 0x0000001f)] & 0x00000003) + 1;
		for (i = 0, j = 0; i < len; i += addCd, j += addCd) {
			bin = ((~binary[i]) & 0x000000ff);
			ret = ((bin & 0x00000001) + ((bin & 0x00000002) >> 1) + ((bin & 0x00000004) >> 2)
					+ ((bin & 0x00000008) >> 3) + ((bin & 0x00000010) >> 4) + ((bin & 0x00000020) >> 5)
					+ ((bin & 0x00000040) >> 6) + ((bin & 0x00000080) >> 7)) + (j & 0x000000ff) + ret;
		}
		if ((ret & 0x00000001) == 0) {
			for (i = 0; i < 32; i++) {
				bin = (((pubKey[i] & 0x00000001) == 0) ? ((~pubKey[i]) & 0x000000ff) : (pubKey[i] & 0x000000ff));
				ret += ((bin & 0x00000001) + ((bin & 0x00000002) >> 1) + ((bin & 0x00000004) >> 2)
						+ ((bin & 0x00000008) >> 3) + ((bin & 0x00000010) >> 4) + ((bin & 0x00000020) >> 5)
						+ ((bin & 0x00000040) >> 6) + ((bin & 0x00000080) >> 7));
			}
		} else {
			for (i = 0; i < 32; i++) {
				bin = (((pubKey[i] & 0x00000001) == 0) ? ((~pubKey[i]) & 0x000000ff) : (pubKey[i] & 0x000000ff));
				ret -= ((bin & 0x00000001) + ((bin & 0x00000002) >> 1) + ((bin & 0x00000004) >> 2)
						+ ((bin & 0x00000008) >> 3) + ((bin & 0x00000010) >> 4) + ((bin & 0x00000020) >> 5)
						+ ((bin & 0x00000040) >> 6) + ((bin & 0x00000080) >> 7));
			}
		}
		return ((~ret) & 0x000000ff);
	}

	private static final int _convert256To(byte[] key256, byte[] pKey, int step) {
		int ns = step;
		for (int i = 0, j = 0; i < 256; i++, j = ((j + 1) & 0x0000001f)) {
			ns = (ns ^ (~(key256[i])));
			if ((ns & 0x00000001) == 0) {
				ns = ~ns;
			}
			key256[i] = _convert(pKey, j, key256[i]);
			key256[i] = (byte) _flip(key256[i], ns);
		}
		return ns;
	}

	private static final void _ed(boolean mode, byte[] binary, byte[] key256, int step) {
		int len = binary.length;
		int ns = step;
		if (mode) {
			for (int i = 0, j = 0; i < len; i++, j = ((j + 1) & 0x000000ff)) {
				ns = (ns ^ (~(key256[j])));
				if ((ns & 0x00000001) != 0) {
					ns = ~ns;
				}
				binary[i] = _convert(key256, j, binary[i]);
				binary[i] = (byte) _flip(binary[i], ns);
			}
		} else {
			for (int i = 0, j = 0; i < len; i++, j = ((j + 1) & 0x000000ff)) {
				ns = (ns ^ (~(key256[j])));
				if ((ns & 0x00000001) != 0) {
					ns = ~ns;
				}
				binary[i] = (byte) _nflip(binary[i], ns);
				binary[i] = _convert(key256, j, binary[i]);
			}
		}
	}

	// ゼロサプレス.
	private static final void _z2(StringBuilder buf, String no) {
		buf.append("00".substring(no.length())).append(no);
	}

	// 16バイトデータ(4バイト配列４つ)をUUIDに変換.
	private static final String byte16ToUUID(int[] n) {
		return byte16ToUUID(n[0], n[1], n[2], n[3]);
	}

	// 16バイトデータ(4バイト配列４つ)をUUIDに変換.
	private static final String byte16ToUUID(int a, int b, int c, int d) {
		final StringBuilder buf = new StringBuilder();
		_z2(buf, Integer.toHexString(((a & 0xff000000) >> 24) & 0x00ff));
		_z2(buf, Integer.toHexString((a & 0x00ff0000) >> 16));
		_z2(buf, Integer.toHexString((a & 0x0000ff00) >> 8));
		_z2(buf, Integer.toHexString(a & 0x000000ff));
		buf.append("-");
		_z2(buf, Integer.toHexString(((b & 0xff000000) >> 24) & 0x00ff));
		_z2(buf, Integer.toHexString((b & 0x00ff0000) >> 16));
		buf.append("-");
		_z2(buf, Integer.toHexString((b & 0x0000ff00) >> 8));
		_z2(buf, Integer.toHexString(b & 0x000000ff));
		buf.append("-");
		_z2(buf, Integer.toHexString(((c & 0xff000000) >> 24) & 0x00ff));
		_z2(buf, Integer.toHexString((c & 0x00ff0000) >> 16));
		buf.append("-");
		_z2(buf, Integer.toHexString((c & 0x0000ff00) >> 8));
		_z2(buf, Integer.toHexString(c & 0x000000ff));
		_z2(buf, Integer.toHexString(((d & 0xff000000) >> 24) & 0x00ff));
		_z2(buf, Integer.toHexString((d & 0x00ff0000) >> 16));
		_z2(buf, Integer.toHexString((d & 0x0000ff00) >> 8));
		_z2(buf, Integer.toHexString(d & 0x000000ff));
		return buf.toString();
	}
}
