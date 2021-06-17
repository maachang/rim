package rim.index;

import rim.core.ColumnType;

/**
 * RimIndex要素.
 */
@SuppressWarnings("rawtypes")
public interface RimIndexElement extends Comparable {
	/**
	 * インデックス型を取得.
	 * @return ColumnType インデックス型が返却されます.
	 */
	public ColumnType getColumnType();
	
	/**
	 * Valueを取得.
	 * @return Comparable 要素のValueが返却されます.
	 */
	public Comparable getValue();

	/**
	 * Valueに対する行番号の管理数を取得.
	 * @return int 行番号が返却されます.
	 */
	public int getLineLength();

	/**
	 * 指定された位置の行番号を取得.
	 * @param no 取得項番を設定します.
	 * @return int 行番号が返却されます.
	 */
	public int getLineNo(int no);
}
