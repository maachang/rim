package rim;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import rim.exception.RimException;

/**
 * Rimファイルをロード.
 */
public class LoadRim {
	private LoadRim() {
	}
	
	/**
	 * Rimファイルをロード.
	 * @param rimFileName ロードするRimファイル名を設定します.
	 * @return Rin ロードされたRimオブジェクトが返却されます.
	 * @exception IOException I/O例外.
	 */
	public static final Rim load(String rimFileName)
		throws IOException {
		return load(new BufferedInputStream(new FileInputStream(rimFileName)));
	}
	
	/**
	 * Rimファイルをロード.
	 * @param in ロードするInputStreamを設定します.
	 * @return Rin ロードされたRimオブジェクトが返却されます.
	 * @exception IOException I/O例外.
	 */
	public static final Rim load(InputStream in)
		throws IOException {
		try {
			// バッファ関連の情報生成.
			final byte[] tmp = new byte[8];
			final Object[] strBuf = new Object[] {
				new byte[64]
			};
			
			// シンボルのチェック.
			checkSimbol(in, tmp);
			
			// ヘッダ情報を取得.
			Object[] headers = readHeader(in, tmp, strBuf);
			int columnLength = (Integer)headers[0];
			String[] columns = (String[])headers[1];
			ColumnType[] columnTypes = (ColumnType[])headers[2];
			headers = null;
			
			// それぞれの文字列を表現する長さを管理するヘッダバイト数を取得.
			// (1byte).
			readBinary(tmp, in, 1);
			final int stringHeaderLength = bin1Int(tmp);
			
			// 全行数を読み込む(4byte).
			readBinary(tmp, in, 4);
			final int rowAll = bin4Int(tmp);
			
			// 全行数に対する長さ管理をするバイト数を取得.
			final int byte1_4Len = RimUtil.intLengthByByte1_4Length(rowAll);
			
			// 登録されているインデックス数を取得.
			readBinary(tmp, in, 2);
			final int indexLength = bin2Int(tmp);
			
			//System.out.println(" columnLength: " + columnLength
			//		+ " stringHeaderLength: " + stringHeaderLength
			//		+ " rowAll: " + rowAll
			//		+ " byte1_4Len: " + byte1_4Len
			//		+ " indexLength: " + indexLength
			//);
			
			// 返却するRimオブジェクトを生成.
			final Rim ret = new Rim(columns, columnTypes, rowAll, indexLength);
			columns = null;
			
			// Body情報を取得.
			readBody(ret, in, tmp, strBuf, stringHeaderLength, columnTypes, columnLength,
				rowAll);
			columnTypes = null;
			
			// 登録インデックスを取得.
			readIndex(ret, in, tmp, strBuf, stringHeaderLength, byte1_4Len, indexLength);
			
			// fix.
			ret.fix();
			
			// クローズ.
			in.close(); in = null;
			return ret;
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch(Exception e) {}
			}
		}
	}
	
	// 1バイトのバイナリをInt変換.
	private static final int bin1Int(byte[] tmp) {
		return tmp[0] & 0x000000ff;
	}
	
	// 2バイトのバイナリをInt変換.
	private static final int bin2Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 8)
			|  (tmp[1] & 0x000000ff);
	}
	
	// 3バイトのバイナリをInt変換.
	private static final int bin3Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 16)
			| ((tmp[1] & 0x000000ff) << 8)
			|  (tmp[2] & 0x000000ff);
	}
	
	// 4バイトのバイナリをInt変換.
	private static final int bin4Int(byte[] tmp) {
		return
			  ((tmp[0] & 0x000000ff) << 24)
			| ((tmp[1] & 0x000000ff) << 16)
			| ((tmp[2] & 0x000000ff) << 8)
			|  (tmp[3] & 0x000000ff);
	}

	// 8バイトのバイナリをLong変換.
	private static final long bin8Long(byte[] tmp) {
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
	
	// 1byte から 4byte までの条件を取得.
	private static final int bin1_4Int(byte[] tmp, int byte1_4Len) {
		switch(byte1_4Len) {
		case 1: return bin1Int(tmp);
		case 2: return bin2Int(tmp);
		case 3: return bin3Int(tmp);
		case 4: return bin4Int(tmp);
		default: return bin4Int(tmp);
		}
	}
	
	// 指定長のバイナリを取得.
	private static final void readBinary(byte[] out, InputStream in, int len)
		throws IOException {
		final int rLen = in.read(out, 0, len);
		if(len != rLen) {
			System.out.println("len: " + len + " rLen: " + rLen);
			throw new RimException("Failed to read Rim information.");
		}
	}
	
	// String読み込み用の文字列バッファを取得.
	private static final byte[] getReadStringBuffer(Object[] strBuf, int len) {
		byte[] b = (byte[])strBuf[0];
		if(b.length < len) {
			b = new byte[len];
			strBuf[0] = b;
		}
		return b;
	}
	
	// シンボルのチェック.
	private static final void checkSimbol(InputStream in, byte[] tmp)
		throws IOException {
		int len = in.read(tmp, 0, RimConstants.SIMBOL_BINARY.length);
		if(len != RimConstants.SIMBOL_BINARY.length) {
			throw new RimException("Not in Rim format.");
		}
		for(int i = 0; i < len; i ++) {
			if(tmp[i] != RimConstants.SIMBOL_BINARY[i]) {
				throw new RimException("Not in Rim format.");
			}
		}
	}
	
	// ヘッダ情報を取得.
	private static final Object[] readHeader(InputStream in, byte[] tmp,
		Object[] strBuf) throws IOException {
		
		int i, oLen;
		byte[] bstr;
		
		// 列数を取得(2byte).
		readBinary(tmp, in, 2);
		int columnLength = bin2Int(tmp);
				
		// 列名を取得(len:1byte, utf8).
		String[] columns = new String[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列名の長さを取得(1byte)
			readBinary(tmp, in, 1);
			oLen = bin1Int(tmp);
			// 列名を取得(UTF8).
			bstr = getReadStringBuffer(strBuf, oLen);
			readBinary(bstr, in, oLen);
			columns[i] = new String(bstr, 0, oLen, "UTF8");
		}
		
		// 列型を取得(1byte).
		ColumnType[] columnTypes = new ColumnType[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列型を取得(1byte)
			readBinary(tmp, in, 1);
			columnTypes[i] = ColumnType.get(bin1Int(tmp));
		}
		// [0]: 列数, [1]: 列名, [2]: 列型.
		return new Object[] {columnLength, columns, columnTypes};
	}
	
	// Booleanオブジェクトを読み込む.
	private static final Boolean readBoolean(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 1);
		return bin1Int(tmp) != 0;
	}
	
	// Byteオブジェクトを読み込む.
	private static final Byte readByte(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 1);
		return (byte)bin1Int(tmp);
	}
	
	// Shortオブジェクトを読み込む.
	private static final Short readShort(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 2);
		return (short)bin2Int(tmp);
	}
	
	// Integerオブジェクトを読み込む.
	private static final Integer readInteger(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 4);
		return bin4Int(tmp);
	}
	
	// Longオブジェクトを読み込む.
	private static final Long readLong(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return bin8Long(tmp);
	}
	
	// Floatオブジェクトを読み込む.
	private static final Float readFloat(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 4);
		return Float.intBitsToFloat(bin4Int(tmp));
	}
	
	// Doubleオブジェクトを読み込む.
	private static final Double readDouble(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return Double.longBitsToDouble(bin8Long(tmp));
	}
	
	// Stringオブジェクトを読み込む.
	private static final String readString(InputStream in, byte[] tmp,
		Object[] strBuf, int stringHeaderLength) throws IOException {
		readBinary(tmp, in, stringHeaderLength);
		int len = bin1_4Int(tmp, stringHeaderLength);
		if(len == 0) {
			return "";
		}
		byte[] bin = getReadStringBuffer(strBuf, len);
		readBinary(bin, in, len);
		return new String(bin, 0, len, "UTF8");
	}
	
	// Dateオブジェクトを読み込む.
	private static final Date readDate(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return new Date(bin8Long(tmp));
	}
	
	// 指定型の要素を取得.
	private static final Object getValue(InputStream in, byte[] tmp, Object[] strBuf,
		int stringHeaderLength, ColumnType type)
		throws IOException {
		switch(type) {
		case Boolean:
			return readBoolean(in, tmp);
		case Byte:
			return readByte(in, tmp);
		case Short:
			return readShort(in, tmp);
		case Integer:
			return readInteger(in, tmp);
		case Long:
			return readLong(in, tmp);
		case Float:
			return readFloat(in, tmp);
		case Double:
			return readDouble(in, tmp);
		case String:
			return readString(in, tmp, strBuf, stringHeaderLength);
		case Date:
			return readDate(in, tmp);
		}
		throw new RimException("Unknown column type: " + type);
	}

	// bodyを取得.
	private static final void readBody(Rim out, InputStream in, byte[] tmp, Object[] strBuf,
		int stringHeaderLength, ColumnType[] columnTypes, int columnLength,
		int rowAll) throws IOException {
		int i, j;
		Object[] row;
		RimBody body = out.getBody();
		for(i = 0; i < rowAll; i ++) {
			row = new Object[columnLength];
			for(j = 0; j < columnLength; j ++) {
				row[j] = getValue(in, tmp, strBuf, stringHeaderLength, columnTypes[j]);
			}
			body.add(row);
		}
	}

	// 登録インデックスを取得.
	@SuppressWarnings("rawtypes")
	private static final void readIndex(Rim out, InputStream in, byte[] tmp, Object[] strBuf,
		int stringHeaderLength, int byte1_4Len, int indexLength)
		throws IOException {
		
		int i, j, len;
		int columnNo;
		int planIndexSize;
		int rowIdLength;
		Object value;
		RimIndex index;
		ColumnType columnType;
		int[] rowIdList = null;
		
		// 全体のインデックス情報のループ.
		for(i = 0; i < indexLength; i ++) {
			
			// 対象インデックスの列番号を取得.
			readBinary(tmp, in, 2);
			columnNo = bin2Int(tmp);
			
			// 対象インデックスの総行数を取得.
			readBinary(tmp, in, byte1_4Len);
			planIndexSize = bin1_4Int(tmp, byte1_4Len);
			
			// 今回処理するインデックスを登録.
			index = out.registerIndex(columnNo, planIndexSize);
			
			// このインデックスの列型を取得.
			columnType = index.getColumnType();
			
			// インデックス追加完了までループ.
			while(true) {
				
				// 1つの最適化されたインデックス情報を取得.
				value = getValue(in, tmp, strBuf, stringHeaderLength, columnType);
				
				// 1つの同一要素に対する行番号数を取得.
				readBinary(tmp, in, byte1_4Len);
				rowIdLength = bin1_4Int(tmp, byte1_4Len);
				
				// RowIdList のバッファが足りない場合は生成.
				if(rowIdList == null || rowIdList.length < rowIdLength) {
					rowIdList = new int[rowIdLength];
				}
				
				// RowIdListをセット.
				for(j = 0; j < rowIdLength; j ++) {
					readBinary(tmp, in, byte1_4Len);
					rowIdList[j] = bin1_4Int(tmp, byte1_4Len);
				}
				
				// 対象のインデックスに情報を追加.
				len = index.add((Comparable)value, rowIdList, rowIdLength);
				// 予定長の取得が行われた場合.
				if(planIndexSize <= len) {
					// 予定長より大きな情報取得が行われた場合は例外出力.
					if(planIndexSize != len) {
						throw new RimException(
							"Information larger than the expected index length(" +
							planIndexSize + ") was read: " + len);
					}
					// このインデックス追加が完了した場合.
					break;
				}
			}
		}
	}
	
	
	public static final void main(String[] args) throws Exception {
		String file = "Z:/home/maachang/project/rim/sampleData/race_horse_master.rim";
		
		Rim rim = load(file);
	}
}
