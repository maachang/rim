package rim;

import java.util.Arrays;

import rim.exception.RimException;
import rim.util.FixedSearchArray;
import rim.util.ObjectList;
import rim.util.TypesUtil;

/**
 * Rim-Bodyデーター.
 */
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
	
	@SuppressWarnings("rawtypes")
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
		for(int i = 0; i < rowLength; i ++) {
			rows[columnNo] = columns[i];
		}
		// 設定した列に対してフラグをON.
		settingRows[columnNo] = true;
	}
	
	/**
	 * 追加処理が完了した場合に呼び出します.
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
		if(rowId < 0 || rowId >= rowLength) {
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
		return columnLength;
	}

	/**
	 * 行数を取得.
	 * @return int 行数が返却されます.
	 */
	public int getRowLength() {
		return rowLength;
	}
}
