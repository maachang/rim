package rim;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import rim.compress.CompressBuffer;
import rim.compress.CompressType;
import rim.compress.Lz4Compress;
import rim.compress.ZstdCompress;
import rim.core.BinaryIO;
import rim.core.ColumnType;
import rim.core.RbbOutputStream;
import rim.core.SearchUtil;
import rim.exception.RimException;
import rim.geo.GeoQuad;
import rim.util.CsvReader;
import rim.util.CsvRow;
import rim.util.ObjectList;
import rim.util.seabass.SeabassCompress;
import rim.util.seabass.SeabassCompressBuffer;

/**
 * CSVファイルから、rim(readInMemory)データーを作成.
 *
 * 指定された列を型指定して、インデックス化します.
 * これにより、CSVデータを高速にインデックス検索が可能です.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SaveRim {
	// 読み込み対象のCSVデーター.
	private CsvReader csv;
	// 出力先のファイル名.
	private OutputStream rimOut;
	// 圧縮タイプ.
	private CompressType compressType;
	// オプション情報.
	private Object option;
	
	// 列型群.
	private ColumnType[] columnTypes;
	
	// インデックス列情報群.
	private ObjectList<IndexColumn> indexColumns =
		new ObjectList<IndexColumn>();
	// Geoインデックス列情報群.
	private ObjectList<GeoIndexColumn> geoIndexColumns =
		new ObjectList<GeoIndexColumn>();
	// Ngramインデックス列情報群.
	private ObjectList<NgramIndexColumn> ngramIndexColumns =
		new ObjectList<NgramIndexColumn>();
	
	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param OutputStream 出力先OutputStreamを設定します.
	 */
	public SaveRim(CsvReader csv, OutputStream rimOut) {
		this(csv, rimOut, CompressType.None, null);
	}
	
	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param OutputStream 出力先OutputStreamを設定します.
	 * @param compressType 圧縮タイプを設定します.
	 */
	public SaveRim(CsvReader csv, OutputStream rimOut,
		CompressType compressType) {
		this(csv, rimOut, compressType, null);
	}
	
	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param OutputStream 出力先OutputStreamを設定します.
	 * @param compressType 圧縮タイプを設定します.
	 * @param ngramLength パースするNgram長を設定します.
	 * @param option オプション情報を設定します.
	 */
	public SaveRim(CsvReader csv, OutputStream rimOut,
		CompressType compressType, Object option) {
		if(compressType == null) {
			compressType = CompressType.None;
		}
		this.csv = csv;
		this.rimOut = rimOut;
		this.compressType = compressType;
		this.option = option;
	}

	/**
	 * クローズ処理.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if(csv != null) {
			csv.close();
			csv = null;
		}
		if(rimOut != null) {
			try {
				rimOut.close();
			} catch(Exception e) {}
			rimOut = null;
		}
		columnTypes = null;
		compressType = null;
		option = null;
		indexColumns = null;
		geoIndexColumns = null;
		ngramIndexColumns = null;
	}

	// クローズチェック.
	private final void checkClose() {
		if(csv == null) {
			throw new RimException("It's already closed.");
		}
	}

	// CSVから列型群を生成.
	private final void createColumnTypes() {
		if(columnTypes != null) {
			return;
		}
		int len = csv.getHeaderSize();
		ColumnType[] list = new ColumnType[len];
		// CSVの１行目は列名で、２行目に各列の型設定が必須.
		if(!csv.hasNext()) {
			throw new RimException("It is not the CSV format to read to for Rim.");
		}
		// CSVで定義されている列型名から列型に変換.
		CsvRow row = csv.next();
		for(int i = 0; i < len; i ++) {
			list[i] = ColumnType.get(row.get(i));
		}
		columnTypes = list;
	}

	/**
	 * インデックス列追加.
	 * @param column 列名を設定します.
	 * @return SaveRim このオブジェクトが返却されます.
	 */
	public SaveRim addIndex(String column) {
		checkClose();
		int columnNo;
		// 指定されたインデックスの列名位置を取得.
		if((columnNo = csv.getHeader().search(column)) == -1) {
			throw new RimException("Specified column name does not exist: " + column);
		}
		// CSV定義の列型群を生成.
		createColumnTypes();
		// 列名位置のインデックス情報を作成.
		indexColumns.add(new IndexColumn(columnNo));
		return this;
	}
	
	/**
	 * Geoインデックス列追加.
	 * @param latColumn 緯度列名を設定します.
	 * @param lonColumn 経度列名を設定します.
	 * @return SaveRim このオブジェクトが返却されます.
	 */
	public SaveRim addGeoIndex(String latColumn, String lonColumn) {
		checkClose();
		int latColumnNo, lonColumnNo;
		// 指定されたインデックスの列名位置を取得.
		if((latColumnNo = csv.getHeader().search(latColumn)) == -1) {
			throw new RimException(
				"Specified latitude column name does not exist: " +
					latColumnNo);
		} else if((lonColumnNo = csv.getHeader().search(lonColumn)) == -1) {
			throw new RimException(
				"Specified longitude column name does not exist: " +
					lonColumnNo);
		}
		// CSV定義の列型群を生成.
		createColumnTypes();
		
		// 緯度・経度の型チェック.
		ColumnType latType = columnTypes[latColumnNo];
		ColumnType lonType = columnTypes[lonColumnNo];
		if(ColumnType.Float != latType && ColumnType.Double != latType) {
			throw new RimException(
				"The type of the specified latitude column is not floating point.");
		} else if(ColumnType.Float != lonType && ColumnType.Double != lonType) {
			throw new RimException(
				"The type of the specified longitude column is not floating point.");
		}
		
		// 列名位置のインデックス情報を作成.
		geoIndexColumns.add(new GeoIndexColumn(latColumnNo, lonColumnNo));
		return this;
	}
	
	/**
	 * Ngramインデックス列追加.
	 * @param column 列名を設定します.
	 * @return SaveRim このオブジェクトが返却されます.
	 */
	public SaveRim addNgramIndex(String column) {
		return addNgramIndex(column, RimConstants.DEFAULT_NGRAM_LENGTH);
	}
	
	/**
	 * Ngramインデックス列追加.
	 * @param column 列名を設定します.
	 * @param ngramLength パースするNgram長を設定します.
	 * @return SaveRim このオブジェクトが返却されます.
	 */
	public SaveRim addNgramIndex(String column, int ngramLength) {
		checkClose();
		int columnNo;
		// 指定されたインデックスの列名位置を取得.
		if((columnNo = csv.getHeader().search(column)) == -1) {
			throw new RimException("Specified column name does not exist: " + column);
		}
		// CSV定義の列型群を生成.
		createColumnTypes();
		
		// 緯度・経度の型チェック.
		ColumnType ngramType = columnTypes[columnNo];
		if(ColumnType.String != ngramType) {
			throw new RimException(
				"The type of the specified ngram column is not string.");
		}
		// 列名位置のNgramインデックス情報を作成.
		ngramIndexColumns.add(new NgramIndexColumn(columnNo, ngramLength));
		return this;
	}
	
	/**
	 * インデックスを作成.
	 * @return int 読み込まれたCSVの行数が返却されます.
	 * @throws IOException
	 */
	public int write() throws IOException {
		checkClose();
		try {
			// インデックスが１つも設定されていない場合.
			if(columnTypes == null) {
				createColumnTypes();
			}
			
			// よく使うパラメータをまとめたオブジェクトを作成.
			final RimParams params = new RimParams();
			
			// オプションを設定.
			params.option = this.option;
			
			// テンポラリ情報.
			params.tmp = BinaryIO.createTmp();
			params.strBuf = BinaryIO.createStringBuffer(false);
			params.rbb = new RbbOutputStream();
			
			// CSVデーターの読み込み.
			ObjectList[] body = readCsv(csv, columnTypes, indexColumns,
				geoIndexColumns, ngramIndexColumns);
			final int rowAll = body[0].size();

			// 全行数に対する長さ管理をするバイト数を取得.
			params.byte1_4Len = BinaryIO.byte1_4Length(rowAll);
			
			// 指定圧縮条件の初期化処理.
			initCompress(params, compressType);

			// シンボルを出力.
			rimOut.write(RimConstants.SIMBOL_BINARY);
			
			// 圧縮タイプを出力(1byte).
			BinaryIO.writeInt1(rimOut, params.tmp, compressType.getId());

			// ヘッダ情報を出力.
			writeHeader(rimOut, params, csv, columnTypes);
			
			// 全行数を書き込む(Saving).
			BinaryIO.writeSavingBinary(rimOut, params.tmp, rowAll);
			
			// 登録されてるインデックス数を書き込む(Saving).
			BinaryIO.writeSavingBinary(rimOut, params.tmp, indexColumns.size());
			
			// 登録されてるGeoインデックス数を書き込む(Saving).
			BinaryIO.writeSavingBinary(rimOut, params.tmp, geoIndexColumns.size());
			
			// 登録されてるNgramインデックス数を書き込む(Saving).
			BinaryIO.writeSavingBinary(rimOut, params.tmp, ngramIndexColumns.size());
			
			// bodyデータを出力.
			writeBody(rimOut, params, body, columnTypes, compressType);

			// インデックス情報を出力.
			writeIndex(rimOut, params, columnTypes, compressType, body, indexColumns);
			
			// Geoインデックス情報を出力.
			writeGeoIndex(rimOut, params, compressType, body, geoIndexColumns);
			
			// Ngramインデックス情報を出力.
			writeNgramIndex(rimOut, params, compressType, body, ngramIndexColumns);
			body = null;

			// 後処理.
			rimOut.close();
			rimOut = null;
			return rowAll;
		} finally {
			if(rimOut != null) {
				try {
					rimOut.close();
				} catch(Exception e) {}
				rimOut = null;
			}
			if(csv != null) {
				try {
					csv.close();
				} catch(Exception e) {}
				csv = null;
			}
		}
	}
	
	// 指定圧縮の初期化処理.
	private static final void initCompress(
		RimParams params, CompressType compressType) {
		// 圧縮タイプが「デフォルト圧縮」の場合.
		if(CompressType.Default == compressType) {
			
			// 属性にSeabassCompのバッファを生成して設定.
			params.attribute = new SeabassCompressBuffer();
		// 圧縮タイプが「GZIP圧縮」の場合.
		} else if(CompressType.Gzip == compressType) {
			
			// 属性にRbbOutputStreamを生成して設定.
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
	}
	
	// CSV内容を読み込んで列群とインデックス群を取得.
	// Body情報は列毎に行情報を管理する.
	private static final ObjectList[] readCsv(CsvReader csv, ColumnType[] typeList,
		ObjectList<IndexColumn> indexColumns, ObjectList<GeoIndexColumn> geoIndexColumns,
		ObjectList<NgramIndexColumn> ngramIndexColumns)
		throws IOException {
		int i;
		List<String> row;
		// 列数を取得.
		final int columnLength = csv.getHeaderSize();
		// 列単位行群のBody情報を生成.
		ObjectList[] columns = new ObjectList[columnLength];
		for(i = 0; i < columnLength; i ++) {
			columns[i] = new ObjectList<Object>(512);
		}
		// csvデータのバイナリ化とIndex情報の抜き出し.
		while(csv.hasNext()) {
			// CSVの次の１行情報を取得.
			row = csv.nextRow();
			// 列単位でループ.
			for(i = 0; i < columnLength; i ++) {
				// 対象列の行情報にCSV列情報の行データを追加.
				columns[i].add(
					typeList[i].convert(row.get(i)));
			}
		}
		return columns;
	}
	
	// 指定したindexを読み込む.
	private static final ObjectList<IndexRow> readIndex(
		ObjectList[] columns, IndexColumn index) {
		int i;
		Object value;
		// インデックス行情報管理情報を取得.
		final ObjectList rows = columns[index.getColumnNo()];
		// 総行数を取得.
		final int rowLength = rows.size();
		final ObjectList<IndexRow> ret =
			new ObjectList<IndexRow>(rowLength);
		for(i = 0; i < rowLength; i ++) {
			// 列情報が存在する場合.
			if((value = rows.get(i)) != null) {
				// インデックス追加.
				ret.add(new IndexGeneralRow(
					(Comparable)value, i));
			}
		}
		// ソート処理.
		if(ret.size() > 0) {
			ret.smart();
			ret.sort();
		}
		return ret;
	}
	
	// 指定したgeoIndexを読み込む.
	private static final ObjectList<IndexRow> readGeoIndex(
		ObjectList[] columns, GeoIndexColumn geo) {
		int i;
		Object lat, lon;
		// Bodyから緯度、経度の列群を取得.
		final ObjectList rowsLat = columns[geo.getLatColumnNo()];
		final ObjectList rowsLon = columns[geo.getLonColumnNo()];
		// 総行数を取得.
		final int rowLength = rowsLat.size();
		final ObjectList<IndexRow> ret =
			new ObjectList<IndexRow>(rowLength);
		for(i = 0; i < rowLength; i ++) {
			// 緯度、経度情報を取得してDouble変換.
			lat = rowsLat.get(i);
			lon = rowsLon.get(i);
			// 緯度・経度情報が存在する場合.
			if(lat != null && lon != null) {
				// インデックス追加.
				ret.add(new IndexGeneralRow(GeoQuad.create(
					(Double)lat, (Double)lon), i));
			}
		}
		// ソート処理.
		if(ret.size() > 0) {
			ret.smart();
			ret.sort();
		}
		return ret;
	}
	
	// 指定したngramIndexを読み込む.
	private static final ObjectList<IndexRow> readNgramIndex(
		ObjectList[] columns, NgramIndexColumn ngram) {
		int i, j;
		int valueLen;
		String value;
		// パースするNgram長を取得.
		final int ngramLength = ngram.getNgramLength();
		// BodyからNgram列群を取得.
		final ObjectList rowsNgram = columns[ngram.getColumnNo()];
		// 総行数を取得.
		final int rowLength = rowsNgram.size();
		final ObjectList<IndexRow> ret =
			new ObjectList<IndexRow>(rowLength);
		for(i = 0; i < rowLength; i ++) {
			// 情報を取得してString取得.
			value = (String)rowsNgram.get(i);
			// 文字列が存在する場合.
			if(value != null) {
				// インデックス追加.
				valueLen = value.length() - (ngramLength - 1);
				// 文字をNgramでパースしてインデックス化.
				for(j = 0; j < valueLen; j ++) {
					// １つのNgram条件を追加.
					ret.add(new IndexNgramRow(
						SearchUtil.getNgramString(value, j, ngramLength),
						i, j, ngramLength));
				}
			}
		}
		// ソート処理.
		if(ret.size() > 0) {
			ret.smart();
			ret.sort();
		}
		return ret;
	}

	// ヘッダ情報を出力.
	private static final void writeHeader(OutputStream out, RimParams params,
		CsvReader csv, ColumnType[] types)
		throws IOException {
		final byte[] tmp = params.tmp;
		final Object[] strBuf = params.strBuf;
		
		// 列数を設定(Saving).
		int len = csv.getHeaderSize();
		BinaryIO.writeSavingBinary(out, tmp, len);

		// 列名を設定(utf8).
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeString(out, tmp, strBuf, csv.getHeader(i));
		}
		
		// 列型を設定(1byte).
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeInt1(out, tmp, types[i].getNo());
		}
	}
	
	// bodyデータを出力.
	private static final void writeBody(OutputStream out, RimParams params,
		ObjectList[] body, ColumnType[] types, CompressType compressType)
		throws IOException {
		ObjectList o;
		final byte[] tmp = params.tmp;
		final int columnLen = types.length;
		final RbbOutputStream rbb = params.rbb;
		for(int i = 0; i < columnLen; i ++) {
			o = body[i];
			rbb.reset();
			switch(types[i]) {
			case Boolean:
				writeBooleanColumns(rbb, tmp, o);
				break;
			case Byte:
				writeByteColumns(rbb, tmp, o);
				break;
			case Short:
				writeShortColumns(rbb, tmp, o);
				break;
			case Integer:
				writeIntegerColumns(rbb, tmp, o);
				break;
			case Long:
				writeLongColumns(rbb, tmp, o);
				break;
			case Float:
				writeFloatColumns(rbb, tmp, o);
				break;
			case Double:
				writeDoubleColumns(rbb, tmp, o);
				break;
			case String:
				writeStringColumns(rbb, tmp, params.strBuf, o);
				break;
			case Date:
				writeDateColumns(rbb, tmp, o);
				break;
			}
			// RbbOutputStreamに書き込んだ情報を出力.
			writeCompress(out, params, compressType);
		}
	}

	// Index群を出力.
	private static final void writeIndex(OutputStream out,
		RimParams params, ColumnType[] types, CompressType compressType,
		ObjectList[] body, ObjectList<IndexColumn> indexColumns)
		throws IOException {
		
		ColumnType type;
		IndexColumn index;
		ObjectList<IndexRow> list;
		
		final byte[] tmp = params.tmp;
		final int byte1_4Len = params.byte1_4Len;
		final int len = indexColumns.size();
		
		// インデックス毎に出力.
		for(int i = 0; i < len; i ++) {
			index = indexColumns.get(i);
			type = types[index.getColumnNo()];
			
			// インデックス情報を読み込む.
			list = readIndex(body, index);
			
			// このIndexを示す列番号を出力(Saving).
			BinaryIO.writeSavingBinary(out, tmp, index.getColumnNo());
			
			// このIndexの総行数を出力(1~4byte).
			BinaryIO.write1_4Binary(out, tmp, byte1_4Len, list.size());
			
			// indexの行群を出力.
			writeIndexRows(out, params, type, compressType, list);
			list = null;
		}
	}
	
	// GeoIndex群を出力.
	private static final void writeGeoIndex(OutputStream out, RimParams params,
		CompressType compressType, ObjectList[] body, ObjectList<GeoIndexColumn> geoIndexColumns)
		throws IOException {
		
		GeoIndexColumn index;
		ObjectList<IndexRow> list;
		
		final byte[] tmp = params.tmp;
		final int len = geoIndexColumns.size();
		
		// インデックス毎に出力.
		for(int i = 0; i < len; i ++) {
			index = geoIndexColumns.get(i);
			
			// インデックス情報を読み込む.
			list = readGeoIndex(body, index);
			
			// 元の緯度情報を示す列番号を出力(Saving).
			BinaryIO.writeSavingBinary(out, tmp, index.getLatColumnNo());
			
			// 元の経度情報を示す列番号を出力(Saving).
			BinaryIO.writeSavingBinary(out, tmp, index.getLonColumnNo());
			
			// このIndexの総行数を出力(1~4byte).
			BinaryIO.write1_4Binary(out, tmp, params.byte1_4Len, list.size());
			
			// indexの行群を出力.
			writeIndexRows(out, params, ColumnType.Long, compressType, list);
			list = null;
		}
	}
	
	// NgramIndex群を出力.
	private static final void writeNgramIndex(OutputStream out, RimParams params,
		CompressType compressType, ObjectList[] body, ObjectList<NgramIndexColumn> ngramIndexColumns)
		throws IOException {
		
		NgramIndexColumn index;
		ObjectList<IndexRow> list;
		
		final byte[] tmp = params.tmp;
		final int len = ngramIndexColumns.size();
		
		// インデックス毎に出力.
		for(int i = 0; i < len; i ++) {
			index = ngramIndexColumns.get(i);
			
			// インデックス情報を読み込む.
			list = readNgramIndex(body, index);
			
			// 列番号を出力(Saving).
			BinaryIO.writeSavingBinary(out, tmp, index.getColumnNo());
			
			// パースするNgram長を出力(byte).
			BinaryIO.writeInt1(out, tmp, index.getNgramLength());
			
			// このIndexの総行数を出力(1~4byte).
			BinaryIO.write1_4Binary(out, tmp, params.byte1_4Len, list.size());
			
			// indexの行群を出力.
			writeIndexRows(out, params, ColumnType.Long, compressType, list);
			list = null;
		}
	}
	
	// indexの行群を書き込む.
	private static final int writeIndexRows(
		OutputStream out, RimParams params, ColumnType type,
		CompressType compressType, ObjectList<IndexRow> list)
		throws IOException {
		
		//
		// ソートされたIndexでは、同一の条件が並んで管理されてる可能性がある
		// ので、それらを以下のように最適化して保存する.
		// <ソートされたIndex情報>
		//  {value: 100, rowId: 1}
		//  {value: 100, rowId: 3}
		//  {value: 100, rowId: 11}
		//  {value: 203, rowId: 5}
		//  {value: 203, rowId: 38}
		//  {value: 210, rowId: 111}
		//
		// <最適化されたIndex情報>
		//  {value: 100, [1, 3, 11]}
		//  {value: 203, [5, 38]}
		//  {value: 210, [111]}
		//
		
		int pos = 0;
		int ret = 0;
		IndexRow row, bef = null;
		final int oneIndexLength = list.size();
		final RbbOutputStream rbb = params.rbb;
		rbb.reset();
		for(int i = 0; i < oneIndexLength; i ++) {
			row = list.get(i);
			// 前回value条件と一致しない場合.
			if(bef != null && !bef.getValue().equals(row.getValue())) {

				// 最適化Index情報を保存.
				ret += optimizeWriteIndex(rbb, params, type, list, pos, i);

				// 現在をポジションとする.
				pos = i;
			}
			// 次の処理での前回情報として保存.
			bef = row;
		}
		
		// 最後の情報を書き込み.
		ret += optimizeWriteIndex(rbb, params, type, list, pos, oneIndexLength);
		
		// rbbの内容を出力.
		writeCompress(out, params, compressType);
		
		return ret;
	}

	// 最適化されたIndex書き込み.
	private static final int optimizeWriteIndex(RbbOutputStream rbb, RimParams params,
		ColumnType type, ObjectList<IndexRow> list,
		int start, int end)
		throws IOException {

		// value情報を出力.
		list.get(start).writeByValue(rbb, params, type);

		// 連続する行数を出力(saving).
		BinaryIO.writeSavingBinary(rbb, params.tmp, end - start);
		
		// 連続する行ID群を出力.
		int ret = 0;
		for(int i = start; i < end; i ++) {
			list.get(i).writeByRowInfo(rbb, params);
			ret ++;
		}
		
		return ret;
	}
	
	// 未圧縮の内容を書き込む.
	private static final void writeNoCompress(OutputStream out, RimParams params)
		throws IOException {
		final RbbOutputStream rbb = params.rbb;
		final int rbbLen = rbb.getLength();
		
		// 圧縮フラグOFF.
		BinaryIO.writeBoolean(out, params.tmp, false);
		// データー長を設定.
		BinaryIO.writeSavingBinary(out, params.tmp, rbbLen);
		// データーを設定.
		out.write(rbb.getRawBuffer(), 0, rbbLen);
	}
	
	// 圧縮条件が存在する場合は圧縮して書き込む.
	private static final void writeCompress(OutputStream out, RimParams params,
		CompressType compressType) throws IOException {
		
		// 圧縮無しの場合.
		if(CompressType.None == compressType) {
			
			// 未圧縮の内容を書き込む.
			writeNoCompress(out, params);
			
		// 圧縮タイプが「デフォルト圧縮」の場合.
		} else if(CompressType.Default == compressType) {
			final RbbOutputStream rbb = params.rbb;
			final int rbbLen = rbb.getLength();
			
			// 圧縮用バッファを取得.
			final SeabassCompressBuffer buf = (SeabassCompressBuffer)params.attribute;
			
			// 圧縮処理を受け取るバッファ長を取得して初期化.
			buf.clearByMaxCompress(rbbLen);
			
			// 圧縮処理.
			SeabassCompress.compress(buf, rbb.getRawBuffer(), 0, rbbLen);
			
			// 圧縮サイズ.
			final int resLen = buf.getLimit();
			
			// 元サイズより圧縮サイズの方が大きい場合.
			if(resLen > rbbLen) {
				
				// 未圧縮の内容を書き込む.
				writeNoCompress(out, params);
				
			// 圧縮サイズの方が小さい場合.
			} else {
				
				// 圧縮フラグON.
				BinaryIO.writeBoolean(out, params.tmp, true);
				// データー長を設定.
				BinaryIO.writeSavingBinary(out, params.tmp, resLen);
				// データーを設定.
				out.write(buf.getRawBuffer(), 0, resLen);
			}
			
		// 圧縮タイプが「GZIP圧縮」の場合.
		} else if(CompressType.Gzip == compressType) {
			final RbbOutputStream rbb = params.rbb;
			final int rbbLen = rbb.getLength();
			
			// 圧縮用バッファを取得.
			RbbOutputStream wrbb = (RbbOutputStream)params.attribute;
			wrbb.reset();
			
			// GZIP圧縮.
			GZIPOutputStream gzip = new GZIPOutputStream(wrbb);
			gzip.write(rbb.getRawBuffer(), 0, rbbLen);
			gzip.flush();
			gzip.finish();
			gzip.close();
			gzip = null;
			
			// 圧縮サイズ.
			final int resLen = wrbb.getLength();
			
			// 元サイズより圧縮サイズの方が大きい場合.
			if(resLen > rbbLen) {
				
				// 未圧縮の内容を書き込む.
				writeNoCompress(out, params);
				
			// 圧縮サイズの方が小さい場合.
			} else {
				
				// 圧縮フラグON.
				BinaryIO.writeBoolean(out, params.tmp, true);
				// データー長を設定.
				BinaryIO.writeSavingBinary(out, params.tmp, resLen);
				// データーを設定.
				out.write(wrbb.getRawBuffer(), 0, resLen);
			}
			
		// 圧縮タイプが「LZ4圧縮」の場合.
		} else if(CompressType.LZ4 == compressType) {
			final RbbOutputStream rbb = params.rbb;
			final int rbbLen = rbb.getLength();
			
			// LZ4オブジェクトを取得.
			final Lz4Compress lz4 = Lz4Compress.getInstance();
			
			// 圧縮用バッファを取得.
			final CompressBuffer oBuf = (CompressBuffer)params.attribute;
			
			// 圧縮処理を受け取るバッファ長を取得して初期化.
			final int oLen = lz4.maxCompressedLength(rbbLen);
			oBuf.clear(oLen);
			
			// 圧縮処理.
			lz4.compress(oBuf, rbb.getRawBuffer(), 0, rbb.getLength());
			
			// 圧縮サイズを取得.
			final int resLen = oBuf.getLimit();
			
			// 元サイズより圧縮サイズの方が大きい場合.
			if(resLen > rbbLen) {
				
				// 未圧縮の内容を書き込む.
				writeNoCompress(out, params);
				
			// 圧縮サイズの方が小さい場合.
			} else {
				
				// 圧縮フラグON.
				BinaryIO.writeBoolean(out, params.tmp, true);
				
				// 圧縮前の元データ長を保存するバイト数を取得.
				int headLen = lz4.writeSrcLengthToByteLength(rbb.getLength());
				
				// データー長を設定.
				BinaryIO.writeSavingBinary(out, params.tmp, resLen + headLen);
				
				// 元のデータサイズを設定.
				lz4.writeSrcLength(params.tmp, 0, rbb.getLength());
				out.write(params.tmp, 0, headLen);
				
				// データーを設定.
				out.write(oBuf.getRawBuffer(), 0, resLen);
			}
		// 圧縮タイプが「Zstd圧縮」の場合.
		} else if(CompressType.Zstd == compressType) {
			final RbbOutputStream rbb = params.rbb;
			final int rbbLen = rbb.getLength();
			
			// optionから圧縮レベルを取得.
			int level = params.option instanceof Number ?
				((Number)params.option).intValue() :
				ZstdCompress.DEFAULT_LEVEL;
			
			// Zstdオブジェクトを取得.
			final ZstdCompress zstd = ZstdCompress.getInstance();
			
			// 圧縮用バッファを取得.
			final CompressBuffer oBuf = (CompressBuffer)params.attribute;
			
			// 圧縮処理.
			zstd.compress(oBuf, rbb.getRawBuffer(), 0, rbbLen, level);
			
			// 元サイズより圧縮サイズの方が大きい場合.
			if(oBuf.getLimit() > rbbLen) {
				
				// 未圧縮の内容を書き込む.
				writeNoCompress(out, params);
				
			// 圧縮サイズの方が小さい場合.
			} else {
				
				// 圧縮フラグON.
				BinaryIO.writeBoolean(out, params.tmp, true);
				// データー長を設定.
				BinaryIO.writeSavingBinary(out, params.tmp, oBuf.getLimit());
				// データーを設定.
				out.write(oBuf.getRawBuffer(), 0, oBuf.getLimit());
			}

			
		// 不明な圧縮タイプ.
		} else {
			throw new RimException(
				"Illegal compression type is set: " + compressType);
		}
	}
	
	// 1つのValueを出力.
	private static final void convertValue(OutputStream out, byte[] tmp, Object[] strBuf,
		ColumnType type, Object value) throws IOException {
		switch(type) {
		case Boolean:
			BinaryIO.writeBoolean(out, tmp, (Boolean)type.convert(value));
			break;
		case Byte:
			BinaryIO.writeInt1(out, tmp, (Byte)type.convert(value));
			break;
		case Short:
			BinaryIO.writeInt2(out, tmp, (Short)type.convert(value));
			break;
		case Integer:
			BinaryIO.writeInt4(out, tmp, (Integer)type.convert(value));
			break;
		case Long:
			BinaryIO.writeLong(out, tmp, (Long)type.convert(value));
			break;
		case Float:
			BinaryIO.writeFloat(out, tmp, (Float)type.convert(value));
			break;
		case Double:
			BinaryIO.writeDouble(out, tmp, (Double)type.convert(value));
			break;
		case String:
			BinaryIO.writeString(out, tmp, strBuf, (String)type.convert(value));
			break;
		case Date:
			BinaryIO.writeDate(out, tmp, (Date)type.convert(value));
			break;
		}
	}
	
	// 列群をBooleanで書き込む.
	private static final void writeBooleanColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeBoolean(out, tmp, (Boolean)o[i]);
		}
	}

	// 列群をByteで書き込む.
	private static final void writeByteColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeInt1(out, tmp, (Byte)o[i]);
		}
	}
	
	// 列群をShortで書き込む.
	private static final void writeShortColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeInt2(out, tmp, (Short)o[i]);
		}
	}

	// 列群をIntegerで書き込む.
	private static final void writeIntegerColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeInt4(out, tmp, (Integer)o[i]);
		}
	}
	
	// 列群をLongで書き込む.
	private static final void writeLongColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeLong(out, tmp, (Long)o[i]);
		}
	}

	// 列群をFloatで書き込む.
	private static final void writeFloatColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeFloat(out, tmp, (Float)o[i]);
		}
	}
	
	// 列群をDoubleで書き込む.
	private static final void writeDoubleColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeDouble(out, tmp, (Double)o[i]);
		}
	}

	// 列群をStringで書き込む.
	private static final void writeStringColumns(OutputStream out, byte[] tmp,
		Object[] strBuf, ObjectList v) throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeString(out, tmp, strBuf, (String)o[i]);
		}
	}
	
	// 列群をDateで書き込む.
	private static final void writeDateColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			BinaryIO.writeDate(out, tmp, (Date)o[i]);
		}
	}
	
	/**
	 * 1つのIndex行情報.
	 */
	private static interface IndexRow<T> extends Comparable<T> {
		/**
		 * インデックス要素を取得.
		 * @return インデックス要素が返却されます.
		 */
		public Comparable getValue();
		
		/**
		 * Value情報をバイナリ出力.
		 * @param rbb 出力先の情報を設定します.5
		 * @param params Rimパラメータを設定します.
		 * @param type 列型を設定します.
		 * @exception IOException I/O例外.
		 */
		public void writeByValue(RbbOutputStream rbb, RimParams params,
			ColumnType type) throws IOException;
		
		/**
		 * 行関連の情報をバイナリ出力.
		 * @param rbb 出力先の情報を設定します.
		 * @param params Rimパラメータを設定します.
		 * @exception IOException I/O例外.
		 */
		public void writeByRowInfo(RbbOutputStream rbb, RimParams params)
			throws IOException;
	}
	
	/**
	 * 1つの基本(Index, GeoIndex)行情報.
	 */
	private static final class IndexGeneralRow
		implements IndexRow<IndexGeneralRow> {
		final Comparable value;
		final int rowId;

		public IndexGeneralRow(Comparable value, int rowId) {
			this.rowId = rowId;
			this.value = value;
		}

		@Override
		public int compareTo(IndexGeneralRow o) {
			return value.compareTo(o.value);
		}

		@Override
		public Comparable getValue() {
			return value;
		}
		
		@Override
		public void writeByValue(RbbOutputStream rbb, RimParams params,
			ColumnType type) throws IOException {
			convertValue(rbb, params.tmp, params.strBuf, type, value);
		}
		
		@Override
		public void writeByRowInfo(RbbOutputStream rbb, RimParams params)
			throws IOException {
			BinaryIO.write1_4Binary(rbb, params.tmp, params.byte1_4Len, rowId);
		}
	}
	
	/**
	 * 1つのNgram行情報.
	 */
	private static final class IndexNgramRow
		implements IndexRow<IndexNgramRow> {
		final long value;
		final int rowId;
		final int position;
		final byte ngramLength;
		
		public IndexNgramRow(long value, int rowId, int position, int ngramLength) {
			this.value = value;
			this.rowId = rowId;
			this.position = position & 0x0000ffff;
			this.ngramLength = (byte)(ngramLength & 0x0000007f);
		}

		@Override
		public int compareTo(IndexNgramRow o) {
			final long n = value - o.value;
			if(n < 0L) {
				return -1;
			} else if(n > 0L) {
				return 1;
			}
			final int r = rowId - o.rowId;
			if(r != 0) {
				return r;
			}
			final int p = position - o.position;
			if(p != 0) {
				return p;
			}
			return 0;
		}

		@Override
		public Comparable getValue() {
			return value;
		}
		
		@Override
		public void writeByValue(RbbOutputStream rbb, RimParams params,
			ColumnType type) throws IOException {
			final byte[] tmp = params.tmp;
			// ngramの条件に従って、ngramLengthの条件で
			// 出力サイズに従って書き込む.
			switch(ngramLength) {
			// unigram.
			case 1:
				BinaryIO.writeInt2(rbb, tmp, value);
				break;
			// bigram.
			case 2:
				BinaryIO.writeInt4(rbb, tmp, value);
				break;
			// trigram.
			case 3:
				tmp[0] = (byte)((value & 0x0000ff0000000000L) >> 40L);
				tmp[1] = (byte)((value & 0x000000ff00000000L) >> 32L);
				tmp[2] = (byte)((value & 0x00000000ff000000L) >> 24L);
				tmp[3] = (byte)((value & 0x0000000000ff0000L) >> 16L);
				tmp[4] = (byte)((value & 0x000000000000ff00L) >> 8L);
				tmp[5] = (byte)( value & 0x00000000000000ffL);
				rbb.write(tmp, 0, 6);
				break;
			}
		}
		
		@Override
		public void writeByRowInfo(RbbOutputStream rbb, RimParams params)
			throws IOException {
			// 行番号を書き込む.
			BinaryIO.write1_4Binary(rbb, params.tmp, params.byte1_4Len, rowId);
			
			// 文字位置情報を書き込む.
			BinaryIO.writeSavingBinary(rbb, params.tmp, position);
		}
	}

	/**
	 * 1つのIndex列を示す内容.
	 */
	private static final class IndexColumn {
		private final int columnNo;

		/**
		 * コンストラクタ.
		 * @param columnNo インデックス対象の列番号を設定します.
		 */
		public IndexColumn(int columnNo) {
			this.columnNo = columnNo;
		}

		/**
		 * インデックス対象の列番号が返却されます.
		 * @return int 列番号が返却されます.
		 */
		public int getColumnNo() {
			return columnNo;
		}
	}
	
	/**
	 * １つのGeoIndex列を示す内容.
	 */
	private static final class GeoIndexColumn {
		private final int latColumnNo;
		private final int lonColumnNo;
		
		/**
		 * コンストラクタ.
		 * @param latColumnNo インデックス対象の緯度列番号を設定します.
		 * @param lonColumnNo インデックス対象の経度列番号を設定します.
		 */
		public GeoIndexColumn(int latColumnNo, int lonColumnNo) {
			this.latColumnNo = latColumnNo;
			this.lonColumnNo = lonColumnNo;
		}

		/**
		 * インデックス対象の緯度列番号が返却されます.
		 * @return int 緯度列番号が返却されます.
		 */
		public int getLatColumnNo() {
			return latColumnNo;
		}
		
		/**
		 * インデックス対象の経度列番号が返却されます.
		 * @return int 経度列番号が返却されます.
		 */
		public int getLonColumnNo() {
			return lonColumnNo;
		}
	}
	
	/**
	 * 1つのNgramIndex列を示す内容.
	 */
	private static final class NgramIndexColumn {
		private final int columnNo;
		private final int ngramLength;

		/**
		 * コンストラクタ.
		 * @param columnNo インデックス対象の列番号を設定します.
		 * @param ngramLength パースするNgram長を設定します.
		 */
		public NgramIndexColumn(int columnNo, int ngramLength) {
			if(ngramLength < RimConstants.MIN_NGRAM_LENGTH) {
				ngramLength = RimConstants.MIN_NGRAM_LENGTH;
			} else if(ngramLength > RimConstants.MAX_NGRAM_LENGTH) {
				ngramLength = RimConstants.MAX_NGRAM_LENGTH;
			}
			this.columnNo = columnNo;
			this.ngramLength = ngramLength;
		}

		/**
		 * インデックス対象の列番号が返却されます.
		 * @return int 列番号が返却されます.
		 */
		public int getColumnNo() {
			return columnNo;
		}
		
		/**
		 * パースするNgram長が返却されます.
		 * @return int パースするNgram長が返却されます.
		 */
		public int getNgramLength() {
			return ngramLength;
		}
	}

	
	// 利用頻度の高いパラメータを１つにまとめた内容.
	private static final class RimParams {
		// 属性.
		Object attribute;
		// テンポラリバッファ.
		byte[] tmp;
		// 文字列テンポラリバッファ.
		Object[] strBuf;
		// 行数に応じた行情報を保持するバイト値.
		int byte1_4Len;
		// 再利用可能なBinaryのOutputStream.
		RbbOutputStream rbb;
		// オプション情報.
		Object option;
	}
}
