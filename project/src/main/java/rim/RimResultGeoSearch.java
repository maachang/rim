package rim;

/**
 * Geo検索結果の情報.
 */
public interface RimResultGeoSearch<T>
	extends RimResultSearch<T> {
	
	/**
	 * 対象の緯度を取得.
	 * @return double 緯度が返却されます.
	 */
	public double getLat();
	
	/**
	 * 対象の経度を取得.
	 * @return double 経度が返却されます.
	 */
	public double getLon();
}
