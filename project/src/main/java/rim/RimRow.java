package rim;

import java.util.Date;

import rim.exception.RimException;
import rim.util.DateUtil;
import rim.util.FixedSearchArray;
import rim.util.TypesKeyValue;

/**
 * Rimの行情報.
 * このオブジェクトは取得毎に毎度生成はせずに使いまわします.
 */
public final class RimRow implements TypesKeyValue<Object, Object> {
	private FixedSearchArray<String> columns;
	private ColumnType[] columnsType;
	private Object[] row;

	/**
	 * コンストラクタ.
	 *
	 * @param columns     カラム管理情報を設定します.
	 * @param columnsType 型情報群を設定します.
	 */
	protected RimRow(FixedSearchArray<String> columns,
		ColumnType[] columnsType) {
		this.columns = columns;
		this.columnsType = columnsType;
	}

	/**
	 * 次の行情報を設定します.
	 */
	protected RimRow set(Object[] row) {
		this.row = row;
		return this;
	}

	/**
	 * 列名を指定して内容を取得します.
	 *
	 * @param column 列名を設定します.
	 *               数字を設定した場合、直接列項番で取得します.
	 * @return Object 要素が返却されます.
	 */
	@Override
	public Object get(Object column) {
		int n = RimBody.getColumnPos(columns, column);
		if (n == -1) {
			return null;
		}
		return row[n];
	}

	/**
	 * 列名を指定して列型を取得します.
	 *
	 * @param column 列名を設定します.
	 *               数字を設定した場合、直接列項番で取得します.
	 * @return ColumnType 列型が返却されます.
	 */
	public ColumnType getColumnType(Object column) {
		int n = RimBody.getColumnPos(columns, column);
		if (n == -1) {
			return null;
		}
		return columnsType[n];
	}

	/**
	 * 列名を取得.
	 *
	 * @param no 列番号を設定します.
	 * @return String 列名が返却されます.
	 */
	public String getColumnName(int no) {
		return columns.get(no);
	}

	/**
	 * 列名を指定して、その列名が存在するかチェックします.
	 *
	 * @param column 列名を設定します.
	 *               数字を設定した場合、直接列項番で取得します.
	 * @return boolean trueの場合、存在します.
	 */
	public boolean contains(Object column) {
		return RimBody.getColumnPos(columns, column) != -1;
	}

	/**
	 * 列数を取得します.
	 *
	 * @return
	 */
	public int size() {
		return columnsType.length;
	}
	
	// ゼロ文字配列.
	private static final String[] ZERO_STRING_ARRAY = new String[0];
	
	@Override
	public String toString() {
		return toString(ZERO_STRING_ARRAY);
	}

	/**
	 * カラム名群を指定して文字列出力.
	 * @param columnArray 表示したいカラム名群を設定します.
	 * @return String 文字列が返却されます.
	 */
	public String toString(String... columnArray) {
		int no;
		Object v;
		ColumnType type;
		final int len = columnArray.length == 0 ?
			columns.size() :
			columnArray.length;
		StringBuilder buf = new StringBuilder("{");
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(", ");
			}
			if(columnArray.length != 0) {
				if((no = columns.search(columnArray[i])) == -1) {
					throw new RimException("The specified column name \"" +
						columnArray[i] + "\" does not exist. ");
				}
				type = columnsType[no];
				v = row[no];
				buf.append(columnArray[i]).append(": ");
			} else {
				type = columnsType[i];
				v = row[i];
				buf.append(columns.get(i)).append(": ");
			}
			if (v == null) {
				buf.append("null");
			} else if (type == ColumnType.String) {
				buf.append("\"").append(v).append("\"");
			} else if (type == ColumnType.Date) {
				buf.append("\"");
				DateUtil.toRfc822(buf, (Date)v);
				buf.append("\"");
			} else {
				buf.append(v);
			}
		}
		buf.append("}");
		return buf.toString();
	}
}
