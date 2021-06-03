package rim;

import rim.exception.RimException;
import rim.util.Alphabet;

/**
 * 圧縮タイプ.
 */
public enum CompressType {
	/** 圧縮なし. **/
	None(0)
	/** デフォルト圧縮. **/
	,Default(1)
	/** Gzip圧縮. **/
	,Gzip(2);
	
	// 圧縮ID.
	private int id;
	
	/**
	 * コンストラクタ.
	 * @param id 圧縮IDを設定します.
	 */
	private CompressType(int id) {
		this.id = id;
	}
	
	/**
	 * 圧縮IDを取得.
	 * @return int 圧縮IDが返却されます.
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * 圧縮IDを設定して圧縮タイプを取得.
	 * @param id 圧縮IDを設定します.
	 * @return CompressType 圧縮タイプが返却されます.
	 */
	public static final CompressType get(int id) {
		switch(id) {
		case 0: return None;
		case 1: return Default;
		case 2: return Gzip;
		}
		throw new RimException("Unknown compression ID: " + id);
	}
	
	/**
	 * 文字を設定して圧縮タイプを取得.
	 * @param name 圧縮タイプを取得するための文字を設定します.
	 * @return CompressType 圧縮タイプが返却されます.
	 */
	public static final CompressType get(String name) {
		if(Alphabet.eqArray(name, "no", "none") != -1) {
			return None;
		} else if(Alphabet.eqArray(
			name, "def", "default", "normal") != -1) {
			return Default;
		} else if(Alphabet.eqArray(name, "gzip", "zip") != -1) {
			return Gzip;
		}
		throw new RimException("Unknown compressed string: " +
			name);
	}
}
