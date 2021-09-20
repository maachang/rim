package rim;

import rim.compress.CompressType;
import rim.core.ColumnType;
import rim.exception.RimException;
import rim.util.ObjectList;

/**
 * Rim情報.
 */
public class RimInfo {
	// 総行数.
	private int rowLength;
	
	// 圧縮タイプ.
	private CompressType compressType;
	
	// 基本インデックス情報群.
	private final ObjectList<GeneralIndexInfo> generalIndexInfos =
		new ObjectList<GeneralIndexInfo>();
	
	// 位置情報インデックス情報群.
	private final ObjectList<GeoIndexInfo> geoIndexInfos =
		new ObjectList<GeoIndexInfo>();
	
	// Ngramインデックス情報群.
	private final ObjectList<NgramIndexInfo> ngramIndexInfos =
		new ObjectList<NgramIndexInfo>();
	
	// fixフラグ.
	private boolean fixFlag;
	
	/**
	 * コンストラクタ
	 * @param rowLength 総行数を設定します.
	 * @param compressType 圧縮タイプを設定します.
	 */
	protected RimInfo(int rowLength, CompressType compressType) {
		this.rowLength = rowLength;
		this.compressType = compressType;
		this.fixFlag = false;
	}
	
	/**
	 * GeneralIndex情報を登録.
	 * @param columnType 列タイプを設定します.
	 * @param columnName 列名を設定します.
	 */
	protected void addGeneralIndex(ColumnType columnType, String columnName) {
		generalIndexInfos.add(new GeneralIndexInfo(columnType, columnName));
	}
	
	/**
	 * GeoIndexを情報登録.
	 * @param latColumn 緯度列名を設定します.
	 * @param lonColumn 経度列名を設定します.
	 */
	protected void addGeoIndex(String latColumn, String lonColumn) {
		geoIndexInfos.add(new GeoIndexInfo(latColumn, lonColumn));
	}
	
	/**
	 * NgramIndex情報を登録.
	 * @param columnName 列名を設定します.
	 * @param ngramLength Ngram長を設定します.
	 */
	protected void addNgramIndex(String columnName, int ngramLength) {
		ngramIndexInfos.add(new NgramIndexInfo(columnName, ngramLength));
	}
	
	/**
	 * Fix処理.
	 */
	protected void fix() {
		if(fixFlag) {
			return;
		}
		fixFlag = true;
	}
	
	/**
	 * 追加処理がFixしているか取得.
	 * @return boolean trueの場合Fixしています.
	 */
	protected boolean isFix() {
		return fixFlag;
	}

	/**
	 * Rimデーター読み込みが完了しているかチェック.
	 */
	protected void checkFix() {
		if(!isFix()) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
	}
	
	/**
	 * 総行数を取得.
	 * @return int 総行数が返却されます.
	 */
	public int getRowLength() {
		checkFix();
		return rowLength;
	}

	/**
	 * 圧縮タイプを取得.
	 * @return CompressType 圧縮タイプが返却されます.
	 */
	public CompressType getCompressType() {
		checkFix();
		return compressType;
	}

	/**
	 * 登録されているGeneralIndex数を取得.
	 * @return int 特録されているGeneralIndex数が返却されます.
	 */
	public int getGeneralIndexLength() {
		checkFix();
		return generalIndexInfos.size();
	}
	
	/**
	 * 項番を指定してGeneralIndex情報を取得.
	 * @param no 項番を指定します.
	 * @return GeneralIndexInfo GeneralIndex情報が返却されます.
	 */
	public GeneralIndexInfo getGeneralIndex(int no) {
		checkFix();
		if(no < 0 || no >= generalIndexInfos.size()) {
			throw new RimException(
				"The specified position is out of range(no: " +
				no + " max: " + generalIndexInfos.size() + ")");
		}
		return generalIndexInfos.get(no);
	}

	/**
	 * 登録されているGeoIndex数を取得.
	 * @return int 特録されているGeoIndex数が返却されます.
	 */
	public int getGeoIndexLength() {
		checkFix();
		return geoIndexInfos.size();
	}
	
	/**
	 * 項番を指定してGeoIndex情報を取得.
	 * @param no 項番を指定します.
	 * @return GeoIndexInfo GeoIndex情報が返却されます.
	 */
	public GeoIndexInfo getGeoIndex(int no) {
		checkFix();
		if(no < 0 || no >= geoIndexInfos.size()) {
			throw new RimException(
				"The specified position is out of range(no: " +
				no + " max: " + geoIndexInfos.size() + ")");
		}
		return geoIndexInfos.get(no);
	}

	/**
	 * 登録されているNgramIndex数を取得.
	 * @return int 特録されているNgramIndex数が返却されます.
	 */
	public int getNgramIndexLength() {
		checkFix();
		return ngramIndexInfos.size();
	}
	
	/**
	 * 項番を指定してNgramIndex情報を取得.
	 * @param no 項番を指定します.
	 * @return NgramIndexInfo GeoIndex情報が返却されます.
	 */
	public NgramIndexInfo getNgramIndex(int no) {
		checkFix();
		if(no < 0 || no >= ngramIndexInfos.size()) {
			throw new RimException(
				"The specified position is out of range(no: " +
				no + " max: " + ngramIndexInfos.size() + ")");
		}
		return ngramIndexInfos.get(no);
	}

	/**
	 * この情報を文字列で出力.
	 * @param space 行開始スペース長を設定します.
	 * @return String 文字列が返却されます.
	 */
	public String toString(int space) {
		if(!isFix()) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		toSpace(buf, space).
			append("rowLength: ").append(rowLength).append("\n");
		toSpace(buf, space).
			append("compressType: ").append(compressType).append("\n");
		
		int len;
		
		len = generalIndexInfos.size();
		if(len > 0) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("generalIndex: Length ").append(len);
			for(int i = 0; i < len; i ++) {
				generalIndexInfos.get(i).toString(buf, i, space + 2);
			}
		}
		
		len = geoIndexInfos.size();
		if(len > 0) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("geoIndex: Length ").append(len);
			for(int i = 0; i < len; i ++) {
				geoIndexInfos.get(i).toString(buf, i, space + 2);
			}
		}
		
		len = ngramIndexInfos.size();
		if(len > 0) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("ngramIndex: Length ").append(len);
			for(int i = 0; i < len; i ++) {
				ngramIndexInfos.get(i).toString(buf, i, space + 2);
			}
		}
		return buf.toString();
	}
	
	@Override
	public String toString() {
		return toString(0);
	}
	
	// スペースを取得.
	private static final StringBuilder toSpace(StringBuilder buf, int space) {
		if(space > 0) {
			for(int i = 0; i < space; i ++) {
				buf.append(" ");
			}
		}
		return buf;
	}

	/**
	 * 基本インデックス情報.
	 */
	public static final class GeneralIndexInfo {
		private final ColumnType columnType;
		private final String columnName;
		/**
		 * コンストラクタ.
		 * @param columnType 列タイプを設定します.
		 * @param columnName 列名を設定します.
		 */
		private GeneralIndexInfo(ColumnType columnType, String columnName) {
			this.columnType = columnType;
			this.columnName = columnName;
		}
		
		/**
		 * インデックス列タイプを取得.
		 * @return ColumnType 列タイプが返却されます.
		 */
		public ColumnType getColumnType() {
			return columnType;
		}
		
		/**
		 * インデックスの列名を取得.
		 * @return String 列名が返却されます.
		 */
		public String getColumnName() {
			return columnName;
		}
		
		/**
		 * 文字列変換.
		 * @param buf StringBuilderを設定します.
		 * @param no 対象の項番を設定します.
		 * @param space 先頭スペース数を設定します.
		 */
		protected void toString(StringBuilder buf, int no, int space) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("[general] ").append(no + 1).
				append(" column: ").append(columnName).
				append(", columnType: ").append(columnType);
		}

	}
	
	/**
	 * 位置情報インデックス情報.
	 */
	public static final class GeoIndexInfo {
		private final String latColumn;
		private final String lonColumn;
		
		/**
		 * コンストラクタ.
		 * @param latColumn 緯度列名を設定します.
		 * @param lonColumn 経度列名を設定します.
		 */
		private GeoIndexInfo(String latColumn, String lonColumn) {
			this.latColumn = latColumn;
			this.lonColumn = lonColumn;
		}
		
		/**
		 * インデックス列タイプを取得.
		 * @return ColumnType 列タイプが返却されます.
		 */
		public ColumnType getColumnType() {
			return ColumnType.Long;
		}
		
		/**
		 * 緯度インデックスの列名を取得.
		 * @return String 緯度列名が返却されます.
		 */
		public String getLatColumn() {
			return latColumn;
		}
		
		/**
		 * 経度インデックスの列名を取得.
		 * @return String 経度列名が返却されます.
		 */
		public String getlonColumn() {
			return lonColumn;
		}
		
		/**
		 * 文字列変換.
		 * @param buf StringBuilderを設定します.
		 * @param no 対象の項番を設定します.
		 * @param space 先頭スペース数を設定します.
		 */
		protected void toString(StringBuilder buf, int no, int space) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("[geo] ").append(no + 1).
				append(" latitude: ").append(latColumn).
				append(", longitude: ").append(lonColumn);
		}
	}
	
	/**
	 * Ngramインデックス.
	 */
	public static final class NgramIndexInfo {
		private final String columnName;
		private final int ngramLength;
		
		/**
		 * コンストラクタ.
		 * @param columnName 列名を設定します.
		 * @param ngramLength Ngram長を設定します.
		 */
		private NgramIndexInfo(String columnName, int ngramLength) {
			this.columnName = columnName;
			this.ngramLength = ngramLength;
		}
		
		/**
		 * インデックス列タイプを取得.
		 * @return ColumnType 列タイプが返却されます.
		 */
		public ColumnType getColumnType() {
			return ColumnType.String;
		}
		
		/**
		 * インデックスの列名を取得.
		 * @return String 列名が返却されます.
		 */
		public String getColumnName() {
			return columnName;
		}
		
		/**
		 * Ngram長を取得.
		 * @return int Ngram長が返却されます.
		 */
		public int getNgramLength() {
			return ngramLength;
		}
		
		/**
		 * Ngramタイプを取得.
		 * @return String NgramLength が 1 の場合 unigram が返却されます.
		 *                NgramLength が 2 の場合 bigram が返却されます.
		 *                NgramLength が 3 の場合 trigram が返却されます.
		 */
		public String getNgramType() {
			switch(ngramLength) {
			case 1: return "unigram";
			case 2: return "bigram";
			}
			return "trigram";
		}
		
		/**
		 * 文字列変換.
		 * @param buf StringBuilderを設定します.
		 * @param no 対象の項番を設定します.
		 * @param space 先頭スペース数を設定します.
		 */
		protected void toString(StringBuilder buf, int no, int space) {
			if(buf.length() > 0) {
				buf.append("\n");
			}
			toSpace(buf, space).
				append("[ngram] ").append(no + 1).
				append(" column: ").append(columnName).
				append(", ngram: ").append(getNgramType()).
				append("(").append(ngramLength).append(")");
		}
	}
}
