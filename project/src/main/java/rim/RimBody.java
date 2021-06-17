package rim;

import java.util.Arrays;
import java.util.NoSuchElementException;

import rim.core.ColumnType;
import rim.core.LikeAnalysis;
import rim.core.LikeParser;
import rim.core.SearchUtil;
import rim.exception.RimException;
import rim.index.RimGeoIndex;
import rim.index.RimIndex;
import rim.util.FixedSearchArray;
import rim.util.ObjectList;
import rim.util.TypesUtil;

/**
 * Rim-Bodyデーター.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RimBody {

	// 列名群.
	private FixedSearchArray<String> columns;
	
	// 列数.
	private int columnLength;
	
	// 行数.
	private int rowLength;

	// 列型群.
	private ColumnType[] columnTypes;

	// 行リスト.
	private Object[] rows;

	// 行情報.
	private RimRow rimRow;
	
	// 設定確認列群.
	private boolean[] settingRows;
	
	// fixフラグ.
	private boolean fixFlag = false;

	/**
	 * コンストラクタ.
	 * @param columns 列名群を設定します.
	 * @param types 列タイプを設定します.
	 * @param rowLength 総行数を設定します.
	 */
	protected RimBody(String[] columns, ColumnType[] types, int rowLength) {
		final FixedSearchArray<String> h = new FixedSearchArray<String>(columns);
		final int columnLength = columns.length;
		final boolean[] f = new boolean[columnLength];
		final Object[] r = new Object[rowLength];
		for(int i = 0; i < rowLength; i ++) {
			r[i] = new Object[columnLength];
		}
		Arrays.fill(f, false);
		this.columns = h;
		this.columnLength = columnLength;
		this.rowLength = rowLength;
		this.columnTypes = types;
		this.rows = r;
		this.rimRow = new RimRow(h, types);
		this.settingRows = f;
		this.fixFlag = false;
	}
	
	/**
	 * 指定列番号に対して列情報を設定.
	 * @param columnNo 列番号を設定します.
	 * @param columns １つの列の行群情報を設定します.
	 */
	protected void setColumns(int columnNo, ObjectList columns) {
		if(columns == null) {
			throw new RimException("No columns have been set.");
		} else if(columns.size() != rows.length) {
			throw new RimException("The number of rows (" +
				columns.size() + ") in the setting column information does " +
				"not match the number of defined rows: " + rows.length);
		}
		setColumns(columnNo, columns.rawArray());
	}
	
	/**
	 * 指定列番号に対して列情報を設定.
	 * @param columnNo 列番号を設定します.
	 * @param columns １つの列の行群情報を設定します.
	 */
	protected void setColumns(int columnNo, Object[] columns) {
		checkNoFix();
		if(columnNo < 0 || columnNo >= columnLength) {
			throw new RimException("The specified column number (" +
				columnNo + ") is beyond the scope of the column definition: " + columnLength);
		} else if(columns == null || columns.length == 0) {
			throw new RimException("No columns have been set.");
		} else if(columns.length < rowLength) {
			throw new RimException("The number of rows (" +
				columns.length + ") in the setting column information does " +
				"not match the number of defined rows: " + rowLength);
		}
		
		// 列情報を追加.
		final Object[] rs = rows;
		for(int i = 0; i < rowLength; i ++) {
			((Object[])rs[i])[columnNo] = columns[i];
		}
		// 設定した列に対してフラグをON.
		settingRows[columnNo] = true;
	}
	
	/**
	 * インデックスの作成.
	 * @param columnNo 列番号を設定します.
	 * @param planIndexSize このインデックスの予定長を設定します.
	 * @return RimIndex 空のRimIndexが返却されます.
	 */
	protected final RimIndex createIndex(int columnNo, int planIndexSize) {
		checkNoFix();
		return new RimIndex(
			this,
			columnNo,
			planIndexSize);
	}
	
	/**
	 * Geoインデックスの作成.
	 * @param latColumnNo 緯度列番号を設定します.
	 * @param lonColumnNo 経度列番号を設定します.
	 * @param planIndexSize このインデックスの予定長を設定します.
	 * @return RimGeoIndex 空のRimGeoIndexが返却されます.
	 */
	protected final RimGeoIndex createGeoIndex(int latColumnNo, int lonColumnNo,
		int planIndexSize) {
		checkNoFix();
		return new RimGeoIndex(
			this,
			latColumnNo,
			lonColumnNo,
			planIndexSize);
	}

	
	/**
	 * Bodyの追加処理がすべて完了した場合に呼び出します.
	 */
	protected void fix() {
		if(fixFlag) {
			return;
		}
		// 設定が確定されていない列情報をチェック.
		for(int i = 0; i < columnLength; i ++) {
			if(!settingRows[i]) {
				throw new RimException(
					"The column number (" + i +
					") data is not set.");
			}
		}
		fixFlag = true;
	}

	/**
	 * 行データがすべて追加されたかチェック.
	 * @return boolean true の場合すべて追加されてます.
	 */
	public boolean isFix() {
		return fixFlag;
	}
	
	/**
	 * Body設定が完了している場合例外.
	 */
	protected void checkNoFix() {
		if(isFix()) {
			throw new RimException(
				"It cannot be processed because the Body is fixed.");
		}
	}

	/**
	 * Body設定が完了していない場合例外.
	 */
	protected void checkFix() {
		if(!isFix()) {
			throw new RimException(
				"The row information has not been set up to the" +
				" specified conditions.");
		}
	}

	/**
	 * 列位置を取得.
	 * @param columns 列名を管理しているFiexedSearchArrayを設定します.
	 * @param column 列名を設定します.
	 *               数字を設定した場合、直接列項番で取得します.
	 * @return int 列位置が返却されます.
	 */
	protected static final int getColumnPos(
		FixedSearchArray<String> columns, Object column) {
		Integer n;
		if (column == null) {
			return -1;
		}
		// 数値だった場合は、番号で処理.
		else if ((n = TypesUtil.getInteger(column)) != null) {
			if (n >= 0 && n < columns.size()) {
				return n;
			}
			return -1;
		}
		return columns.search(column.toString());
	}

	/**
	 * 行番号を設定して行情報を取得.
	 * @param rowId 行番号を設定します.
	 * @return RimRow 行情報が返却されます.
	 */
	public RimRow getRow(int rowId) {
		checkFix();
		if(rowId < 0 || rowId >= rowLength) {
			throw new RimException("Line numbers are out of range.");
		}
		return rimRow.set(rowId, (Object[])rows[rowId]);
	}
	
	/**
	 * 全行情報を取得.
	 * @return Object[] 全行情報が返却されます.
	 */
	protected Object[] getRows() {
		return rows;
	}

	/**
	 * 列名を設定して列型を取得.
	 * @param column 列名を設定します.
	 *               数字を設定した場合、直接列項番で取得します.
	 * @return ColumnType 列型が返却されます.
	 */
	public ColumnType getColumnType(Object column) {
		final int pos = getColumnPos(columns, column);
		if(pos == -1) {
			throw new RimException(
				"The specified column condition does not exist or is out of range: "
				+ column );
		}
		return columnTypes[pos];
	}
	
	/**
	 * 指定列名を設定して、列番号を取得.
	 * @param name 列名を設定します.
	 * @return int 列番号が返却されます.
	 *             存在しない場合は-1.
	 */
	public int getColumnNo(String name) {
		return columns.search(name);
	}
	
	/**
	 * 指定列番号を設定して、列名を取得.
	 * @param no 列番号を設定します.
	 * @return String 列名が返却されます.
	 */
	public String getColumnName(int no) {
		return columns.get(no);
	}
	
	/**
	 * 列数を取得.
	 * @return int 列数が返却されます.
	 */
	public int getColumnLength() {
		return columnLength;
	}

	/**
	 * 行数を取得.
	 * @return int 行数が返却されます.
	 */
	public int getRowLength() {
		return rowLength;
	}
	

	/**
	 * 一致した検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> eq(boolean ascFlag,  boolean notEq,
		String columnName, Object value) {
		return eq(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}

	/**
	 * 一致した検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> eq(boolean ascFlag,  boolean notEq,
		int columnNo, Object value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchEq(
				this, ascFlag, notEq, columnNo, value));
	}
	
	/**
	 * 大なり[>]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> gt(boolean ascFlag, boolean notEq,
		String columnName, Object value) {
		return gt(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}
	
	/**
	 * 大なり[>]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> gt(boolean ascFlag, boolean notEq,
		int columnNo, Object value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchGT(
				this, ascFlag, notEq, columnNo, value));
	}
	
	/**
	 * 大なり[>=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> ge(boolean ascFlag, boolean notEq,
		String columnName, Object value) {
		return ge(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}
	
	/**
	 * 大なり[>=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> ge(boolean ascFlag, boolean notEq,
		int columnNo, Object value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchGE(
				this, ascFlag, notEq, columnNo, value));
	}
	
	/**
	 * 小なり[<]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> lt(boolean ascFlag, boolean notEq,
		String columnName, Object value) {
		return lt(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}
	
	/**
	 * 小なり[<]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> lt(boolean ascFlag, boolean notEq,
		int columnNo, Object value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchLT(
				this, ascFlag, notEq, columnNo, value));
	}
	
	/**
	 * 小なり[<=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> le(boolean ascFlag, boolean notEq,
		String columnName, Object value) {
		return le(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}
	
	/**
	 * 小なり[<=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> le(boolean ascFlag, boolean notEq,
		int columnNo, Object value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchLE(
			this, ascFlag, notEq, columnNo, value));
	}
	
	/**
	 * 指定した start から end まで範囲検索.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param start 開始条件を設定します.
	 * @param end 終了条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> between(
		boolean ascFlag, boolean notEq, String columnName, Object start, Object end) {
		return between(ascFlag, notEq, getColumnNameByNo(columnName), start, end);
	}
	
	/**
	 * 指定した start から end まで範囲検索.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param start 開始条件を設定します.
	 * @param end 終了条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> between(
		boolean ascFlag, boolean notEq, int columnNo, Object start, Object end) {
		checkFix();
		return new ResultSearchBody(new NormalSearchBetween(
			this, ascFlag, notEq, columnNo, start, end));
	}
	
	/**
	 * 指定したvaluesに一致する内容を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param values 一致条件を複数設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> in(
		boolean ascFlag, boolean notEq, String columnName, Object... values) {
		return in(ascFlag, notEq, getColumnNameByNo(columnName), values);
	}
	
	/**
	 * 指定したvaluesに一致する内容を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param values 一致条件を複数設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> in(
		boolean ascFlag, boolean notEq, int columnNo, Object... values) {
		checkFix();
		return new ResultSearchBody(new NormalSearchIn(
			this, ascFlag, notEq, columnNo, values));
	}

	/**
	 * Like検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnName 列名を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> like(boolean ascFlag, boolean notEq,
		String columnName, String value) {
		return like(ascFlag, notEq, getColumnNameByNo(columnName), value);
	}
	
	/**
	 * Like検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param columnNo 列番号を設定します.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public RimResultSearch<Integer> like(boolean ascFlag, boolean notEq,
		int columnNo, String value) {
		checkFix();
		return new ResultSearchBody(new NormalSearchLike(
			this, ascFlag, notEq, columnNo, value));
	}
	
	// 指定列のBody検索結果を格納するオブジェクト.
	private static final class ResultSearchBody
		implements RimResultSearch<Integer> {
		// 各検索条件の検索管理オブジェクト.
		private NormalSearch normalSearch;
		// hasNext()呼び出しでの検索位置情報.
		private int execHasNextBySearchPosition = -1;
		
		/**
		 * コンストラクタ.
		 * @param normalSearch
		 */
		public ResultSearchBody(NormalSearch normalSearch) {
			this.normalSearch = normalSearch;
		}

		@Override
		public boolean hasNext() {
			// 終端まで読み込まれてた場合.
			if(normalSearch.isEnd()) {
				return false;
			// 今回既にnextRowId()を取得してる場合.
			} else if(execHasNextBySearchPosition != -1) {
				return true;
			// 次の情報を検索.
			} else if(
				(execHasNextBySearchPosition = normalSearch.nextRowId()) == -1) {
				// 終端を検知.
				normalSearch.updatePositon(-1);
				return false;
			}
			return true;
		}

		@Override
		public Integer next() {
			int ret = -1;
			// nextRowId()がhasNext()等で既に呼び出されている場合.
			if(execHasNextBySearchPosition != -1) {
				// nextRowId()で読まれたデータ読み込み位置を更新.
				ret = execHasNextBySearchPosition;
				execHasNextBySearchPosition = -1;
				// 位置情報をコピー.
				normalSearch.updatePositon(ret);
			}
			// 終端まで読み込まれてた場合.
			if(normalSearch.isEnd()) {
				throw new NoSuchElementException();
			} else if(ret != -1) {
				// nextRowId()がhasNext()等で既に呼び出されている
				// 場合はその時取得した値を返却.
				return ret;
			}
			// 次の情報を読み込む.
			ret = normalSearch.nextRowId();
			// 位置情報を更新.
			normalSearch.updatePositon(ret);
			// 終端を検出した場合.
			if(ret == -1) {
				throw new NoSuchElementException();
			}
			return ret;
		}
		
		@Override
		public RimRow nextRow() {
			int rowId = next();
			return normalSearch.body.getRow(rowId);
		}

		@Override
		public Comparable getValue() {
			return normalSearch.getValue();
		}

		@Override
		public int getLineNo() {
			return normalSearch.getRowId();
		}
	}
	
	// 検索管理オブジェクト雛形.
	private static abstract class NormalSearch {
		// RimBody.
		protected RimBody body;
		// body行列情報.
		protected Object[] rows;
		// 最大行番号.
		protected int rowLength;
		// 検索対象列番号.
		protected int columnNo;
		// 検索対象列タイプ.
		protected ColumnType columnType;
		// 現在取得位置情報.
		protected int position;
		// 昇順フラグ.
		protected boolean ascFlag;
		// Notフラグ.
		protected boolean notEq;
		// 読み込み終了フラグ.
		protected boolean endFlag;
		
		/**
		 * 初期化.
		 * @param body
		 * @param ascFlag
		 * @param columnNo
		 */
		protected void init(RimBody body, boolean ascFlag, boolean notEq, int columnNo) {
			this.body = body;
			this.rows = body.rows;
			this.rowLength = this.rows.length;
			this.columnNo = columnNo;
			this.columnType = body.columnTypes[columnNo];
			this.ascFlag = ascFlag;
			this.notEq = notEq;
			this.position = ascFlag ? -1 : rowLength;
		}
		
		/**
		 * ポジション更新.
		 * @param position
		 */
		protected void updatePositon(int position) {
			// -1が設定された場合は情報の終端読み込み.
			if(position == -1) {
				// 終了フラグを設定.
				endFlag = true;
			}
			this.position = position;
		}
		
		/**
		 * 対象要素を検索条件型に変換.
		 * @param value
		 * @return
		 */
		protected Comparable convert(Object value) {
			if(value == null) {
				return null;
			}
			return (Comparable)columnType.convert(value);
		}
		
		/**
		 * 読み込み終了チェック.
		 * @return
		 */
		protected boolean isEnd() {
			return endFlag;
		}
		
		/**
		 * 次のポジションを取得.
		 * @return
		 */
		protected int nextPosition() {
			return ascFlag ? position + 1 : position - 1;
		}
		
		/**
		 * 現在取得されている行番号を取得.
		 * @return
		 */
		public int getRowId() {
			if(endFlag) {
				throw new NoSuchElementException();
			}
			return position;
		}
		
		/**
		 * 現在取得されている行番号の要素を取得.
		 * @return
		 */
		public Comparable getValue() {
			if(endFlag) {
				throw new NoSuchElementException();
			}
			return (Comparable)((Object[])rows[position])[columnNo];
		}
		
		/**
		 * 条件が一致する次の行番号を取得.
		 * @return
		 */
		public abstract int nextRowId();
	}
	
	// [=] 検索管理オブジェクト.
	private static final class NormalSearchEq extends NormalSearch {
		private Comparable value;
		NormalSearchEq(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.notEq = notEq;
			this.value = super.convert(value);
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalEq(rows, columnNo, ascFlag, notEq,
				nextPosition(), value);
		}
	}
	
	// [>] 検索管理オブジェクト.
	private static final class NormalSearchGT extends NormalSearch {
		private Comparable value;
		NormalSearchGT(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.value = super.convert(value);
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalGT(rows, columnNo, ascFlag, notEq,
				nextPosition(), value);
		}
	}
	
	// [>=] 検索管理オブジェクト.
	private static final class NormalSearchGE extends NormalSearch {
		private Comparable value;
		NormalSearchGE(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.value = super.convert(value);
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalGE(rows, columnNo, ascFlag, notEq,
				nextPosition(), value);
		}
	}
	
	// [<] 検索管理オブジェクト.
	private static final class NormalSearchLT extends NormalSearch {
		private Comparable value;
		NormalSearchLT(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.value = super.convert(value);
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalLT(rows, columnNo, ascFlag, notEq,
				nextPosition(), value);
		}
	}
	
	// [<=] 検索管理オブジェクト.
	private static final class NormalSearchLE extends NormalSearch {
		private Comparable value;
		NormalSearchLE(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.value = super.convert(value);
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalLE(rows, columnNo, ascFlag, notEq,
				nextPosition(), value);
		}
	}
	
	// [between] 検索管理オブジェクト.
	private static final class NormalSearchBetween extends NormalSearch {
		private Comparable start;
		private Comparable end;
		NormalSearchBetween(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object start, Object end) {
			// start や end がnullの場合は例外.
			if(start == null || end == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.start = super.convert(start);
			this.end = super.convert(end);
			
			// 昇順・降順に合わせて、startとendを入れ替え.
			if((ascFlag && this.start.compareTo(this.end) > 0) ||
				(!ascFlag && this.start.compareTo(this.end) < 0)) {
				Comparable t = this.start;
				this.start = this.end;
				this.end = t;
			}
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			int ret = SearchUtil.normalBetween(
				rows, columnNo, ascFlag, notEq, nextPosition(),
				start, end);
			return ret;
		}
	}
	
	// [in] 検索管理オブジェクト.
	private static final class NormalSearchIn extends NormalSearch {
		private Comparable[] inList;
		NormalSearchIn(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			Object... values) {
			final int len = values.length;
			// valuesが設定されていない場合は例外.
			if(len == 0) {
				throw new RimException("At least one condition must be set.");
			}
			Comparable[] inList = new Comparable[len];
			this.init(body, ascFlag, notEq, columnNo);
			for(int i = 0; i < len; i ++) {
				values[i] = super.convert(values[i]);
				// valuesのどれかがnullの場合は例外.
				if(values[i] == null) {
					throw new RimException("Null cannot be set in the search condition.");
				}
				inList[i] = (Comparable)values[i];
			}
			Arrays.sort(inList);
			this.inList = inList;
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalIn(
				rows, columnNo, ascFlag, notEq, nextPosition(), inList);
		}
	}
	
	// [like] 検索管理オブジェクト.
	private static final class NormalSearchLike extends NormalSearch {
		private LikeParser parser;
		NormalSearchLike(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			String value) {
			// valueがnullの場合は例外.
			if(value == null) {
				throw new RimException("Null cannot be set in the search condition.");
			// 対象列型がStringでない場合は例外.
			} else if(body.getColumnType(columnNo) != ColumnType.String) {
				throw new RimException("The column type of the specified row \"" +
					body.getColumnName(columnNo) + "\" is not a String. ");
			}
			LikeParser parser = LikeAnalysis.analysis(value);
			init(body, ascFlag, notEq, columnNo, parser);
			
		}
		private final void init(RimBody body, boolean ascFlag, boolean notEq, int columnNo,
			LikeParser parser) {
			// valueがnullの場合は例外.
			if(parser == null) {
				throw new RimException("Null cannot be set in the search condition.");
			}
			this.init(body, ascFlag, notEq, columnNo);
			this.parser = parser;
		}
		@Override
		public int nextRowId() {
			if(endFlag) {
				return -1;
			}
			return SearchUtil.normalLike(rows, columnNo, ascFlag, notEq,
				nextPosition(), parser);
		}
	}
	
	// 列名から列番号を取得.
	private final int getColumnNameByNo(String columnName) {
		int no = columns.search(columnName);
		if(no == -1) {
			throw new RimException("The specified column name \"" +
				columnName + "\" does not exist. ");
		}
		return no;
	}
}
