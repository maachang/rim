package rim.core;

import java.util.Iterator;

/**
 * 基本Flag配列インターフェイス.
 */
public interface BaseFlags<T> {
	/**
	 * フラグのクリア.
	 * @return T このオブジェクトが返却されます.
	 */
	public T clear();
	
	/**
	 * フラグのセット.
	 * @param no 番号を設定します.
	 * @param flag 設定するフラグ条件を設定します.
	 * @return T このオブジェクトが返却されます.
	 */
	public T put(int no, boolean flag);
	
	/**
	 * フラグの取得.
	 * @param no 番号を設定します.
	 * @return boolean 設定されているフラグが返却されます.
	 */
	public boolean get(int no);
	
	/**
	 * フラグ管理長を取得.
	 * @return int フラグ管理数が返却されます.
	 */
	public int getLength();
	
	/**
	 * AND処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとANDするためのFlagsを設定します.
	 * @return T このオブジェクトが返却されます.
	 */
	public T and(T value);
	
	/**
	 * OR処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとORするためのFlagsを設定します.
	 * @return T このオブジェクトが返却されます.
	 */
	public T or(T value);
	
	/**
	 * オブジェクトを別のインスタンスでコピー.
	 * @return T コピーされたオブジェクトが返却されます.
	 */
	public T copy();
	
	/**
	 * 有効な行番号を取得するIteratorを取得.
	 * @param ascFlag 昇順取得の場合は true.
	 * @return Iterator<Integer> 有効な行番号を返却する
	 *                           Iteratorが返却されます.
	 */
	public Iterator<Integer> iterator(boolean ascFlag);
}
