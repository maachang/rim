package rim;

/**
 * Geo検索結果の情報.
 */
public interface RimResultGeo
	extends RimResult {
	
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
	
	/**
	 * 厳密な半径を再計算.
	 * @return double 厳密な半径を計算して返却されます.
	 */
	public double getStrictRedius();
}
