package rim.geo;

/**
 * [不正確＋高速版]緯度経度間の直線距離計算.
 *
 * 速度は早いですが、誤差がそこそこ出ます.
 */
public final class GeoFastLine {
	private GeoFastLine() {}

	/** 緯度1メートル係数. **/
	private static final double _latitudeM = 0.0002777778 / 30.8184033702;

	/** 経度１メートル係数. **/
	private static final double _longitudeM = 0.0002777778 / 25.244958122;

	/**
	 * メートル換算された緯度を通常の緯度に戻します.
	 *
	 * @param lat
	 *            緯度を設定します.
	 * @return double 元の単位に計算された内容が返却されます.
	 */
	public static final double calcLat(final int lat) {
		return ((double) lat * _latitudeM);
	}

	/**
	 * メートル換算された経度を通常の経度に戻します.
	 *
	 * @param lon
	 *            経度を設定します.
	 * @return double 元の単位に計算された内容が返却されます.
	 */
	public static final double calcLon(final int lon) {
		return ((double) lon * _longitudeM);
	}

	/**
	 * 緯度をメートル計算.
	 *
	 * @param lat
	 *            緯度を設定します.
	 * @return int メートル単位に計算された情報が返却されます.
	 */
	public static final int calcLat(final double lat) {
		return (int) (lat / _latitudeM);
	}

	/**
	 * 経度をメートル計算.
	 *
	 * @param lon
	 *            経度を設定します.
	 * @return int メートル単位に計算された情報が返却されます.
	 */
	public static final int calcLon(final double lon) {
		return (int) (lon / _longitudeM);
	}

	/**
	 * メートル換算された緯度経度の直線距離を計算. この処理は、厳密な[getLatLonLine]よりも精度が劣りますが、
	 * 計算速度はビット計算で求めているので、とても高速に動作します.
	 *
	 * @param ax
	 *            中心位置の緯度を設定します.
	 * @param ay
	 *            中心位置の経度を設定します.
	 * @param bx
	 *            対象位置の緯度を設定します.
	 * @param by
	 *            対象位置の経度を設定します.
	 * @return 大まかな直線距離が返却されます.
	 */
	public static final int get(final double ax, final double ay, final double bx, final double by) {
		return get((int) (ax / _latitudeM), (int) (ay / _longitudeM), (int) (bx / _latitudeM),
				(int) (by / _longitudeM));
	}

	/**
	 * メートル換算された緯度経度の直線距離を計算. http://d.hatena.ne.jp/nowokay/20120604#1338773843 を参考.
	 * この処理は、厳密な[getLatLonLine]よりも精度が劣りますが、 計算速度はビット計算で求めているので、とても高速に動作します.
	 *
	 * @param ax
	 *            中心位置の緯度(メートル変換されたもの)を設定します.
	 * @param ay
	 *            中心位置の経度(メートル変換されたもの)を設定します.
	 * @param bx
	 *            対象位置の緯度(メートル変換されたもの)を設定します.
	 * @param by
	 *            対象位置の経度(メートル変換されたもの)を設定します.
	 * @return 大まかな直線距離が返却されます.
	 */
	public static final int get(final int ax, final int ay, final int bx, final int by) {
		// 精度はあまり高めでないが、高速で近似値を計算できる.
		final int dx, dy;
		return ((dx = (ax > bx) ? ax - bx : bx - ax) < (dy = (ay > by) ? ay - by : by - ay)) ?
			(((dy << 8) + (dy << 3) - (dy << 4) - (dy << 1) + (dx << 7) - (dx << 5) + (dx << 3) - (dx << 1)) >> 8) :
			(((dx << 8) + (dx << 3) - (dx << 4) - (dx << 1) + (dy << 7) - (dy << 5) + (dy << 3) - (dy << 1)) >> 8);
	}

}
