package rim;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import rim.compress.CompressBuffer;
import rim.compress.CompressType;
import rim.compress.Lz4Compress;
import rim.compress.ZstdCompress;
import rim.core.BinaryIO;
import rim.core.ColumnType;
import rim.core.RbInputStream;
import rim.core.RbbOutputStream;
import rim.exception.RimException;
import rim.index.GeneralIndex;
import rim.index.GeoIndex;
import rim.index.NgramIndex;
import rim.util.FileUtil;
import rim.util.seabass.SeabassCompress;
import rim.util.seabass.SeabassCompressBuffer;

/**
 * Rimファイルをロード.
 */
public class LoadRim {
	private LoadRim() {
	}
	
	private static final int GENERAL_INDEX = 0;
	private static final int GEO_INDEX = 1;
	private static final int NGRAM_INDEX = 2;
	
	// 利用頻度の高いパラメータを１つにまとめた内容.
	private static final class RimParams {
		// 属性情報.
		Object attribute;
		// テンポラリバッファ.
		byte[] tmp;
		// 文字列テンポラリバッファ.
		Object[] strBuf;
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
			params.tmp = BinaryIO.createTmp();
			params.strBuf = BinaryIO.createStringBuffer(true);
			params.chunkedBuffer = new byte[1024];
			
			// シンボルのチェック.
			checkSimbol(in, params.tmp);
			
			// 圧縮タイプを取得(1byte).
			final CompressType compressType = CompressType.get(
				BinaryIO.readInt1(in, params.tmp));
			
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
			
			// 全行数を読み込む(Saving).
			final int rowAll = BinaryIO.readSavingInt(in, params.tmp);
			
			// 全行数に対する長さ管理をするバイト数を取得.
			params.byte1_4Len = BinaryIO.byte1_4Length(rowAll);
			
			// 登録されているインデックス数を取得(Saving).
			final int indexLength = BinaryIO.readSavingInt(in, params.tmp);
			
			// 登録されているGeoインデックス数を取得(Saving).
			final int geoIndexLength = BinaryIO.readSavingInt(in, params.tmp);
			
			// 登録されているNgramインデックス数を取得(Saving).
			final int ngramIndexLength = BinaryIO.readSavingInt(in, params.tmp);
			
			// 返却するRimオブジェクトを生成.
			final RimBody body = new RimBody(columns, columnTypes, rowAll);
			final Rim ret = new Rim(body, indexLength, geoIndexLength,
				ngramIndexLength);
			columns = null;
			
			// Body情報を取得.
			readBody(ret, body, in, params, columnTypes, compressType,
				columnLength, rowAll);
			columnTypes = null;
			
			// 登録インデックスを取得.
			readIndex(ret, in, params, compressType, indexLength);
			
			// 登録Geoインデックスを取得.
			readGeoIndex(ret, in, params, compressType, geoIndexLength);
			
			// 登録Ngramインデックスを取得.
			readNGramIndex(ret, in, params, compressType, ngramIndexLength);
			
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
	
	// ヘッダ情報を取得.
	private static final Object[] readHeader(InputStream in, RimParams params)
		throws IOException {
		
		int i;
		
		// 列数を取得(Saving).
		final int columnLength = BinaryIO.readSavingInt(in, params.tmp);
				
		// 列名を取得(utf8).
		final String[] columns = new String[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列名を取得
			columns[i] = BinaryIO.readString(in, params.tmp, params.strBuf);
		}
		
		// 列型を取得(1byte).
		final ColumnType[] columnTypes = new ColumnType[columnLength];
		for(i = 0; i < columnLength; i ++) {
			// 列型を取得(1byte)
			columnTypes[i] = ColumnType.get(
				BinaryIO.readInt1(in, params.tmp));
		}
		// [0]: 列数, [1]: 列名, [2]: 列型.
		return new Object[] {columnLength, columns, columnTypes};
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
			boolean compFlag = BinaryIO.readBoolean(in, params.tmp);
			
			// データ塊長を取得.
			len = BinaryIO.readSavingInt(in, params.tmp);
			
			// 塊受付バッファサイズが小さい場合.
			if(params.chunkedBuffer.length < len) {
				// lenに合わせて再生成.
				params.chunkedBuffer = new byte[len];
			}
			
			// 塊データを取得.
			BinaryIO.readBinary(params.chunkedBuffer, in, len);
			
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
	private static final void readIndex(Rim out, InputStream in, RimParams params,
		CompressType compressType, int indexLength) throws IOException {
		int columnNo;
		int planIndexSize;
		GeneralIndex index;
		
		// 全体のインデックス情報のループ.
		for(int i = 0; i < indexLength; i ++) {
			
			// 対象インデックスの列番号を取得(Saving).
			columnNo = BinaryIO.readSavingInt(in, params.tmp);
			
			// 対象インデックスの総行数を取得(1~4byte).
			planIndexSize = BinaryIO.readBin1_4Int(
				in, params.tmp, params.byte1_4Len);
			
			// 今回処理するインデックスを登録.
			index = out.registerIndex(columnNo, planIndexSize);
			
			// １つのインデックスを読み込む.
			readOneIndex(in, params, compressType, index, GENERAL_INDEX,
				planIndexSize);
		}
	}
	
	// 登録Geoインデックスを取得.
	private static final void readGeoIndex(Rim out, InputStream in, RimParams params,
		CompressType compressType, int indexLength) throws IOException {
		int latColumnNo;
		int lonColumnNo;
		int planIndexSize;
		GeoIndex index;
		
		// 全体のインデックス情報のループ.
		for(int i = 0; i < indexLength; i ++) {
			
			// 緯度列番号を取得(Saving).
			latColumnNo = BinaryIO.readSavingInt(in, params.tmp);
			
			// 経度列番号を取得(Saving).
			lonColumnNo = BinaryIO.readSavingInt(in, params.tmp);
			
			// 対象インデックスの総行数を取得(1~4byte).
			planIndexSize = BinaryIO.readBin1_4Int(
				in, params.tmp, params.byte1_4Len);
			
			// 今回処理するインデックスを登録.
			index = out.registerGeoIndex(
				latColumnNo, lonColumnNo, planIndexSize);
			
			// １つのインデックスを読み込む.
			readOneIndex(in, params, compressType, index, GEO_INDEX,
				planIndexSize);
		}
	}
	
	// 登録Ngramインデックスを取得.
	private static final void readNGramIndex(Rim out, InputStream in, RimParams params,
		CompressType compressType, int indexLength) throws IOException {
		int columnNo;
		int ngramLength;
		int planIndexSize;
		NgramIndex index;
		
		// 全体のインデックス情報のループ.
		for(int i = 0; i < indexLength; i ++) {
			
			// 列番号を取得(Saving).
			columnNo = BinaryIO.readSavingInt(in, params.tmp);
			
			// パースするNgram長を取得(1byte).
			ngramLength = BinaryIO.readInt1(in, params.tmp);
			
			// 対象インデックスの総行数を取得(1~4byte).
			planIndexSize = BinaryIO.readBin1_4Int(
				in, params.tmp, params.byte1_4Len);
			
			// 今回処理するインデックスを登録.
			index = out.registerNgramIndex(
				columnNo, ngramLength, planIndexSize);
			
			// １つのインデックスを読み込む.
			readOneIndex(in, params, compressType, index, NGRAM_INDEX,
				planIndexSize);
		}
	}
	
	// Ngram要素を取得.
	private static final long getNgramValue(InputStream in, RimParams params,
		int ngramLength)
		throws IOException {
		switch(ngramLength) {
		// unigram.
		case 1:
			return ((long)BinaryIO.readInt2(in, params.tmp)) &
				0x000000000000ffffL;
		// bigram.
		case 2:
			return ((long)BinaryIO.readInt4(in, params.tmp)) &
				0x00000000ffffffffL;
		}
		// trigram.
		BinaryIO.readBinary(params.tmp, in, 6);
		final byte[] b = params.tmp;
		return ((b[0] & 0x00000000000000ffL) << 40) |
				((b[1] & 0x00000000000000ffL) << 32) |
				((b[2] & 0x00000000000000ffL) << 24) |
				((b[3] & 0x00000000000000ffL) << 16) |
				((b[4] & 0x00000000000000ffL) << 8) |
				 (b[5] & 0x00000000000000ffL);
	}
	
	// １つのインデックス情報を読み込む.
	@SuppressWarnings("rawtypes")
	private static final void readOneIndex(InputStream in, RimParams params,
		CompressType compressType, Object index, int indexType, int planIndexSize)
		throws IOException {
		
		int i, len;
		int rowIdLength;
		Object value;
		int[] rowIdList = null;
		int[] ngramPosition = null;
		
		byte[] data;
		RbInputStream rbIn;
		final int[] dataLen = new int[1];
		
		// このインデックスの列型を取得.
		int ngramLength = -1;
		ColumnType columnType = null;
		switch(indexType) {
		// Index.
		case GENERAL_INDEX:
			columnType = ((GeneralIndex)index).getColumnType();
			break;
		// GeoIndex.
		case GEO_INDEX:
			columnType = ColumnType.Long;
			break;
		// NgramIndex
		case NGRAM_INDEX:
			columnType = ColumnType.Long;
			ngramLength = ((NgramIndex)index).getNgramLength();
			break;
		}
		
		// 圧縮フラグを取得.
		boolean compFlag = BinaryIO.readBoolean(in, params.tmp);
		
		// データ塊長を取得.
		len = BinaryIO.readSavingInt(in, params.tmp);
		
		// 塊受付バッファサイズが小さい場合.
		if(params.chunkedBuffer.length < len) {
			// lenに合わせて再生成.
			params.chunkedBuffer = new byte[len];
		}
		
		// 塊データを取得.
		BinaryIO.readBinary(params.chunkedBuffer, in, len);
		
		// 圧縮されている場合、塊を解凍して取得.
		data = readDecompress(
			dataLen, compressType, params, compFlag, len);
		
		// 塊情報をRbInputStreamに置き換える.
		rbIn = new RbInputStream(data, 0, dataLen[0]);
		
		// インデックス追加完了までループ.
		while(true) {
			
			// インデックスの対象要素を取得.
			value = null;
			switch(indexType) {
			// Index.
			case GENERAL_INDEX:
				value = getValue(rbIn, params, columnType);
				break;
			// GeoIndex.
			case GEO_INDEX:
				value = getValue(rbIn, params, columnType);
				break;
			// NgramIndex.
			case NGRAM_INDEX:
				value = getNgramValue(rbIn, params, ngramLength);
				break;
			}
			
			// 1つの同一要素に対する行番号数を取得.
			rowIdLength = BinaryIO.readSavingInt(rbIn, params.tmp);
			
			// RowIdList のバッファが足りない場合は生成.
			if(rowIdList == null || rowIdList.length < rowIdLength) {
				rowIdList = new int[rowIdLength];
			}
			
			// 対象のインデックスに情報を追加.
			switch(indexType) {
			// Index.
			case GENERAL_INDEX:
				// RowIdListをセット.
				for(i = 0; i < rowIdLength; i ++) {
					rowIdList[i] = BinaryIO.readBin1_4Int(
						rbIn, params.tmp, params.byte1_4Len);
				}
				len = ((GeneralIndex)index).add(
					(Comparable)value, rowIdList, rowIdLength);
				break;
			// GeoIndex.
			case GEO_INDEX:
				// RowIdListをセット.
				for(i = 0; i < rowIdLength; i ++) {
					rowIdList[i] = BinaryIO.readBin1_4Int(
						rbIn, params.tmp, params.byte1_4Len);
				}
				len = ((GeoIndex)index).add(
					(Long)value, rowIdList, rowIdLength);
				break;
			// NgramIndex.
			case NGRAM_INDEX:
				// Ngramポジションのバッファが足りない場合は生成.
				if(ngramPosition == null || ngramPosition.length < rowIdLength) {
					ngramPosition = new int[rowIdLength];
				}
				// RowIdListとNgramポジションを取得.
				for(i = 0; i < rowIdLength; i ++) {
					// RowIdList.
					rowIdList[i] = BinaryIO.readBin1_4Int(
						rbIn, params.tmp, params.byte1_4Len);
					// ngramPosition.
					ngramPosition[i] = BinaryIO.readSavingInt(
						rbIn, params.tmp);
				}
				len = ((NgramIndex)index).add(
					(Long)value, rowIdList, ngramPosition, rowIdLength);
			}
			
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
			return BinaryIO.readBoolean(in, params.tmp);
		case Byte:
			return (byte)BinaryIO.readInt1(in, params.tmp);
		case Short:
			return (short)BinaryIO.readInt2(in, params.tmp);
		case Integer:
			return BinaryIO.readInt4(in, params.tmp);
		case Long:
			return BinaryIO.readLong(in, params.tmp);
		case Float:
			return BinaryIO.readFloat(in, params.tmp);
		case Double:
			return BinaryIO.readDouble(in, params.tmp);
		case String:
			return BinaryIO.readString(in, params.tmp, params.strBuf);
		case Date:
			return BinaryIO.readDate(in, params.tmp);
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
				out[i] = BinaryIO.readBoolean(in, params.tmp);
			}
			return;
		case Byte:
			for(int i = 0; i < len; i ++) {
				out[i] = (byte)BinaryIO.readInt1(in, params.tmp);
			}
			return;
		case Short:
			for(int i = 0; i < len; i ++) {
				out[i] = (short)BinaryIO.readInt2(in, params.tmp);
			}
			return;
		case Integer:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readInt4(in, params.tmp);
			}
			return;
		case Long:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readLong(in, params.tmp);
			}
			return;
		case Float:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readFloat(in, params.tmp);
			}
			return;
		case Double:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readDouble(in, params.tmp);
			}
			return;
		case String:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readString(in, params.tmp, params.strBuf);
			}
			return;
		case Date:
			for(int i = 0; i < len; i ++) {
				out[i] = BinaryIO.readDate(in, params.tmp);
			}
			return;
		}
		throw new RimException("Unknown column type: " + type);
	}

	
	// test.
	public static final void main(String[] args) throws Exception {
		//testSearch(args);
		//testGeoSearch(args);
		testNgramSearch(args);
	}
	
	// [テスト用]Ngram検索関連のテスト.
	public static final void testNgramSearch(String[] args) throws Exception {
		String file = "Z:/home/maachang/project/rim/sampleData/race_horse_master.rim";
		
		// ロード処理.
		Rim rim = load(file);
		
		NgramIndex index = rim.getNgramIndex("name");
		
		// 昇順の場合は true.
		boolean ascFlag = false;
		
		// 同一行を取得しない場合は true.
		boolean lineExclusion = true;
		
		// Ngram 検索ワード.
		String value = "ナイ";
		
		int cnt = 0;
		
		// 検索処理.
		RimResultNgram result = index.search(ascFlag, lineExclusion, value);
		while(result.hasNext()) {
			System.out.println("nextRow: " + result.nextRow());
			cnt ++;
		}
		System.out.println("件数 :" + cnt + " 件");
	}

	
	// [テスト用]Geo検索関連のテスト.
	public static final void testGeoSearch(String[] args) throws Exception {
		// 全国駅情報.
		String file = "Z:/home/maachang/project/rim/sampleData/station.rim";
		
		// ロード処理.
		Rim rim = load(file);
		
		// 駅の緯度経度インデックスを取得.
		GeoIndex index = rim.getGeoIndex("lat", "lon");
		
		// ベンチマーク計測.
		int benchLen = 1;
		
		// 昇順の場合は true.
		boolean ascFlag = true;
		
		// 東京タワーの周辺を検索.
		double lat = 35.658581;
		double lon = 139.745433;
		
		// 1000メートル半径を検索.
		int distance = 1000;
		
		RimResultGeo result;
		
		//long time;
		long all = 0L;
		/**
		for(int i = 0; i < benchLen; i ++) {
			time = System.currentTimeMillis();
			//result = index.searchRadius(ascFlag, lat, lon, distance);
			result = index.searchRadius(lat, lon, distance);
			while(result.hasNext()) {
				result.next();
			}
			all += System.currentTimeMillis() - time;
		}
		**/
		
		result = index.searchRadius(ascFlag, lat, lon, distance);
		//result = index.searchRadius(lat, lon, distance);
		
		int cnt = 0;
		while(result.hasNext()) {
			//result.next();
			System.out.println("nextRow: " + result.nextRow());
			System.out.println(" distance: " + result.getValue() +
				" / " + result.getStrictRedius() + " m");
			cnt ++;
		}
		System.out.println("駅 :" + cnt + " 件");
		
		System.out.println("time: " + (all) + " / " + benchLen + " msec");
	}
	
	// [テスト用]検索関連のテスト.
	public static final void testSearch(String[] args) throws Exception {
		String file = "Z:/home/maachang/project/rim/sampleData/race_horse_master.rim";
		
		Rim rim = load(file);
		
		int viweLen = 5;
		
		int execType = 0;
		
		// 対象列名.
		String column = "id";
		
		// 昇順・降順フラグ.
		boolean ascFlag = true;
		
		// notフラグ.
		boolean notFlag = false;
		
		Object[] values = null;
		
		for(int x = 0; x < 7; x ++) {
		for(int y = 0; y <= 1; y ++) {
		for(int z = 0; z <= 1; z ++) {
		
		execType = x;
		notFlag = y != 0;
		//notFlag = true;
		ascFlag = z == 0;
		
		RimBody body = rim.getBody();
		GeneralIndex index = rim.getIndex(column);
		
		RimResult ri;
		
		ri = null;
		switch(execType) {
		case 0:
			// a[t]n[f]: 3
			// a[t]n[t]: 3
			// a[f]n[f]: 2284
			// a[f]n[t]: 2284
			values = setValue(ascFlag, notFlag, 3, 3, 2283, 2283);
			
			System.out.println("[index]eq(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.eq(ascFlag, notFlag, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] eq(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.eq(ascFlag, notFlag, column, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 1:
			// a[t]n[f]: 3
			// a[t]n[t]: 3
			// a[f]n[f]: 2284
			// a[f]n[t]: 2284
			values = setValue(ascFlag, notFlag, 3, 3, 2284, 2284);
			
			System.out.println("[index]gt(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.gt(ascFlag, notFlag, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] gt(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.gt(ascFlag, notFlag, column, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 2:
			// a[t]n[f]: 3
			// a[t]n[t]: 3
			// a[f]n[f]: 2284
			// a[f]n[t]: 2284
			values = setValue(ascFlag, notFlag, 3, 3, 2284, 2284);
			
			System.out.println("[index]ge(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.ge(ascFlag, notFlag, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] ge(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.ge(ascFlag, notFlag, column, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 3:
			// a[t]n[f]: 3
			// a[t]n[t]: 3
			// a[f]n[f]: 2284
			// a[f]n[t]: 2284
			values = setValue(ascFlag, notFlag, 3, 3, 2284, 2284);
			
			System.out.println("[index]lt(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.lt(ascFlag, notFlag, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] lt(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.lt(ascFlag, notFlag, column, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 4:
			// a[t]n[f]: 3
			// a[t]n[t]: 3
			// a[f]n[f]: 2284
			// a[f]n[t]: 2284
			values = setValue(ascFlag, notFlag, 3, 3, 2284, 2284);
			
			System.out.println("[index]le(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.le(ascFlag, notFlag, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] le(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.le(ascFlag, notFlag, column, values[0]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 5:
			// a[t]n[f]: {3, 5}
			// a[t]n[t]: {3, 5}
			// a[f]n[f]: {2283, 9998}
			// a[f]n[t]: {2283, 9998}
			values = (Object[])(setValue(ascFlag, notFlag,
				new Object[] {3, 5},
				new Object[] {3, 5},
				new Object[] {2283, 9998},
				new Object[] {2283, 9998}
			)[0]);
			
			System.out.println("[index]between(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			
			ri = index.between(ascFlag, notFlag, values[0], values[1]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] between(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.between(ascFlag, notFlag, column, values[0], values[1]);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		case 6:
			// a[t]n[f]: {1, 3,-30, 5, 1000}
			// a[t]n[t]: {3, 5}
			// a[f]n[f]: {1, 3,-30, 5, 1000}
			// a[f]n[t]: {2283, 9998}
			values = (Object[])(setValue(ascFlag, notFlag,
				new Object[] {1, 3,-30, 5, 1000},
				new Object[] {2, 4, 6, 8, 10},
				new Object[] {1, 3,-30, 5, 1000},
				new Object[] {9998, 2283, 2281, 2279, 2277}
			)[0]);
			
			System.out.println("[index]in(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = index.in(ascFlag, notFlag, values);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			System.out.println("[body] in(ascFlag: " + ascFlag + " notFlag: " + notFlag +
				" column: " + column + " value: " + strAry(values));
			ri = body.in(ascFlag, notFlag, column, values);
			
			viewResultSearch(ri, body, viweLen);
			System.out.println();
			
			break;
		}
		
		}}}
		//}}
	}
	
	
	// [テスト用]対象ファイルのロード時間を計測.
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
	
	private static final void viewResultSearch(RimResult ri, RimBody body, int maxCnt) {
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
	
	/**
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
	**/
	
	private static final Object[] setValue(boolean ascFlag, boolean notFlag,
		Object atnf, Object atnt, Object afnf, Object afnt) {
		Object[] values;
		if(ascFlag) {
			if(notFlag) {
				values = new Object[] {
					atnt
				};
			} else {
				values = new Object[] {
					atnf
				};
			}
		} else {
			if(notFlag) {
				values = new Object[] {
					afnt
				};
			} else {
				values = new Object[] {
					afnf
				};
			}
		}
		return values;
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
