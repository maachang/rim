package rim.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import rim.exception.RimException;
import rim.util.UTF8IO;

public class BinaryIO {
	private BinaryIO() {}
	
	/**
	 * テンポラリバイナリを生成.
	 * @return byte[] テンポラリ用のバイナリが返却されます.
	 */
	public static final byte[] createTmp() {
		return new byte[8];
	}
	
	/**
	 * StringをUTF8変換する時に使う一時バッファの生成.
	 * @param addChar trueの場合 getStringCharArray が利用出来ます.
	 * @return Object[] バッファ情報が返却されます.
	 */
	public static final Object[] createStringBuffer(boolean addChar) {
		if(addChar) {
			return new Object[] {
				new byte[64]
				,new char[64]
			};
		} else {
			return new Object[] {
				new byte[64]
			};
		}
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
	
	/**
	 * 長さからその内容に収まるバイト数を取得.
	 * @param len 長さを設定します.
	 * @return 長さの範囲に収まるバイト数が返却されます.
	 */
	public static final int byte1_4Length(int len) {
		if(len < 0) {
			return 4;
		} else if(len <= 0x000000ff) {
			return 1;
		} else if(len <= 0x0000ffff) {
			return 2;
		} else if(len <= 0x00ffffff) {
			return 3;
		}
		return 4;
	}
	
	/**
	 * 1byteのデーターセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len1Binary(byte[] tmp, int len) {
		tmp[0] = (byte)(len & 0x000000ff);
		return tmp;
	}

	/**
	 * 2byteのデーターセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len2Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0x0000ff00) >> 8);
		tmp[1] = (byte) (len & 0x000000ff);
		return tmp;
	}

	/**
	 * 3byteのデーターセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len3Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0x00ff0000) >> 16);
		tmp[1] = (byte)((len & 0x0000ff00) >> 8);
		tmp[2] = (byte) (len & 0x000000ff);
		return tmp;
	}
	
	/**
	 * 4byteのデーターセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len4Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0xff000000) >> 24);
		tmp[1] = (byte)((len & 0x00ff0000) >> 16);
		tmp[2] = (byte)((len & 0x0000ff00) >> 8);
		tmp[3] = (byte) (len & 0x000000ff);
		return tmp;
	}

	/**
	 * 8byteのデーターセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len8Binary(byte[] tmp, long len) {
		tmp[0] = (byte)((len & 0xff00000000000000L) >> 56L);
		tmp[1] = (byte)((len & 0x00ff000000000000L) >> 48L);
		tmp[2] = (byte)((len & 0x0000ff0000000000L) >> 40L);
		tmp[3] = (byte)((len & 0x000000ff00000000L) >> 32L);
		tmp[4] = (byte)((len & 0x00000000ff000000L) >> 24L);
		tmp[5] = (byte)((len & 0x0000000000ff0000L) >> 16L);
		tmp[6] = (byte)((len & 0x000000000000ff00L) >> 8L);
		tmp[7] = (byte) (len & 0x00000000000000ffL);
		return tmp;
	}

	/**
	 * 1byte から 4byte までの条件に対して、データーセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len1_4 1Byteから4Byteまでの値を設定します.
	 * @param len 書き込む長さを設定します.
	 * @return byte[] 設定された内容が返却されます.
	 */
	public static final byte[] len1_4Binary(byte[] tmp, int len1_4, int len) {
		switch(len1_4) {
		case 1: return len1Binary(tmp, len);
		case 2: return len2Binary(tmp, len);
		case 3: return len3Binary(tmp, len);
		case 4: return len4Binary(tmp, len);
		default: return len4Binary(tmp, len);
		}
	}
	
	/**
	 * Savingされた数字をセット.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len 書き込む数字を設定します.
	 *            この値にマイナス値は設定出来ません.
	 *            また、最大数は 0x3fffffff を超えて設定出来ません.
	 * @return int 書き込まれたバイト数が返却されます.
	 */
	public static final int len4SavingBinary(byte[] tmp, int len) {
		// 先頭バイトの0x03の２ビット分に、長さに必要なバイト長を
		// 設定して出力容量を削減します.
		// ただし、マイナス値には対応してないので、主に文字列長を
		// 設定する場合に利用します.
		if(len < 0) {
			throw new RimException(
				"Negative values cannot be set: " + len);
		} else if(len > 0x3fffffff) {
			throw new RimException("It's too long: " + len);
		} else if(len <= 0x003f) {
			// 1byte.
			tmp[0] = (byte)(((len & 0x0000003f) << 2) | 0);
			return 1;
		} else if(len <= 0x003fff) {
			// 2byte.
			tmp[0] = (byte)(((len & 0x0000003f) << 2) | 1);
			tmp[1] = (byte)( (len & 0x00003fc0) >> 6);
			return 2;
		} else if(len <= 0x003fffff) {
			// 3byte.
			tmp[0] = (byte)(((len & 0x0000003f) << 2) | 2);
			tmp[1] = (byte)( (len & 0x00003fc0) >> 6);
			tmp[2] = (byte)( (len & 0x003fc000) >> 14);
			return 3;
		} else {
			// 4byte.
			tmp[0] = (byte)(((len & 0x0000003f) << 2) | 3);
			tmp[1] = (byte)( (len & 0x00003fc0) >> 6);
			tmp[2] = (byte)( (len & 0x003fc000) >> 14);
			tmp[3] = (byte)( (len & 0x3fc00000) >> 22);
			return 4;
		}
	}

	/**
	 * Booleanオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeBoolean(
		OutputStream out, byte[] tmp, Boolean v)
		throws IOException {
		// valueがnullの場合は０(false)を設定.
		if(v == null) {
			len1Binary(tmp, 0);
		} else {
			len1Binary(tmp, v ? 1 : 0);
		}
		out.write(tmp, 0, 1);
	}

	/**
	 * 1バイトのIntを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeInt1(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len1Binary(tmp, 0);
		} else {
			len1Binary(tmp, v.intValue());
		}
		out.write(tmp, 0, 1);
	}
	
	/**
	 * 2バイトのIntを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeInt2(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len2Binary(tmp, 0);
		} else {
			len2Binary(tmp, v.intValue());
		}
		out.write(tmp, 0, 2);
	}
	
	/**
	 * 3バイトのIntを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeInt3(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len3Binary(tmp, 0);
		} else {
			len3Binary(tmp, v.intValue());
		}
		out.write(tmp, 0, 3);
	}


	/**
	 * 4バイトのIntを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeInt4(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len4Binary(tmp, 0);
		} else {
			len4Binary(tmp, v.intValue());
		}
		out.write(tmp, 0, 4);
	}
	
	/**
	 * Longオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeLong(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, 0L);
		} else {
			len8Binary(tmp, v.longValue());
		}
		out.write(tmp, 0, 8);
	}

	/**
	 * Floatオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeFloat(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len4Binary(tmp, Float.floatToIntBits(0f));
		} else {
			len4Binary(tmp, Float.floatToIntBits(v.floatValue()));
		}
		out.write(tmp, 0, 4);
	}
	
	/**
	 * Doubleオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeDouble(OutputStream out, byte[] tmp, Number v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, Double.doubleToLongBits(0d));
		} else {
			len8Binary(tmp, Double.doubleToLongBits(v.doubleValue()));
		}
		out.write(tmp, 0, 8);
	}

	/**
	 * Stringオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param strBuf 文字列バッファを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeString(OutputStream out, byte[] tmp,
		Object[] strBuf, String v)
		throws IOException {
		// valueがnullの場合は０文字を設定.
		if(v == null) {
			writeSavingBinary(out, tmp, 0);
		// 文字列がnullや空の場合も０文字を設定.
		} else if(v.isEmpty()) {
			writeSavingBinary(out, tmp, 0);
		} else {
			byte[] b = getStringByteArray(strBuf, v.length() * 4);
			int len = UTF8IO.encode(b, v);
			// 文字列のバイナリ長を設定.
			writeSavingBinary(out, tmp, len);
			// 文字列を設定.
			out.write(b, 0, len);
		}
	}
	
	/**
	 * Dateオブジェクトを書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeDate(OutputStream out, byte[] tmp, Date v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, 0L);
		} else {
			len8Binary(tmp, v.getTime());
		}
		out.write(tmp, 0, 8);
	}
	
	/**
	 * 1byte から 4byte までの条件を書き込む.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param len1_4 1Byteから4Byteまでの値を設定します.
	 * @param v 設定内容を設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void write1_4Binary(
		OutputStream out, byte[] tmp, int len1_4, int v)
		throws IOException {
		out.write(len1_4Binary(tmp, len1_4, v), 0, len1_4);
	}
	
	/**
	 * Savingされた数字をを書き込み.
	 * @param out 出力先のOutputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param v 数字をを設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void writeSavingBinary(
		OutputStream out, byte[] tmp, int v)
		throws IOException {
		final int len = len4SavingBinary(tmp, v);
		out.write(tmp, 0, len);
	}
	
	/**
	 * 1バイトのバイナリをInt変換.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int intが返却されます.
	 */
	public static final int bin1Int(byte[] tmp) {
		return tmp[0] & 0x000000ff;
	}
	
	/**
	 * 2バイトのバイナリをInt変換.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int intが返却されます.
	 */
	public static final int bin2Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 8)
			|  (tmp[1] & 0x000000ff);
	}
	
	/**
	 * 3バイトのバイナリをInt変換.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int intが返却されます.
	 */
	public static final int bin3Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 16)
			| ((tmp[1] & 0x000000ff) << 8)
			|  (tmp[2] & 0x000000ff);
	}
	
	/**
	 * 4バイトのバイナリをInt変換.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int intが返却されます.
	 */
	public static final int bin4Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 24)
			| ((tmp[1] & 0x000000ff) << 16)
			| ((tmp[2] & 0x000000ff) << 8)
			|  (tmp[3] & 0x000000ff);
	}

	/**
	 * 8バイトのバイナリをLong変換.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int intが返却されます.
	 */
	public static final long bin8Long(byte[] tmp) {
		return 
			  ((tmp[0] & 0x00000000000000ffL) << 56L)
			| ((tmp[1] & 0x00000000000000ffL) << 48L)
			| ((tmp[2] & 0x00000000000000ffL) << 40L)
			| ((tmp[3] & 0x00000000000000ffL) << 32L)
			| ((tmp[4] & 0x00000000000000ffL) << 24L)
			| ((tmp[5] & 0x00000000000000ffL) << 16L)
			| ((tmp[6] & 0x00000000000000ffL) << 8L)
			|  (tmp[7] & 0x00000000000000ffL);
	}
	
	/**
	 * 1byte から 4byte までの条件を取得.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param byte1_4Len 1Byteから4Byteまでの値を設定します.
	 * @return int intが返却されます.
	 */
	public static final int bin1_4Int(byte[] tmp, int byte1_4Len) {
		switch(byte1_4Len) {
		case 1: return bin1Int(tmp);
		case 2: return bin2Int(tmp);
		case 3: return bin3Int(tmp);
		case 4: return bin4Int(tmp);
		default: return bin4Int(tmp);
		}
	}
	
	/**
	 * バイナリからSavingTopバイト数を取得.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int 0 ～ 3までのバイト数が返却されます.
	 */
	public static final int binSavingTopByte(byte[] tmp) {
		return tmp[0] & 0x03;
	}
	
	/**
	 * バイナリからSavingTopデータを取得.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int Topデータが返却されます.
	 */
	public static final int binSavingTopValue(byte[] tmp) {
		return (tmp[0] & 0x00fc) >> 2;
	}
	
	/**
	 * バイナリからSavingNextデータとTopデータを含めた
	 * Int情報情報を取得.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param topByte binSavingTopByteで取得した値を設定します.
	 * @param topValue binSavingTopValueで取得した値を設定します.
	 * @return int 数字情報が返却されます.
	 */
	public static final int binSavingNextValue(
		byte[] tmp, int topByte, int topValue) {
		switch(topByte) {
		case 0:
			return topValue;
		case 1:
			return topValue |
				((tmp[0] & 0x00ff) << 6);
		case 2:
			return topValue |
				((tmp[0] & 0x00ff) << 6) |
				((tmp[1] & 0x00ff) << 14);
		}
		// case 3:
		return topValue |
			((tmp[0] & 0x00ff) << 6) |
			((tmp[1] & 0x00ff) << 14) |
			((tmp[2] & 0x00ff) << 22);
	}
	
	/**
	 * 指定長のバイナリを取得.
	 * @param out 出力対象のバイナリを設定します.
	 * @param in InputStreamを設定します.
	 * @param len 取得する長さを設定します.
	 * @throws IOException I/O例外.
	 */
	public static final void readBinary(byte[] out, InputStream in, int len)
		throws IOException {
		final int rLen = in.read(out, 0, len);
		if(len != rLen) {
			throw new RimException("Failed to read Rim information(" +
				len + " / " + rLen + ")");
		}
	}
	
	/**
	 * Booleanを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return boolean オブジェクトが返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final boolean readBoolean(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 1);
		return bin1Int(tmp) != 0;
	}
	
	/**
	 * 1バイトのintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readInt1(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 1);
		return bin1Int(tmp);
	}
	
	/**
	 * 2バイトのintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readInt2(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 2);
		return bin2Int(tmp);
	}
	
	/**
	 * 3バイトのintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readInt3(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 3);
		return bin3Int(tmp);
	}
	
	/**
	 * 4バイトのintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readInt4(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 4);
		return bin4Int(tmp);
	}
	
	/**
	 * longを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return kong longが返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final long readLong(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return bin8Long(tmp);
	}
	
	/**
	 * floatを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return float float が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final float readFloat(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 4);
		return Float.intBitsToFloat(bin4Int(tmp));
	}
	
	/**
	 * doubleを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return double doubleが返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final double readDouble(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return Double.longBitsToDouble(bin8Long(tmp));
	}
	
	/**
	 * Stringを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param strBuf 文字列バッファを設定します.
	 * @return String 文字列が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final String readString(
		InputStream in, byte[] tmp, Object[] strBuf)
		throws IOException {
		final int len = readSavingInt(in, tmp);
		if(len == 0) {
			return "";
		}
		// 文字列取得用バッファを取得.
		final byte[] bin = getStringByteArray(strBuf, len);
		readBinary(bin, in, len);
		
		// バイナリから文字列変換用バッファを取得.
		final char[] chr = getStringCharArray(strBuf, len);
		return UTF8IO.decode(chr, bin, 0, len);
	}
	
	/**
	 * Dateを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return Date オブジェクトが返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final Date readDate(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return new Date(bin8Long(tmp));
	}
	
	/**
	 * 1byte から 4byte までのintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @param byte1_4Len 1Byteから4Byteまでの値を設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readBin1_4Int(InputStream in, byte[] tmp, int byte1_4Len)
		throws IOException {
		readBinary(tmp, in, byte1_4Len);
		return bin1_4Int(tmp, byte1_4Len);
	}
	
	/**
	 * Savingされたintを取得.
	 * @param in InputStreamを設定します.
	 * @param tmp テンポラリ用のバイナリを設定します.
	 * @return int int が返却されます.
	 * @throws IOException I/O例外.
	 */
	public static final int readSavingInt(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 1);
		int topByte = binSavingTopByte(tmp);
		int topValue = binSavingTopValue(tmp);
		if(topByte > 0) {
			readBinary(tmp, in, topByte);
			return binSavingNextValue(tmp, topByte, topValue);
		}
		return topValue;
	}
}
