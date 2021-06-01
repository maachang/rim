package rim;

import rim.exception.RimException;
import rim.util.FixedSearchArray;
import rim.util.TypesUtil;

/**
 * Rim-Bodyデーター.
 */
public class RimBody {

	// 列名群.
	private FixedSearchArray<String> columns;

	// 列型群.
	private ColumnType[] columnTypes;

	// 行リスト.
	private Object[] rows;

	// 行情報.
	private RimRow rimRow;

	// 追加カウント.
	private int addCount = 0;
	
	// fixフラグ.
	private boolean fixFlag = false;

	/**
	 * コンストラクタ.
	 * @param columns 列名群を設定します.
	 * @param types 列タイプを設定します.
	 * @param rowLength 総行数を設定します.
	 */
	protected RimBody(String[] columns, ColumnType[] types, int rowLength) {
		FixedSearchArray<String> h = new FixedSearchArray<String>(columns);
		Object[] r = new Object[rowLength];
		this.columns = h;
		this.columnTypes = types;
		this.rows = r;
		this.rimRow = new RimRow(h, types);
		this.fixFlag = false;
	}

	/**
	 * 行情報の追加.
	 * @param row 行情報を設定します.
	 */
	protected void add(Object[] row) {
		if(row == null) {
			throw new RimException(
				"The row information to be added is not set.");

		} else if(row.length != columnTypes.length) {
			throw new RimException("The number of columns (" +
				row.length + ") in the specified row to be added " +
				"does not match the defined number of columns (" +
				columnTypes.length + "). ");

		} else if(rows.length <= addCount) {
			throw new RimException(
				"Additions have been made beyond the planned line.");

		}
		rows[addCount ++] = row;
	}
	
	/**
	 * 追加処理が完了した場合に呼び出します.
	 */
	protected void fix() {
		if(fixFlag) {
			return;
		} else if(rows.length != addCount) {
			throw new RimException("The expected number of lines (" +
				rows.length + ") has not been reached (" + addCount + ").");
		}
		fixFlag = true;
	}

	/**
	 * 行データがすべて追加されたかチェック.
	 * @return boolean true の場合すべて追加されてます.
	 */
	protected boolean isFix() {
		return fixFlag;
	}

	/**
	 * 行追加が完了しているかチェック.
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
	protected static final int getColumnPos(FixedSearchArray<String> columns,
		Object column) {
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
		if(rowId < 0 || rowId >= rows.length) {
			throw new RimException("Line numbers are out of range.");
		}
		return rimRow.set((Object[])rows[rowId]);
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
		return columnTypes.length;
	}

	/**
	 * 行数を取得.
	 * @return int 行数が返却されます.
	 */
	public int getRowLength() {
		return rows.length;
	}
}
