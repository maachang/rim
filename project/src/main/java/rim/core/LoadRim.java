package rim.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import rim.Rim;
import rim.RimConstants;
import rim.compress.CompressBuffer;
import rim.compress.CompressType;
import rim.compress.Lz4Compress;
import rim.compress.ZstdCompress;
import rim.exception.RimException;
import rim.util.FileUtil;
import rim.util.UTF8IO;
import rim.util.seabass.SeabassCompress;
import rim.util.seabass.SeabassCompressBuffer;

/**
 * Rimファイルをロード.
 */
public class LoadRim {
	private LoadRim() {
	}
	
	// 利用頻度の高いパラメータを１つにまとめた内容.
	private static final class RimParams {
		// 属性情報.
		Object attribute;
		// テンポラリバッファ.
		byte[] tmp;
		// 文字列テンポラリバッファ.
		Object[] strBuf;
		// 文字列の長さを保持するバイト値.
		int stringHeaderLength;
		// 行数に応じた行情報を保持するバイト値.
		int byte1_4Len;
		// データ塊を一時受け取るバッファ.
		byte[] chunkedBuffer;
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
			// よく使うパラメータをまとめたオブジェクトを作成.
			RimParams params = new RimParams();
			
			// バッファ関連の情報生成.
			params.tmp = new byte[8];
			params.strBuf = new Object[] {
				new byte[64]
				,new char[64]
			};
			params.chunkedBuffer = new byte[1024];
			
			// シンボルのチェック.
			checkSimbol(in, params.tmp);
			
			// 圧縮タイプを取得(1byte).
			readBinary(params.tmp, in, 1);
			CompressType compressType = CompressType.get(bin1Int(params.tmp));
			
			// 圧縮タイプが「デフォルト圧縮」の場合.
			if(CompressType.Default == compressType) {
				
				// 属性にSeabassCompのバッファを生成して設定.
				params.attribute = new SeabassCompressBuffer();
			// 圧縮タイプが「GZIP圧縮」の場合.
			} else if(CompressType.Gzip == compressType) {
				
				// 属性にRbbOutputStreamを設定.
				params.attribute = new RbbOutputStream();
			// 圧縮タイプが「LZ4圧縮」の場合.
			} else if(CompressType.LZ4 == compressType) {
				
				// LZ4が利用可能かチェック.
				if(!Lz4Compress.getInstance().isSuccessLibrary()) {
					throw new RimException("LZ4 is not available.");
				}
				
				// 属性にCompressBufferのバッファを生成して設定.
				params.attribute = new CompressBuffer();
			// 圧縮タイプが「Zstd圧縮」の場合.
			} else if(CompressType.Zstd == compressType) {
				
				// Zstdが利用可能かチェック.
				if(!ZstdCompress.getInstance().isSuccessLibrary()) {
					throw new RimException("Zstd is not available.");
				}
				
				// 属性にCompressBufferのバッファを生成して設定.
				params.attribute = new CompressBuffer();
			}
			
			// ヘッダ情報を取得.
			Object[] headers = readHeader(in, params);
			int columnLength = (Integer)headers[0];
			String[] columns = (String[])headers[1];
			ColumnType[] columnTypes = (ColumnType[])headers[2];
			headers = null;
			
			// それぞれの文字列を表現する長さを管理するヘッダバイト数を取得.
			// (1byte).
			readBinary(params.tmp, in, 1);
			params.stringHeaderLength = bin1Int(params.tmp);
			
			// 全行数を読み込む(4byte).
			readBinary(params.tmp, in, 4);
			final int rowAll = bin4Int(params.tmp);
			
			// 全行数に対する長さ管理をするバイト数を取得.
			params.byte1_4Len = RimUtil.intLengthByByte1_4Length(rowAll);
			
			// 登録されているインデックス数を取得.
			readBinary(params.tmp, in, 2);
			final int indexLength = bin2Int(params.tmp);
			
			//System.out.println(" columnLength: " + columnLength
			//		+ " stringHeaderLength: " + params.stringHeaderLength
			//		+ " rowAll: " + rowAll
			//		+ " byte1_4Len: " + params.byte1_4Len
			//		+ " compressType: " + compressType
			//		+ " indexLength: " + indexLength
			//);
			
			// 返却するRimオブジェクトを生成.
			final RimBody body = new RimBody(columns, columnTypes, rowAll);
			final Rim ret = new Rim(body, indexLength);
			columns = null;
			
			// Body情報を取得.
			readBody(ret, body, in, params, columnTypes, compressType,
				columnLength, rowAll);
			columnTypes = null;
			
			// 登録インデックスを取得.
			readIndex(ret, in, params, compressType, indexLength);
			
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
	private static final int bin1_4Int(RimParams params) {
		return bin1_4Int(params.tmp, params.byte1_4Len);
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
			throw new RimException("Failed to read Rim information(" +
				len + " / " + rLen + ")");
		}
	}
	
	
	// Stringオブジェクトを取得.
	// UTF8専用.
	private static final String convertString(InputStream in, RimParams params,
		int strHeaderLen)
		throws IOException {
		readBinary(params.tmp, in, strHeaderLen);
		final int len = bin1_4Int(params.tmp, strHeaderLen);
		if(len == 0) {
			return "";
		}
		// 文字列取得用バッファを取得.
		final byte[] bin = RimUtil.getStringByteArray(params.strBuf, len);
		readBinary(bin, in, len);
		
		// バイナリから文字列変換用バッファを取得.
		final char[] chr = RimUtil.getStringCharArray(params.strBuf, len);
		return UTF8IO.decode(chr, bin, 0, len);
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
	private static final Object[] readHeader(InputStream in, RimParams params)
		throws IOException {
		
		int i;
		
		// 列数を取得(2byte).
		readBinary(params.tmp, in, 2);
		int columnLength = bin2Int(params.tmp);
				
		// 列名を取得(len:1byte, utf8).
		String[] columns = new String[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列名を取得(ヘッダ:1byte)
			columns[i] = convertString(in, params, 1);
		}
		
		// 列型を取得(1byte).
		ColumnType[] columnTypes = new ColumnType[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列型を取得(1byte)
			readBinary(params.tmp, in, 1);
			columnTypes[i] = ColumnType.get(bin1Int(params.tmp));
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
	private static final String readString(InputStream in, RimParams params)
		throws IOException {
		return convertString(in, params, params.stringHeaderLength);
	}
	
	// Dateオブジェクトを読み込む.
	private static final Date readDate(InputStream in, byte[] tmp)
		throws IOException {
		readBinary(tmp, in, 8);
		return new Date(bin8Long(tmp));
	}
	
	// 圧縮されてる場合は解凍して取得.
	private static final byte[] readDecompress(int[] outLen, CompressType compressType,
		RimParams params, boolean noCompressFlag, int len) throws IOException {
		
		// 圧縮無しの場合.
		// 圧縮フラグがOFFの場合.
		if(!noCompressFlag || CompressType.None == compressType) {
			// そのまま返却.
			outLen[0] = len;
			return params.chunkedBuffer;
			
		// 圧縮タイプが「デフォルト圧縮」の場合.
		} else if(CompressType.Default == compressType) {
			// attributeからバッファを取得.
			SeabassCompressBuffer buf = (SeabassCompressBuffer)params.attribute;
			
			// 解凍後のデータサイズを取得.
			int dLen = SeabassCompress.decompressLength(params.chunkedBuffer, 0, len);
			buf.clear(dLen);
			
			// 解凍.
			SeabassCompress.decompress(buf, params.chunkedBuffer, 0, len);
			
			// 解凍内容を返却.
			outLen[0] = dLen;
			return buf.getRawBuffer();
			
		// 圧縮タイプが「GZIP圧縮」の場合.
		} else if(CompressType.Gzip == compressType) {
			// attributeからRbbOutputStreamを取得.
			RbbOutputStream buf = (RbbOutputStream)params.attribute;
			buf.reset();
			
			// chunkedBufferをRbInputStreamにセットしたGZIP解凍生成.
			GZIPInputStream in = new GZIPInputStream(
				new RbInputStream(params.chunkedBuffer, 0, len));
			
			final byte[] b = new byte[1024];
			int rLen;
			while((rLen = in.read(b)) != -1) {
				buf.write(b, 0, rLen);
			}
			in.close(); in = null;
			
			outLen[0] = buf.getLength();
			return buf.getRawBuffer();
		// 圧縮タイプが「LZ4圧縮」の場合.
		} else if(CompressType.LZ4 == compressType) {
			Lz4Compress lz4 = Lz4Compress.getInstance();
			
			// attributeからバッファを取得.
			CompressBuffer buf = (CompressBuffer)params.attribute;
			
			// 塊情報の先頭情報から、解凍対象の長さを取得.
			int[] outReadByte = new int[1];
			final int dLen = lz4.decompressLength(
				outReadByte, params.chunkedBuffer, 0, len);
			
			// 解凍バッファに解凍内容をセット.
			buf.clear(dLen);
			buf.setLimit(dLen);
			
			// 解凍.
			lz4.decompress(buf, params.chunkedBuffer, outReadByte[0]);
			
			// 解凍内容を返却.
			outLen[0] = dLen;
			return buf.getRawBuffer();
			
		// 圧縮タイプが「Zstd圧縮」の場合.
		} else if(CompressType.Zstd == compressType) {
			ZstdCompress zstd = ZstdCompress.getInstance();
			
			// attributeからバッファを取得.
			CompressBuffer buf = (CompressBuffer)params.attribute;
			
			// 解凍.
			zstd.decompress(buf, params.chunkedBuffer, 0, len);
		
			// 解凍内容を返却.
			outLen[0] = buf.getLimit();
			return buf.getRawBuffer();
			
		// 不明な圧縮タイプ.
		} else {
			throw new RimException(
				"Illegal compression type is set: " + compressType);
		}
	}
	
	// 指定型の要素を取得.
	private static final Object getValue(InputStream in, RimParams params,
		ColumnType type) throws IOException {
		switch(type) {
		case Boolean:
			return readBoolean(in, params.tmp);
		case Byte:
			return readByte(in, params.tmp);
		case Short:
			return readShort(in, params.tmp);
		case Integer:
			return readInteger(in, params.tmp);
		case Long:
			return readLong(in, params.tmp);
		case Float:
			return readFloat(in, params.tmp);
		case Double:
			return readDouble(in, params.tmp);
		case String:
			return readString(in, params);
		case Date:
			return readDate(in, params.tmp);
		}
		throw new RimException("Unknown column type: " + type);
	}
	
	// 指定型の要素群を連続して取得.
	private static final void getValues(Object[] out, InputStream in, RimParams params,
		ColumnType type)
		throws IOException {
		final int len = out.length;
		switch(type) {
		case Boolean:
			for(int i = 0; i < len; i ++) {
				out[i] = readBoolean(in, params.tmp);
			}
			return;
		case Byte:
			for(int i = 0; i < len; i ++) {
				out[i] = readByte(in, params.tmp);
			}
			return;
		case Short:
			for(int i = 0; i < len; i ++) {
				out[i] = readShort(in, params.tmp);
			}
			return;
		case Integer:
			for(int i = 0; i < len; i ++) {
				out[i] = readInteger(in, params.tmp);
			}
			return;
		case Long:
			for(int i = 0; i < len; i ++) {
				out[i] = readLong(in, params.tmp);
			}
			return;
		case Float:
			for(int i = 0; i < len; i ++) {
				out[i] = readFloat(in, params.tmp);
			}
			return;
		case Double:
			for(int i = 0; i < len; i ++) {
				out[i] = readDouble(in, params.tmp);
			}
			return;
		case String:
			for(int i = 0; i < len; i ++) {
				out[i] = readString(in, params);
			}
			return;
		case Date:
			for(int i = 0; i < len; i ++) {
				out[i] = readDate(in, params.tmp);
			}
			return;
		}
		throw new RimException("Unknown column type: " + type);
	}

	// bodyを取得.
	private static final void readBody(Rim out, RimBody body, InputStream in,
		RimParams params, ColumnType[] columnTypes, CompressType compressType,
		int columnLength, int rowAll) throws IOException {
		int i, len;
		byte[] data;
		Object[] columns;
		RbInputStream rbIn;
		final int[] dataLen = new int[1];
		for(i = 0; i < columnLength; i ++) {
			
			// 圧縮フラグを取得.
			boolean compFlag = readBoolean(in, params.tmp);
			
			// データ塊長を取得.
			len = readInteger(in, params.tmp);
			
			// 塊受付バッファサイズが小さい場合.
			if(params.chunkedBuffer.length < len) {
				// lenに合わせて再生成.
				params.chunkedBuffer = new byte[len];
			}
			
			// 塊データを取得.
			readBinary(params.chunkedBuffer, in, len);
			
			// 圧縮されている場合、塊を解凍して取得.
			data = readDecompress(dataLen, compressType, params, compFlag, len);
			
			// 塊情報をRbInputStreamに置き換える.
			rbIn = new RbInputStream(data, 0, dataLen[0]);
			
			// １つの列の全行情報を作成.
			columns = new Object[rowAll];
			
			// 1つの列の全行情報を取得.
			getValues(columns, rbIn, params, columnTypes[i]);
			
			// bodyに列の全行情報をセット.
			body.setColumns(i, columns);
			
			// クリア.
			columns = null;
			rbIn = null;
		}
	}

	// 登録インデックスを取得.
	@SuppressWarnings("rawtypes")
	private static final void readIndex(Rim out, InputStream in, RimParams params,
		CompressType compressType, int indexLength) throws IOException {
		
		int i, j, len;
		int columnNo;
		int planIndexSize;
		int rowIdLength;
		Object value;
		RimIndex index;
		ColumnType columnType;
		int[] rowIdList = null;
		
		byte[] data;
		RbInputStream rbIn;
		final int[] dataLen = new int[1];

		
		// 全体のインデックス情報のループ.
		for(i = 0; i < indexLength; i ++) {
			
			// 対象インデックスの列番号を取得.
			readBinary(params.tmp, in, 2);
			columnNo = bin2Int(params.tmp);
			
			// 対象インデックスの総行数を取得.
			readBinary(params.tmp, in, params.byte1_4Len);
			planIndexSize = bin1_4Int(params);
			
			// 今回処理するインデックスを登録.
			index = out.registerIndex(columnNo, planIndexSize);
			
			// このインデックスの列型を取得.
			columnType = index.getColumnType();
			
			// 圧縮フラグを取得.
			boolean compFlag = readBoolean(in, params.tmp);
			
			// データ塊長を取得.
			len = readInteger(in, params.tmp);
			
			// 塊受付バッファサイズが小さい場合.
			if(params.chunkedBuffer.length < len) {
				// lenに合わせて再生成.
				params.chunkedBuffer = new byte[len];
			}
			
			// 塊データを取得.
			readBinary(params.chunkedBuffer, in, len);
			
			// 圧縮されている場合、塊を解凍して取得.
			data = readDecompress(dataLen, compressType, params, compFlag, len);
			
			// 塊情報をRbInputStreamに置き換える.
			rbIn = new RbInputStream(data, 0, dataLen[0]);
			
			// インデックス追加完了までループ.
			while(true) {
				
				// 1つの最適化されたインデックス情報を取得.
				value = getValue(rbIn, params, columnType);
				
				// 1つの同一要素に対する行番号数を取得.
				readBinary(params.tmp, rbIn, params.byte1_4Len);
				rowIdLength = bin1_4Int(params);
				
				// RowIdList のバッファが足りない場合は生成.
				if(rowIdList == null || rowIdList.length < rowIdLength) {
					rowIdList = new int[rowIdLength];
				}
				
				// RowIdListをセット.
				for(j = 0; j < rowIdLength; j ++) {
					readBinary(params.tmp, rbIn, params.byte1_4Len);
					rowIdList[j] = bin1_4Int(params);
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
	
	// 対象ファイルのロード時間を計測.
	protected static final void testLoadTime(String[] args) throws Exception {
		String loadFileName;
		
		if(args.length == 0 || (loadFileName = args[0]).isEmpty()) {
			System.err.println("ロード対象のファイル名が指定されていません.");
			System.exit(1);
			return;
		}
		
		if(!FileUtil.isFile(loadFileName)) {
			System.err.println("ロード対象のファイルは存在しません: " + loadFileName);
			System.exit(1);
			return;
		}
		
		// 計測開始.
		long tm = System.currentTimeMillis();
		
		// データロード.
		load(loadFileName);
		
		// 計測終了.
		tm = System.currentTimeMillis() - tm;
		
		System.out.println("ロードファイル名: " + loadFileName);
		System.out.println("ファイルサイズ: " + FileUtil.getFileLength(loadFileName) + " byte");
		System.out.println("ロード時間: " + tm + " ミリ秒");
		
		System.exit(0);
	}
	
	// テスト用ロード時間を計測する.
	public static final void main(String[] args) throws Exception {
		String file = "Z:/home/maachang/project/rim/sampleData/race_horse_master.rim";
		
		Rim rim = load(file);
		
		int execType = 0;
		
		// 対象列名.
		String column = "id";
		
		// 昇順・降順フラグ.
		boolean ascFlg = true;
		
		// notフラグ.
		boolean notFlag = false;
		
		Object[] values = null;
		
		for(int x = 0; x < 7; x ++) {
		for(int y = 0; y <= 1; y ++) {
		
		execType = x;
		ascFlg = y == 0;
		
		RimBody body = rim.getBody();
		RimIndex index = rim.getIndex(column);
		
		ResultSearch<Integer> ri;
		
		System.out.println();
		
		System.out.println("[index] column: " + column + " exec: " + execTypeName(execType) + " ascFlg: " + ascFlg);
		
		ri = null;
		switch(execType) {
		case 0:
			values = new Object[] {
				100
			};
			System.out.println("eq(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.eq(ascFlg, values[0]);
			break;
		case 1:
			values = new Object[] {
				2284
			};
			System.out.println("gt(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.gt(ascFlg, values[0]);
			break;
		case 2:
			values = new Object[] {
				2284
			};
			System.out.println("ge(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.ge(ascFlg, values[0]);
			break;
		case 3:
			values = new Object[] {
				4
			};
			System.out.println("lt(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.lt(ascFlg, values[0]);
			break;
		case 4:
			values = new Object[] {
				4
			};
			System.out.println("le(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.le(ascFlg, values[0]);
			break;
		case 5:
			values = new Object[] {
				2283, 9998
			};
			System.out.println("between(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.between(ascFlg, values[0], values[1]);
			break;
		case 6:
			values = new Object[] {
				100, 500, 2000, 5000, 9998
			};
			System.out.println("in(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.in(ascFlg, values);
			break;
		}
		viewResultSearch(ri, body, 5);
		
		System.out.println();
		
		System.out.println("[body] column: " + column + " exec: " + execTypeName(execType) + " ascFlg: " + ascFlg);
		
		ri = null;
		switch(execType) {
		case 0:
			values = new Object[] {
				100
			};
			System.out.println("eq(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.eq(ascFlg, notFlag, column, values[0]);
			break;
		case 1:
			values = new Object[] {
				2284
			};
			System.out.println("gt(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.gt(ascFlg, notFlag, column, values[0]);
			break;
		case 2:
			values = new Object[] {
				2284
			};
			System.out.println("ge(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.ge(ascFlg, notFlag, column, values[0]);
			break;
		case 3:
			values = new Object[] {
				4
			};
			System.out.println("lt(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.lt(ascFlg, notFlag, column, values[0]);
			break;
		case 4:
			values = new Object[] {
				4
			};
			System.out.println("le(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.le(ascFlg, notFlag, column, values[0]);
			break;
		case 5:
			values = new Object[] {
				2283, 9998
			};
			System.out.println("between(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.between(ascFlg, notFlag, column, values[0], values[1]);
			break;
		case 6:
			values = new Object[] {
				100, 500, 2000, 5000, 9998
			};
			System.out.println("in(ascFlg: " + ascFlg + " notFlg: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.in(ascFlg, notFlag, column, values);
			break;
		}
		viewResultSearch(ri, body, 5);
		
		System.out.println();
		
		}}
	}
	
	private static final void viewResultSearch(ResultSearch<Integer> ri, RimBody body, int maxCnt) {
		int cnt = 0;
		while(ri.hasNext()) {
			if(cnt > maxCnt) {
				break;
			}
			ri.next();
			System.out.println(" (" + cnt + ") value: " + ri.getValue() + " lineNo: " + ri.getLineNo() + " " +
				"");
				//body.getRow(ri.getLineNo()));
			cnt ++;
		}
	}
	
	private static final String execTypeName(int no) {
		switch(no) {
		case 0: return "eq";
		case 1: return "gt";
		case 2: return "ge";
		case 3: return "lt";
		case 4: return "le";
		case 5: return "between";
		case 6: return "in";
		}
		return "unknown";
	}
	
	private static final String strAry(Object[] ary) {
		int len = ary.length;
		if(len == 1) {
			return String.valueOf(ary[0]);
		}
		StringBuilder buf = new StringBuilder("[");
		for(int i = 0; i < len; i ++) {
			if(i != 0) {
				buf.append(", ");
			}
			buf.append(ary[i]);
		}
		return buf.append("]").toString();
	}

}