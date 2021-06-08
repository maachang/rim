package rim;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import rim.compress.CompressBuffer;
import rim.compress.CompressType;
import rim.compress.Lz4Compress;
import rim.compress.ZstdCompress;
import rim.exception.RimException;
import rim.util.CsvReader;
import rim.util.CsvRow;
import rim.util.ObjectList;
import rim.util.UTF8IO;
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
	/**
	 * 1つのIndex行情報.
	 */
	private static final class IndexRow implements Comparable<IndexRow> {
		final int rowId;
		final Comparable value;

		public IndexRow(int rowId, Comparable value) {
			this.rowId = rowId;
			this.value = value;
		}

		@Override
		public int compareTo(IndexRow o) {
			return value.compareTo(o.value);
		}

		public int getRowId() {
			return rowId;
		}

		public Comparable getValue() {
			return value;
		}
	}

	/**
	 * 1つのIndex列を示す内容.
	 */
	private static final class IndexColumn {
		final int no;
		ObjectList<IndexRow> rows;

		public IndexColumn(int no) {
			this.no = no;
			this.rows = new ObjectList<IndexRow>();
		}

		public int getNo() {
			return no;
		}

		public ObjectList<IndexRow> getRows() {
			return rows;
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
		// 文字列の長さを保持するバイト値.
		int stringHeaderLength;
		// 行数に応じた行情報を保持するバイト値.
		int byte1_4Len;
		// オプション情報.
		Object option;
		// 再利用可能なBinaryのOutputStream.
		RbbOutputStream rbb;
	}

	// 読み込み対象のCSVデーター.
	private CsvReader csv;
	// 出力先のファイル名.
	private String outFileName;
	// 圧縮タイプ.
	private CompressType compressType;
	// 文字の型に対する長さを出力するバイナリ長(1byteから4byte).
	private int stringHeaderLength;
	// オプション情報.
	private Object option;
	// CSV列毎の変換型群.
	private ColumnType[] indexTypes;
	// インデックス列情報群.
	private ObjectList<IndexColumn> indexColumns =
		new ObjectList<IndexColumn>();

	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param outFileName 出力先ファイル名を設定します.
	 */
	public SaveRim(CsvReader csv, String outFileName) {
		this(csv, outFileName, CompressType.None,
			RimConstants.DEFAULT_STRING_HEADER_LENGTH, null);
	}
	
	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param outFileName 出力先ファイル名を設定します.
	 * @param compressType 圧縮タイプを設定します.
	 */
	public SaveRim(CsvReader csv, String outFileName, CompressType compressType) {
		this(csv, outFileName, compressType,
			RimConstants.DEFAULT_STRING_HEADER_LENGTH, null);
	}

	/**
	 * コンストラクタ.
	 * @param csv 読み込み対象のCSVを設定します.
	 * @param outFileName 出力先ファイル名を設定します.
	 * @param compressType 圧縮タイプを設定します.
	 * @param stringHeaderLength 文字列の長さを管理するバイト数を設定します.
	 * @param option オプション情報を設定します.
	 */
	public SaveRim(CsvReader csv, String outFileName, CompressType compressType,
		int stringHeaderLength, Object option) {
		if(compressType == null) {
			compressType = CompressType.None;
		}
		if(stringHeaderLength <= 0) {
			stringHeaderLength = 1;
		} else if(stringHeaderLength > 4) {
			stringHeaderLength = 4;
		}
		this.csv = csv;
		this.outFileName = outFileName;
		this.compressType = compressType;
		this.stringHeaderLength = stringHeaderLength;
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
		outFileName = null;
		indexColumns = null;
	}

	// クローズチェック.
	private final void checkClose() {
		if(csv == null) {
			throw new RimException("It's already closed.");
		}
	}

	// Index型情報を取得.
	private final void getIndexTypes() {
		if(indexTypes != null) {
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
		indexTypes = list;
	}

	/**
	 * インデックス列追加.
	 * @param column
	 * @param type
	 * @return
	 */
	public SaveRim addIndex(String column) {
		checkClose();
		int no;
		// 指定されたインデックスの列名位置を取得.
		if((no = csv.getHeader().search(column)) == -1) {
			throw new RimException("Specified column name does not exist: " + column);
		}
		// CSV定義の列型群を生成.
		getIndexTypes();
		// 列名位置のインデックス情報を作成.
		indexColumns.add(new IndexColumn(no));
		return this;
	}

	/**
	 * インデックスを作成.
	 * @return int 読み込まれたCSVの行数が返却されます.
	 * @throws IOException
	 */
	public int write() throws IOException {
		checkClose();
		OutputStream out = null;
		try {
			// インデックスが１つも設定されていない場合.
			if(indexTypes == null) {
				getIndexTypes();
			}
			// インデックスリストを作成.
			final IndexColumn[] indexList = createIndexList(csv, indexColumns);
			
			// よく使うパラメータをまとめたオブジェクトを作成.
			final RimParams params = new RimParams();
			
			// オプションを設定.
			params.option = this.option;
			
			// stringHeaderLengthをRimParamsにセット.
			params.stringHeaderLength = stringHeaderLength;

			// テンポラリ情報.
			params.tmp = new byte[8];
			params.strBuf = new Object[] {
				new byte[64]
			};
			params.rbb = new RbbOutputStream();
			
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

			// CSVデーターの読み込み.
			ObjectList[] body = readCsv(csv, indexTypes, indexList);
			final int rowAll = body[0].size();

			// 全行数に対する長さ管理をするバイト数を取得.
			params.byte1_4Len = RimUtil.intLengthByByte1_4Length(rowAll);

			// 保存先情報を生成.
			out = new BufferedOutputStream(
				new FileOutputStream(outFileName)
			);

			// シンボルを出力.
			out.write(RimConstants.SIMBOL_BINARY);
			
			// 圧縮タイプを出力(1byte).
			out.write(len1Binary(params.tmp, compressType.getId()), 0, 1);

			// ヘッダ情報を出力.
			writeHeader(out, params, csv, indexTypes);

			// それぞれの文字列を表現する長さを管理するヘッダバイト数を出力
			// (1byte).
			out.write(len1Binary(params.tmp, params.stringHeaderLength), 0, 1);

			// 全行数を書き込む(4byte).
			out.write(len4Binary(params.tmp, rowAll), 0, 4);
			
			// 登録されてるインデックス数を書き込む(2byte).
			out.write(len2Binary(params.tmp, indexColumns.size()), 0, 2);

			// bodyデータを出力.
			writeBody(out, params, body, indexTypes, compressType);
			body = null;

			// インデックス情報を出力.
			writeIndex(out, params, indexTypes, compressType, indexColumns);

			// 後処理.
			out.close();
			out = null;
			close();

			return rowAll;
		} finally {
			if(out != null) {
				try {
					out.close();
				} catch(Exception e) {}
			}
			if(csv != null) {
				try {
					csv.close();
				} catch(Exception e) {}
				csv = null;
				close();
			}
		}
	}

	// 列位置に対するインデックス管理情報を生成.
	private static final IndexColumn[] createIndexList(CsvReader csv, ObjectList<IndexColumn> list) {
		IndexColumn c;
		final IndexColumn[] ret = new IndexColumn[csv.getHeaderSize()];
		final int len = list.size();
		for(int i = 0; i < len; i ++) {
			c = list.get(i);
			ret[c.getNo()] = c;
		}
		return ret;
	}
	
	// CSV内容を読み込んで列群とインデックス群を取得.
	// Body情報は列毎に行情報を管理する.
	private static final ObjectList[] readCsv(CsvReader csv, ColumnType[] typeList,
		IndexColumn[] indexList) throws IOException {
		int i;
		Object o;
		List<String> row;
		int rowId = 0;
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
					o = typeList[i].convert(row.get(i)));
				// 対象項目をIndex化する場合.
				if(indexList[i] != null) {
					// 変換できないものとnullはIndex対象にしない.
					if(o != null) {
						// indexに生成した行情報を追加.
						indexList[i].rows.add(
							new IndexRow(rowId, (Comparable)o));
						o = null;
					}
				}
			}
			rowId ++;
		}
		// インデックス情報のソート処理.
		for(i = 0; i < columnLength; i ++) {
			if(indexList[i] != null) {
				indexList[i].rows.smart();
				indexList[i].rows.sort();
			}
		}
		// 列情報のスマート化.
		for(i = 0; i < columnLength; i ++) {
			columns[i].smart();
		}
		return columns;
	}

	// 1byteのデーターセット.
	private static final byte[] len1Binary(byte[] tmp, int len) {
		tmp[0] = (byte)(len & 0x000000ff);
		return tmp;
	}

	// 2byteのデーターセット.
	private static final byte[] len2Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0x0000ff00) >> 8);
		tmp[1] = (byte) (len & 0x000000ff);
		return tmp;
	}

	// 3byteのデーターセット.
	private static final byte[] len3Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0x00ff0000) >> 16);
		tmp[1] = (byte)((len & 0x0000ff00) >> 8);
		tmp[2] = (byte) (len & 0x000000ff);
		return tmp;
	}
	
	// 4byteのデーターセット.
	private static final byte[] len4Binary(byte[] tmp, int len) {
		tmp[0] = (byte)((len & 0xff000000) >> 24);
		tmp[1] = (byte)((len & 0x00ff0000) >> 16);
		tmp[2] = (byte)((len & 0x0000ff00) >> 8);
		tmp[3] = (byte) (len & 0x000000ff);
		return tmp;
	}

	// 8byteのデーターセット.
	private static final byte[] len8Binary(byte[] tmp, long len) {
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

	// 1byte から 4byte までの条件に対して、データーセット.
	private static final byte[] len1_4Binary(byte[] tmp, int len1_4, int len) {
		switch(len1_4) {
		case 1: return len1Binary(tmp, len);
		case 2: return len2Binary(tmp, len);
		case 3: return len3Binary(tmp, len);
		case 4: return len4Binary(tmp, len);
		default: return len4Binary(tmp, len);
		}
	}
	
	// Booleanオブジェクトを書き込む.
	private static final void writeBoolean(OutputStream out, byte[] tmp, Boolean v)
		throws IOException {
		// valueがnullの場合は０(false)を設定.
		if(v == null) {
			len1Binary(tmp, 0);
		} else {
			len1Binary(tmp, v ? 1 : 0);
		}
		out.write(tmp, 0, 1);
	}

	// Byteオブジェクトを書き込む.
	private static final void writeByte(OutputStream out, byte[] tmp, Byte v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len1Binary(tmp, 0);
		} else {
			len1Binary(tmp, v);
		}
		out.write(tmp, 0, 1);
	}
	
	// Shortオブジェクトを書き込む.
	private static final void writeShort(OutputStream out, byte[] tmp, Short v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len2Binary(tmp, 0);
		} else {
			len2Binary(tmp, v);
		}
		out.write(tmp, 0, 2);
	}

	// Integerオブジェクトを書き込む.
	private static final void writeInteger(OutputStream out, byte[] tmp, Integer v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len4Binary(tmp, 0);
		} else {
			len4Binary(tmp, v);
		}
		out.write(tmp, 0, 4);
	}
	
	// Longオブジェクトを書き込む.
	private static final void writeLong(OutputStream out, byte[] tmp, Long v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, 0L);
		} else {
			len8Binary(tmp, v);
		}
		out.write(tmp, 0, 8);
	}

	// Floatオブジェクトを書き込む.
	private static final void writeFloat(OutputStream out, byte[] tmp, Float v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len4Binary(tmp, Float.floatToIntBits(0f));
		} else {
			len4Binary(tmp, Float.floatToIntBits(v));
		}
		out.write(tmp, 0, 4);
	}
	
	// Doubleオブジェクトを書き込む.
	private static final void writeDouble(OutputStream out, byte[] tmp, Double v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, Double.doubleToLongBits(0d));
		} else {
			len8Binary(tmp, Double.doubleToLongBits(v));
		}
		out.write(tmp, 0, 8);
	}

	// Stringオブジェクトを書き込む.
	private static final void writeString(OutputStream out, byte[] tmp, Object[] strBuf,
		int stringHeaderLength, String v) throws IOException {
		// valueがnullの場合は０文字を設定.
		if(v == null) {
			len4Binary(tmp, 0);
			out.write(tmp, 0, stringHeaderLength);
		} else {
			// 文字列がnullや空の場合も０文字を設定.
			if(v.isEmpty()) {
				len4Binary(tmp, 0);
				out.write(tmp, 0, stringHeaderLength);
			} else {
				byte[] b = RimUtil.getStringByteArray(strBuf, v.length() * 4);
				int len = UTF8IO.encode(b, v);
				// 文字列のバイナリ長を設定.
				out.write(len1_4Binary(tmp, stringHeaderLength, len),
					0, stringHeaderLength);
				// 文字列を設定.
				out.write(b, 0, len);
			}
		}
	}
	
	// Dateオブジェクトを書き込む.
	private static final void writeDate(OutputStream out, byte[] tmp, Date v)
		throws IOException {
		// valueがnullの場合は０を設定.
		if(v == null) {
			len8Binary(tmp, 0L);
		} else {
			len8Binary(tmp, v.getTime());
		}
		out.write(tmp, 0, 8);
	}
	
	// 未圧縮の内容を書き込む.
	private static final void writeNoCompress(OutputStream out, RimParams params)
		throws IOException {
		final RbbOutputStream rbb = params.rbb;
		final int rbbLen = rbb.getLength();
		
		// 圧縮フラグOFF.
		writeBoolean(out, params.tmp, false);
		// データー長を設定.
		out.write(len4Binary(params.tmp, rbbLen), 0, 4);
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
				writeBoolean(out, params.tmp, true);
				// データー長を設定.
				out.write(len4Binary(params.tmp, resLen), 0, 4);
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
				writeBoolean(out, params.tmp, true);
				// データー長を設定.
				out.write(len4Binary(params.tmp, resLen), 0, 4);
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
				writeBoolean(out, params.tmp, true);
				
				// 圧縮前の元データ長を保存するバイト数を取得.
				int headLen = lz4.writeSrcLengthToByteLength(rbb.getLength());
				
				// データー長を設定.
				out.write(len4Binary(params.tmp, resLen + headLen), 0, 4);
				
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
				writeBoolean(out, params.tmp, true);
				// データー長を設定.
				out.write(len4Binary(params.tmp, oBuf.getLimit()), 0, 4);
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
		int stringHeaderLength, ColumnType type, Object value) throws IOException {
		switch(type) {
		case Boolean:
			writeBoolean(out, tmp, (Boolean)type.convert(value));
			break;
		case Byte:
			writeByte(out, tmp, (Byte)type.convert(value));
			break;
		case Short:
			writeShort(out, tmp, (Short)type.convert(value));
			break;
		case Integer:
			writeInteger(out, tmp, (Integer)type.convert(value));
			break;
		case Long:
			writeLong(out, tmp, (Long)type.convert(value));
			break;
		case Float:
			writeFloat(out, tmp, (Float)type.convert(value));
			break;
		case Double:
			writeDouble(out, tmp, (Double)type.convert(value));
			break;
		case String:
			writeString(out, tmp, strBuf, stringHeaderLength,
				(String)type.convert(value));
			break;
		case Date:
			writeDate(out, tmp, (Date)type.convert(value));
			break;
		}
	}

	// ヘッダ情報を出力.
	private static final void writeHeader(OutputStream out, RimParams params,
		CsvReader csv, ColumnType[] types)
		throws IOException {
		final byte[] tmp = params.tmp;
		
		// 列数を設定(2byte).
		int len = csv.getHeaderSize();
		out.write(len2Binary(tmp, len), 0, 2);

		// 列名を設定(len:1byte, utf8).
		String name;
		for(int i = 0; i < len; i ++) {
			name = csv.getHeader(i);
			out.write(len1Binary(tmp, name.length()), 0, 1);
			out.write(name.getBytes("UTF8"));
		}
		
		// 列型を設定(1byte).
		for(int i = 0; i < len; i ++) {
			out.write(len1Binary(tmp, types[i].getNo()), 0, 1);
		}
	}
	
	// 列群をBooleanで書き込む.
	private static final void writeBooleanColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeBoolean(out, tmp, (Boolean)o[i]);
		}
	}

	// 列群をByteで書き込む.
	private static final void writeByteColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeByte(out, tmp, (Byte)o[i]);
		}
	}
	
	// 列群をShortで書き込む.
	private static final void writeShortColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeShort(out, tmp, (Short)o[i]);
		}
	}

	// 列群をIntegerで書き込む.
	private static final void writeIntegerColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeInteger(out, tmp, (Integer)o[i]);
		}
	}
	
	// 列群をLongで書き込む.
	private static final void writeLongColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeLong(out, tmp, (Long)o[i]);
		}
	}

	// 列群をFloatで書き込む.
	private static final void writeFloatColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeFloat(out, tmp, (Float)o[i]);
		}
	}
	
	// 列群をDoubleで書き込む.
	private static final void writeDoubleColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeDouble(out, tmp, (Double)o[i]);
		}
	}

	// 列群をStringで書き込む.
	private static final void writeStringColumns(OutputStream out, byte[] tmp, Object[] strBuf,
		int stringHeaderLength, ObjectList v) throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeString(out, tmp, strBuf, stringHeaderLength, (String)o[i]);
		}
	}
	
	// 列群をDateで書き込む.
	private static final void writeDateColumns(OutputStream out, byte[] tmp, ObjectList v)
		throws IOException {
		final int len = v.size();
		final Object[] o = v.rawArray();
		for(int i = 0; i < len; i ++) {
			writeDate(out, tmp, (Date)o[i]);
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
			o = body[i]; body[i] = null;
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
				writeStringColumns(rbb, tmp, params.strBuf,
					params.stringHeaderLength, o);
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
	private static final void writeIndex(OutputStream out, RimParams params, ColumnType[] types,
		CompressType compressType, ObjectList<IndexColumn> indexColumns) throws IOException {
		final int len = indexColumns.size();

		// インデックス毎に出力.
		for(int i = 0; i < len; i ++) {
			writeOneIndex(out, params, types[indexColumns.get(i).getNo()], compressType,
				indexColumns.get(i));
		}
	}

	// １つのIndexを出力.
	private static final int writeOneIndex(OutputStream out, RimParams params, ColumnType type,
		CompressType compressType, IndexColumn index) throws IOException {

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
		final byte[] tmp = params.tmp;
		final int byte1_4Len = params.byte1_4Len;

		IndexRow row, bef = null;
		final ObjectList<IndexRow> list = index.getRows();
		final int oneIndexLength = list.size();

		// このIndexを示す列番号を出力(2byte).
		out.write(len2Binary(tmp, index.getNo()), 0, 2);

		// このIndexの総行数を出力.
		out.write(len1_4Binary(tmp, byte1_4Len, oneIndexLength), 0, byte1_4Len);

		// インデックス内容を出力.
		int pos = 0;
		int ret = 0;
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

		final byte[] tmp = params.tmp;
		final Object[] strBuf = params.strBuf;
		final int stringHeaderLength = params.stringHeaderLength;
		final int byte1_4Len = params.byte1_4Len;
		
		// value情報を出力.
		convertValue(rbb, tmp, strBuf, stringHeaderLength, type,
			list.get(start).getValue());

		// 連続する行数を出力.
		rbb.write(len1_4Binary(tmp, byte1_4Len, end - start), 0, byte1_4Len);
		
		// 連続する行ID群を出力.
		int ret = 0;
		for(int i = start; i < end; i ++) {
			rbb.write(len1_4Binary(tmp, byte1_4Len, list.get(i).getRowId()),
				0, byte1_4Len);
			ret ++;
		}
		
		return ret;
	}
}
