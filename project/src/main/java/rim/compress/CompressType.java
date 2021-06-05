package rim.compress;

import rim.exception.RimException;
import rim.util.Alphabet;
import rim.util.TypesUtil;

/**
 * 圧縮タイプ.
 */
public enum CompressType {
	/** 圧縮なし. **/
	None(0)
	/** デフォルト圧縮. **/
	,Default(1)
	/** Gzip圧縮. **/
	,Gzip(2)
	/** LZ4圧縮. **/
	,LZ4(10)
	/** Zstd圧縮. **/
	,Zstd(11)
	;
	
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
		case 10: return LZ4;
		case 11: return Zstd;
		}
		throw new RimException("Unknown compression ID: " + id);
	}
	
	/**
	 * Zstdを文字列指定した場合の圧縮レベルを取得.
	 * @param name 文字列を設定します.
	 *             圧縮レベルを付与したzstdの指定の場合は
	 *             "zstd10"のように語尾に圧縮レベルを数値で
	 *             設定することで圧縮レベルを指定出来ます.
	 *             また、指定せず"zstd"の場合はデフォルトの
	 *             圧縮レベルが返却されます.
	 * @return int 圧縮レベルが返却されます.
	 *             -1が返却された場合はzstd指定でないか、
	 *             指定内容が正しく無いので取得出来ません。
	 *             （数字以外か範囲外(1-22)の場合）
	 */
	public static final int getZstdLevel(String name) {
		int p = -1;
		// 頭の文字がzstd関連で一致するかチェック.
		if(Alphabet.indexOf(name, "zstd") == 0) {
			p = 4;
		} else if(Alphabet.indexOf(name, "zstandard") == 0) {
			p = 9;
		}
		// zstd系のシンボル文字が設定されている場合.
		if(p != -1) {
			// zstd系のシンボルだけが設定されている場合.
			if(Alphabet.eqArray(name, "zstd", "zstandard") != -1) {
				// デフォルトの圧縮レベルを返却.
				return ZstdCompress.DEFAULT_LEVEL;
			}
			// zstd10 のように語尾に圧縮レベルの数値が指定されているかチェック.
			Integer level = TypesUtil.getInteger(name.substring(p));
			// 数字でない場合、もしくは圧縮レベルの範囲外の場合.
			if(level == null || !ZstdCompress.isLevel(level)) {
				return -1;
			}
			// 圧縮レベルを返却.
			return level;
		}
		return -1;
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
		} else if(Alphabet.eqArray(name, "lz4") != -1) {
			return LZ4;
		} else if(getZstdLevel(name) >= 1) {
			return Zstd;
		}
		throw new RimException("Unknown compressed string: " +
			name);
	}
}
