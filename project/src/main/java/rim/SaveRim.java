package rim;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import rim.exception.RimException;
import rim.util.BinaryBuffer;
import rim.util.CsvReader;
import rim.util.CsvRow;
import rim.util.ObjectList;

/**
 * CSVファイルから、rim(readInMemory)データーを作成.
 *
 * 指定された列を型指定して、インデックス化します.
 * これにより、CSVデータを高速にインデックス検索が可能です.
 */
public class SaveRim {
	/**
	 * 1つのIndex行情報.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final class IndexRow implements Comparable<IndexRow> {
		final int rowId;
		final Comparable value;

		public IndexRow(int rowId, Comparable<?> value) {
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

	// 読み込み対象のCSVデーター.
	private CsvReader csv;
	// 出力先のファイル名.
	private String outFileName;
	// 文字の型に対する長さを出力するバイナリ長(1byteから4byte).
	private int stringHeaderLength;
	// CSV列毎の変換型群.
	private ColumnType[] indexTypes;
	// インデックス列情報群.
	private ObjectList<IndexColumn> indexColumns =
		new ObjectList<IndexColumn>();

	/**
	 * コンストラクタ.
	 * @param csv
	 * @param outFileName
	 */
	public SaveRim(CsvReader csv, String outFileName) {
		this(csv, outFileName, RimConstants.DEFAULT_STRING_HEADER_LENGTH);
	}

	/**
	 * コンストラクタ.
	 * @param csv
	 * @param outFileName
	 * @param stringHeaderLength
	 */
	public SaveRim(CsvReader csv, String outFileName, int stringHeaderLength) {
		if(stringHeaderLength <= 0) {
			stringHeaderLength = 1;
		} else if(stringHeaderLength > 4) {
			stringHeaderLength = 4;
		}
		this.csv = csv;
		this.outFileName = outFileName;
		this.stringHeaderLength = stringHeaderLength;
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
			IndexColumn[] indexList = createIndexList(csv, indexColumns);

			// 増加可能なバイナリバッファ.
			BinaryBuffer buf = new BinaryBuffer();

			// テンポラリバイナリ.
			final byte[] tmp = new byte[8];

			// CSVデーターのロード.
			final int rowAll = loadCsv(buf, stringHeaderLength, tmp, csv,
				indexTypes, indexList);

			// 全行数に対する長さ管理をするバイト数を取得.
			final int byte1_4Len = RimUtil.intLengthByByte1_4Length(rowAll);

			// 保存先情報を生成.
			out = new BufferedOutputStream(
				new FileOutputStream(outFileName)
			);

			// シンボルを出力.
			out.write(RimConstants.SIMBOL_BINARY);

			// ヘッダ情報を出力.
			writeHeader(out, tmp, csv, indexTypes);

			// それぞれの文字列を表現する長さを管理するヘッダバイト数を出力
			// (1byte).
			out.write(len1Binary(tmp, stringHeaderLength), 0, 1);

			// 全行数を書き込む(4byte).
			out.write(len4Binary(tmp, rowAll), 0, 4);
			
			// 登録されてるインデックス数を書き込む(2byte).
			out.write(len2Binary(tmp, indexColumns.size()), 0, 2);

			// bodyデータを出力.
			writeBody(out, tmp, buf);
			buf = null;

			// インデックス情報を出力.
			writeIndex(out, tmp, stringHeaderLength, byte1_4Len,
				indexTypes, indexColumns);

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

	// CSVデータを読み込んでインデックスを作成する.
	@SuppressWarnings("rawtypes")
	private static final int loadCsv(BinaryBuffer body, int stringHeaderLength,
		byte[] tmp, CsvReader csv, ColumnType[] typeList, IndexColumn[] indexList)
			throws IOException {
		Object o;
		String e;
		int i;
		int rowId = 0;
		List<String> row;
		final int headerLength = csv.getHeaderSize();
		OutputStream bout = body.getOutputStream();
		try {
			// csvデータのバイナリ化とIndex情報の抜き出し.
			while(csv.hasNext()) {
				row = csv.nextRow();
				for(i = 0; i < headerLength; i ++) {
					// １つの要素をBodyに出力.
					convertValue(bout, tmp, stringHeaderLength, typeList[i],
						e = row.get(i));
					// 対象項目をIndex化する場合.
					if(indexList[i] != null) {
						// 変換できないものとnullはIndex対象にしない.
						o = typeList[i].convert(e);
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
			for(i = 0; i < headerLength; i ++) {
				if(indexList[i] != null) {
					indexList[i].rows.sort();
				}
			}
			bout.close();
			bout = null;
			return rowId;
		} finally {
			if(bout != null) {
				try {
					bout.close();
				} catch(Exception ee) {}
			}
		}
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
	private static final void writeString(OutputStream out, byte[] tmp, int stringHeaderLength,
		String v) throws IOException {
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
				final byte[] b = v.getBytes("UTF8");
				int len = b.length;
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
	
	// 1つのValueを出力.
	private static final void convertValue(OutputStream out, byte[] tmp, int stringHeaderLength,
		ColumnType type, Object value) throws IOException {
		switch(type) {
			case Boolean: {
				writeBoolean(out, tmp, (Boolean)type.convert(value));
				break;
			}
			case Byte: {
				writeByte(out, tmp, (Byte)type.convert(value));
				break;
			}
			case Short: {
				writeShort(out, tmp, (Short)type.convert(value));
				break;
			}
			case Integer: {
				writeInteger(out, tmp, (Integer)type.convert(value));
				break;
			}
			case Long: {
				writeLong(out, tmp, (Long)type.convert(value));
				break;
			}
			case Float: {
				writeFloat(out, tmp, (Float)type.convert(value));
				break;
			}
			case Double: {
				writeDouble(out, tmp, (Double)type.convert(value));
				break;
			}
			case String: {
				writeString(out, tmp, stringHeaderLength,
					(String)type.convert(value));
				break;
			}
			case Date: {
				writeDate(out, tmp, (Date)type.convert(value));
				break;
			}
		}
	}

	// ヘッダ情報を出力.
	private static final void writeHeader(OutputStream out, byte[] tmp, CsvReader csv,
		ColumnType[] types)
		throws IOException {
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

	// bodyデータを出力.
	private static final void writeBody(OutputStream out, byte[] tmp, BinaryBuffer body)
		throws IOException {

		// bodyデーターのバイナリを書き込み.
		out.write(body.toByteArray());

		// bodyをClose.
		body.close();
	}

	// Index群を出力.
	private static final void writeIndex(OutputStream out, byte[] tmp, int stringHeaderLength,
		int byte1_4Len, ColumnType[] types, ObjectList<IndexColumn> indexColumns)
		throws IOException {
		final int len = indexColumns.size();

		// インデックス毎に出力.
		for(int i = 0; i < len; i ++) {
			writeOneIndex(out, tmp, stringHeaderLength, byte1_4Len,
				types[indexColumns.get(i).getNo()], indexColumns.get(i));
		}
	}

	// １つのIndexを出力.
	private static final int writeOneIndex(OutputStream out, byte[] tmp, int stringHeaderLength,
		int byte1_4Len, ColumnType type, IndexColumn index)
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

		IndexRow row, bef = null;
		final ObjectList<IndexRow> list = index.getRows();
		final int len = list.size();

		// このIndexを示す列番号を出力(2byte).
		out.write(len2Binary(tmp, index.getNo()), 0, 2);

		// このIndexの総行数を出力.
		out.write(len1_4Binary(tmp, byte1_4Len, len), 0, byte1_4Len);

		// インデックス内容を出力.
		int pos = 0;
		int ret = 0;
		for(int i = 0; i < len; i ++) {
			row = list.get(i);
			// 前回value条件と一致しない場合.
			if(bef != null && !bef.getValue().equals(row.getValue())) {

				// 最適化Index情報を保存.
				ret += optimizeWriteIndex(out, tmp, stringHeaderLength, byte1_4Len,
					type, list, pos, i);

				// 現在をポジションとする.
				pos = i;
			}
			// 次の処理での前回情報として保存.
			bef = row;
		}
		
		// 最後の情報を書き込み.
		ret += optimizeWriteIndex(out, tmp, stringHeaderLength, byte1_4Len,
			type, list, pos, len);
		return ret;
	}

	// 最適化されたIndex書き込み.
	private static final int optimizeWriteIndex(OutputStream out, byte[] tmp, int stringHeaderLength,
		int byte1_4Len, ColumnType type, ObjectList<IndexRow> list, int start, int end)
		throws IOException {

		// value情報を出力.
		convertValue(out, tmp, stringHeaderLength, type, list.get(start).getValue());

		// 連続する行数を出力.
		out.write(len1_4Binary(tmp, byte1_4Len, end - start), 0, byte1_4Len);

		// 連続する行ID群を出力.
		int ret = 0;
		for(int i = start; i < end; i ++) {
			out.write(len1_4Binary(tmp, byte1_4Len, list.get(i).getRowId()),
				0, byte1_4Len);
			ret ++;
		}
		return ret;
	}
}
