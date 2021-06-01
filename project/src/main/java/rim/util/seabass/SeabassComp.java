package rim.util.seabass;

/**
 * Seabass 簡易圧縮 & 解凍.
 */
public class SeabassComp {

	/** １ブロックでの圧縮サイズ上限. **/
	private static final int LIMIT = 64;

	/** 基本Hashシフト値. **/
	private static final int DEF_SHIFT = 0;

	/** Hash衝突を回避するための係数. **/
	private static final int HASH = 0x1e35a7bd;

	/** 各非圧縮条件に対するヘッダ内容. **/
	private static final byte b60 = (byte) (60 << 2); // 2バイトヘッダ.
	private static final byte b61 = (byte) (61 << 2); // 3バイトヘッダ.
	private static final byte b62 = (byte) (62 << 2); // 4バイトヘッダ.
	private static final byte b63 = (byte) (63 << 2); // 5バイトヘッダ.

	/**
	 * 圧縮バッファサイズの計算
	 *
	 * @param len
	 *            圧縮対象のメモリサイズを設定します.
	 * @return 圧縮バッファでの最大サイズが返却されます.
	 */
	public static final int calcMaxCompressLength(int len) {
		return 32 + len + (len / 6);
	}

	/**
	 * 解凍バッファサイズの取得.
	 *
	 * @param binary
	 *            対象のバイナリを設定します.
	 * @param off
	 *            対象のオフセット値を設定します.
	 * @return int 解凍対象のバイナリサイズが返却されます.
	 */
	public static final int decompressLength(byte[] binary, int off) {
		int ret = 0;
		int i = 0;
		do {
			ret += (binary[off] & 0x7f) << (i++ * 7);
		} while ((binary[off++] & 0x80) == 0x80);
		return ret;
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            data to be compressed
	 * @return compressed data block
	 */
	public static final SeabassCompBuffer compress(byte[] in) {
		return compress(in, 0, in.length, null, DEF_SHIFT);
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            data to be compressed
	 * @param out
	 *            JSnappyBuffer for compressed data block
	 * @return reference to <code>out</code>
	 */
	public static final SeabassCompBuffer compress(byte[] in, SeabassCompBuffer out) {
		return compress(in, 0, in.length, out, DEF_SHIFT);
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            data to be compressed
	 * @param offset
	 *            offset in <code>in<code>, on which encoding is started
	 * @param length
	 *            number of bytes read from the input block
	 * @return compressed data block
	 */
	public static final SeabassCompBuffer compress(byte[] in, int offset, int length) {
		return compress(in, offset, length, null, DEF_SHIFT);
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            data to be compressed
	 * @return compressed data block
	 */
	public static final SeabassCompBuffer compress(SeabassCompBuffer in) {
		return compress(in.getData(), 0, in.getLength(), null, DEF_SHIFT);
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            data to be compressed
	 * @param out
	 *            buffer for decompressed data block
	 * @return reference to <code>out</code>
	 */
	public static final SeabassCompBuffer compress(SeabassCompBuffer in, SeabassCompBuffer out) {
		return compress(in.getData(), 0, in.getLength(), out, DEF_SHIFT);
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            compressed data block
	 * @return decompressed data block
	 */
	public static final SeabassCompBuffer decompress(byte[] in) {
		return decompress(in, 0, in.length, null);
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            compressed data block
	 * @param out
	 *            JSnappyBuffer for decompressed data block
	 * @return reference to <code>out</code>
	 */
	public static final SeabassCompBuffer decompress(byte[] in, SeabassCompBuffer out) {
		return decompress(in, 0, in.length, out);
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            byte array containing the compressed data block
	 * @param offset
	 *            offset in <code>in<code>, on which decoding is started
	 * @param length
	 *            length of compressed data block
	 * @return decompressed data block
	 */
	public static final SeabassCompBuffer decompress(byte[] in, int offset, int length) {
		return decompress(in, offset, length, null);
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            compressed data block
	 * @return decompressed data block
	 */
	public static final SeabassCompBuffer decompress(SeabassCompBuffer in) {
		return decompress(in.getData(), 0, in.getLength());
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            compressed data block
	 * @param out
	 *            JSnappyBuffer for decompressed data block
	 * @return reference to <code>out</code>
	 */
	public static final SeabassCompBuffer decompress(SeabassCompBuffer in, SeabassCompBuffer out) {
		return decompress(in.getData(), 0, in.getLength(), out);
	}

	/**
	 * データ圧縮.
	 *
	 * @param in
	 *            圧縮対象のバイナリを設定します.
	 * @param offset
	 *            圧縮対象バイナリの開始位置を設定します.
	 * @param length
	 *            圧縮対象の長さを設定します.
	 * @param out
	 *            圧縮結果を格納するBufferオブジェクトを設定します. 設定されない場合は、内部で新規作成されます.
	 * @param shift
	 *            圧縮テーブル長を増やす場合は、この値に整数を設定します.
	 * @return JSnappyBuffer 圧縮されたBufferオブジェクトが返却されます. この情報は第４引数でオブジェクトを渡した場合、
	 *         同様オブジェクトが返却されます.
	 */
	public static final SeabassCompBuffer compress(byte[] in, int offset, int length, SeabassCompBuffer out, int shift) {
		int lenM4 = length - 4;
		int offLenM4 = lenM4 + offset;
		int offLen = offset + length;
		int len, tLen, fp, io, o;
		int hOff = 0, hLen = 0;

		// Hash衝突率を下げるためのヒントを設定.
		int hashShift = nlzs(length);
		if (hashShift > 16) {
			hashShift = 31 - hashShift;
		}

		// 圧縮バッファの調整.
		if (out == null) {
			out = new SeabassCompBuffer(calcMaxCompressLength(length));
		} else {
			out.clear(calcMaxCompressLength(length));
		}
		byte[] target = out.getData();
		int targetIndex = 0;

		// ヘッダに元の長さをセット.
		int n = length;
		while (n > 0) {
			target[targetIndex++] = (n >= 128) ? (byte) (0x80 | (n & 0x7f)) : (byte) n;
			n >>= 7;
		}

		// 圧縮用Hash条件を生成.
		int _msk = (bitMask(length / 6) << shift) - 1;
		int[] _cc = new int[_msk + 1];

		// 先頭４バイトの圧縮用Hash条件をセット.
		int iLen = (offLenM4 < offset + 4) ? offLenM4 : offset + 4;
		for (int i = offset; i < iLen; i++) {
			_cc[(((((in[i] & 0xff) << 24) | ((in[i + 1] & 0xff) << 16) | ((in[i + 2] & 0xff) << 8) | (in[i + 3] & 0xff))
					* HASH) >> hashShift) & _msk] = i;
		}

		int lastHit = offset;
		for (int i = offset + 4; i < offLenM4; i++) {

			// 圧縮条件が存在する場合.
			if ((n = ((in[i] & 0xff) << 24) | ((in[i + 1] & 0xff) << 16) | ((in[i + 2] & 0xff) << 8)
					| (in[i + 3] & 0xff)) == (((in[fp = _cc[((n * HASH) >> hashShift) & _msk]] & 0xff) << 24)
							| ((in[fp + 1] & 0xff) << 16) | ((in[fp + 2] & 0xff) << 8) | (in[fp + 3] & 0xff))
					&& fp + 4 < i && i + 4 < offLen) {

				// 圧縮対象の同一条件を検索.
				if (in[fp + 4] == in[i + 4]) {
					// 1バイトずつ一致チェック.
					// 圧縮連続長.
					hLen = 5;
					o = fp + 5;
					io = i + 5;

					// 圧縮対象同一までチェック.
					for (tLen = (io + LIMIT < offLen) ? ((o + LIMIT < i) ? LIMIT : i - o) : offLen - io; hLen < tLen
							&& in[o++] == in[io++]; hLen++)
						;
				} else {
					hLen = 4; // 圧縮連続長.
				}

				// 圧縮位置をセット.
				hOff = i - fp;

				// 圧縮用Hash条件をセット.
				_cc[((n * HASH) >> hashShift) & _msk] = i;

			}
			// 圧縮条件が存在しない場合.
			else {

				// 圧縮用Hash条件をセット.
				_cc[((n * HASH) >> hashShift) & _msk] = i;

				// 圧縮処理なし.
				continue;
			}

			// 非圧縮情報をセット.
			if (lastHit < i) {
				// (3bit)ヘッド[0]をセット.
				if ((len = (i - lastHit) - 1) < 60) {
					// 非圧縮条件が60バイト未満の場合.
					target[targetIndex++] = (byte) (len << 2);
				} else if (len < 0x100) {
					// 非圧縮条件が256バイト未満の場合.
					target[targetIndex] = b60;
					target[targetIndex + 1] = (byte) len;
					targetIndex += 2;
				} else if (len < 0x10000) {
					// 非圧縮条件が65536バイト未満の場合.
					target[targetIndex] = b61;
					target[targetIndex + 1] = (byte) len;
					target[targetIndex + 2] = (byte) (len >> 8);
					targetIndex += 3;
				} else if (len < 0x1000000) {
					// 非圧縮条件が16777216バイト未満の場合.
					target[targetIndex] = b62;
					target[targetIndex + 1] = (byte) len;
					target[targetIndex + 2] = (byte) (len >> 8);
					target[targetIndex + 3] = (byte) (len >> 16);
					targetIndex += 4;
				} else {
					// 非圧縮条件が16777215バイト以上の場合.
					target[targetIndex] = b63;
					target[targetIndex + 1] = (byte) len;
					target[targetIndex + 2] = (byte) (len >> 8);
					target[targetIndex + 3] = (byte) (len >> 16);
					target[targetIndex + 4] = (byte) (len >> 24);
					targetIndex += 5;
				}
				System.arraycopy(in, lastHit, target, targetIndex, len + 1);
				targetIndex += len + 1;
				lastHit = i;
			}

			// 圧縮位置をセット.
			if (hLen <= 11 && hOff < 2048) {
				// (3bit)ヘッド[1]をセット.
				target[targetIndex] = (byte) (1 | ((hLen - 4) << 2) | ((hOff >> 3) & 0xe0));
				target[targetIndex + 1] = (byte) (hOff & 0xff);
				targetIndex += 2;
			} else if (hOff < 65536) {
				// (3bit)ヘッド[2]をセット.
				target[targetIndex] = (byte) (2 | ((hLen - 1) << 2));
				target[targetIndex + 1] = (byte) (hOff);
				target[targetIndex + 2] = (byte) (hOff >> 8);
				targetIndex += 3;
			} else {
				// (3bit)ヘッド[3]をセット.
				target[targetIndex] = (byte) (3 | ((hLen - 1) << 2));
				target[targetIndex + 1] = (byte) (hOff);
				target[targetIndex + 2] = (byte) (hOff >> 8);
				target[targetIndex + 3] = (byte) (hOff >> 16);
				target[targetIndex + 4] = (byte) (hOff >> 24);
				targetIndex += 5;
			}

			// 圧縮用Hash条件をセット.
			tLen = (lastHit > offLenM4) ? offLenM4 : lastHit;
			for (; i < tLen; i++) {
				_cc[(((((in[i] & 0xff) << 24) | ((in[i + 1] & 0xff) << 16) | ((in[i + 2] & 0xff) << 8)
						| (in[i + 3] & 0xff)) * HASH) >> hashShift) & _msk] = i;
			}
			lastHit = i + hLen;

			tLen = (lastHit - 1 > offLenM4) ? offLenM4 : lastHit - 1;
			for (; i < tLen; i++) {
				_cc[(((((in[i] & 0xff) << 24) | ((in[i + 1] & 0xff) << 16) | ((in[i + 2] & 0xff) << 8)
						| (in[i + 3] & 0xff)) * HASH) >> hashShift) & _msk] = i;
			}
			i = lastHit - 1;
		}

		// 終了時に非圧縮情報が存在する場合.
		if (lastHit < offLen) {
			// (3bit)ヘッド[0]をセット.
			if ((len = (offLen - lastHit) - 1) < 60) {
				target[targetIndex++] = (byte) (len << 2);
			} else if (len < 0x100) {
				target[targetIndex] = b60;
				target[targetIndex + 1] = (byte) len;
				targetIndex += 2;
			} else if (len < 0x10000) {
				target[targetIndex] = b61;
				target[targetIndex + 1] = (byte) len;
				target[targetIndex + 2] = (byte) (len >> 8);
				targetIndex += 3;
			} else if (len < 0x1000000) {
				target[targetIndex] = b62;
				target[targetIndex + 1] = (byte) len;
				target[targetIndex + 2] = (byte) (len >> 8);
				target[targetIndex + 3] = (byte) (len >> 16);
				targetIndex += 4;
			} else {
				target[targetIndex] = b63;
				target[targetIndex + 1] = (byte) len;
				target[targetIndex + 2] = (byte) (len >> 8);
				target[targetIndex + 3] = (byte) (len >> 16);
				target[targetIndex + 4] = (byte) (len >> 24);
				targetIndex += 5;
			}
			System.arraycopy(in, lastHit, target, targetIndex, len + 1);
			targetIndex += len + 1;
		}
		out.setLength(targetIndex);
		return out;
	}

	/**
	 * データ解凍.
	 *
	 * @param in
	 *            解凍対象のバイナリを設定します.
	 * @param offset
	 *            解凍対象バイナリの開始位置を設定します.
	 * @param length
	 *            解凍対象の長さを設定します.
	 * @param out
	 *            解凍結果を格納するBufferオブジェクトを設定します. 設定されない場合は、内部で新規作成されます.
	 * @return JSnappyBuffer 解凍されたBufferオブジェクトが返却されます. この情報は第４引数でオブジェクトを渡した場合、
	 *         同様オブジェクトが返却されます.
	 * @exception 例外.
	 */
	public static final SeabassCompBuffer decompress(final byte[] in, final int offset, final int length, SeabassCompBuffer out) {
		int p;
		int sourceIndex = offset;
		int targetLength = 0;

		// 全体の長さを取得.
		p = 0;
		do {
			targetLength += (in[sourceIndex] & 0x7f) << (p++ * 7);
		} while ((in[sourceIndex++] & 0x80) == 0x80);

		// 解凍データ長をセット.
		if (out == null) {
			out = new SeabassCompBuffer(targetLength);
		} else {
			out.clear(targetLength);
		}

		out.setLength(targetLength);
		final byte[] outBuffer = out.getData();

		int c, bc;
		int n = 0, o = 0;
		int targetIndex = 0;
		final int offLen = offset + length;

		while (sourceIndex < offLen && targetIndex < targetLength) {

			// 対象ブロック毎の処理.
			if ((bc = in[sourceIndex] & 3) == 0) {

				// 非圧縮情報の取得.
				if ((o = (n = (in[sourceIndex++] >> 2) & 0x3f) - 60) > -1) {
					for (o++, c = 1, n = (in[sourceIndex]
							& 0xff); c < o; n |= (in[sourceIndex + c] & 0xff) << (c * 8), c++)
						;
					sourceIndex += o;
				}
				System.arraycopy(in, sourceIndex, outBuffer, targetIndex, n + 1);
				sourceIndex += n + 1;
				targetIndex += n + 1;

				continue;
			}
			// 圧縮情報の取得.
			else if (bc == 1) {// 1.
				n = ((in[sourceIndex] >> 2) & 0x7) + 4;
				o = ((in[sourceIndex] & 0xe0) << 3) | (in[sourceIndex + 1] & 0xff);
				sourceIndex += 2;
			} else if (bc == 2) {// 2.
				n = ((in[sourceIndex] >> 2) & 0x3f) + 1;
				o = (in[sourceIndex + 1] & 0xff) | ((in[sourceIndex + 2] & 0xff) << 8);
				sourceIndex += 3;
			} else { // 3.
				n = ((in[sourceIndex] >> 2) & 0x3f) + 1;
				o = (in[sourceIndex + 1] & 0xff) | ((in[sourceIndex + 2] & 0xff) << 8)
						| ((in[sourceIndex + 3] & 0xff) << 16) | ((in[sourceIndex + 4] & 0xff) << 24);
				sourceIndex += 5;
			}
			// 圧縮情報のセット.
			for (p = targetIndex - o, c = p + n; p < c; outBuffer[targetIndex++] = outBuffer[p++])
				;
		}

		// 処理範囲を超えている場合はエラー.
		if (targetIndex > targetLength) {
			throw new SeabassCompException("Superfluous input data encountered on offset (index:" + targetIndex + " max:"
					+ targetLength + ")");
		}
		return out;
	}

	/** 連続左ゼロビット長を取得. **/
	protected static final int nlzs(int x) {
		x |= (x >> 1);
		x |= (x >> 2);
		x |= (x >> 4);
		x |= (x >> 8);
		x |= (x >> 16);
		x = (x & 0x55555555) + (x >> 1 & 0x55555555);
		x = (x & 0x33333333) + (x >> 2 & 0x33333333);
		x = (x & 0x0f0f0f0f) + (x >> 4 & 0x0f0f0f0f);
		x = (x & 0x00ff00ff) + (x >> 8 & 0x00ff00ff);
		return (x & 0x0000ffff) + (x >> 16 & 0x0000ffff);
	}

	/** ビットサイズの取得. **/
	protected static final int bitMask(int x) {
		if (x <= 256) {
			return 256;
		}
		x |= (x >> 1);
		x |= (x >> 2);
		x |= (x >> 4);
		x |= (x >> 8);
		x |= (x >> 16);
		x = (x & 0x55555555) + (x >> 1 & 0x55555555);
		x = (x & 0x33333333) + (x >> 2 & 0x33333333);
		x = (x & 0x0f0f0f0f) + (x >> 4 & 0x0f0f0f0f);
		x = (x & 0x00ff00ff) + (x >> 8 & 0x00ff00ff);
		x = (x & 0x0000ffff) + (x >> 16 & 0x0000ffff);
		return 1 << (((x & 0x0000ffff) + (x >> 16 & 0x0000ffff)) - 1);
	}
}
