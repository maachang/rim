package rim;

import java.util.Iterator;

/**
 * 検索結果の情報.
 */
@SuppressWarnings("rawtypes")
interface ResultSearch<T> extends Iterator<T> {
	/**
	 * 対象要素を取得.
	 * @return Comprable 対象要素が返却されます.
	 */
	public Comparable getValue();
	
	/**
	 * 対象の行番号を取得.
	 * @return int 行番号が返却されます.
	 */
	public int getLineNo();

}
