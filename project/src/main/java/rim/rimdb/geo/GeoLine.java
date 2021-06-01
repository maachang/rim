package rim.rimdb.geo;

/**
 * [正確＋低速版]緯度経度間の直線距離計算.
 *
 * 速度は遅いですが、正しく距離を計算します.
 */
public final class GeoLine {
	protected GeoLine() {
	}

	/** ベッセル楕円体(旧日本測地系). **/
	private static final double BESSEL_A = 6377397.155;
	private static final double BESSEL_E2 = 0.00667436061028297;
	private static final double BESSEL_MNUM = 6334832.10663254;

	/** GRS80(世界測地系). **/
	private static final double GRS80_A = 6378137.000;
	private static final double GRS80_E2 = 0.00669438002301188;
	private static final double GRS80_MNUM = 6335439.32708317;

	/** WGS84(GPS). **/
	private static final double WGS84_A = 6378137.000;
	private static final double WGS84_E2 = 0.00669437999019758;
	private static final double WGS84_MNUM = 6335439.32729246;

	private static final double[] _A = new double[] { BESSEL_A, GRS80_A, WGS84_A };
	private static final double[] _E2 = new double[] { BESSEL_E2, GRS80_E2, WGS84_E2 };
	private static final double[] _MNUM = new double[] { BESSEL_MNUM, GRS80_MNUM, WGS84_MNUM };

	/** ベッセル楕円体(旧日本測地系). **/
	public static final int BESSEL = 0;

	/** GRS80(世界測地系). **/
	public static final int GRS80 = 1;

	/** WGS84(GPS). **/
	public static final int WGS84 = 2;

	/** PI-180. **/
	private static final double PI_180 = Math.PI / 180.0;

	/**
	 * 厳密な直線を求める.
	 * Copyright © 2007-2012 やまだらけ
	 * http://yamadarake.jp/trdi/report000001.html This software is released under
	 * the MIT License.
	 */
	private static final double _getLatLonLine(final double srcLat, final double srcLon, final double destLat,
			final double destLon, final double a, final double e2, final double mnum) {

		final double pi180 = PI_180;
		final double my = ((srcLat + destLat) / 2.0) * pi180;
		final double dy = (srcLat - destLat) * pi180;
		final double dx = (srcLon - destLon) * pi180;

		final double s = Math.sin(my);
		final double w = Math.sqrt(1.0 - e2 * s * s);
		final double m = mnum / (w * w * w);
		final double n = a / w;

		final double dym = dy * m;
		final double dxncos = dx * n * Math.cos(my);

		return Math.sqrt(dym * dym + dxncos * dxncos);
	}

	/**
	 * 緯度経度のポイント間の直線距離（メートル）を厳密に求める. この計算では[GRS80(世界測地系)]で求められます.
	 *
	 * @param srcLat
	 *            元の緯度を設定します.
	 * @param srcLon
	 *            元の経度を設定します.
	 * @param destLat
	 *            先の緯度を設定します.
	 * @param destLon
	 *            先の経度を設定します.
	 * @return double 距離が返却されます.
	 */
	public static double get(final double srcLat, final double srcLon, final double destLat, final double destLon) {
		return _getLatLonLine(srcLat, srcLon, destLat, destLon, GRS80_A, GRS80_E2, GRS80_MNUM);
	}

	/**
	 * 緯度経度のポイント間の直線距離（メートル）を厳密に求める.
	 *
	 * @param type
	 *            以下の条件が指定可能です. [BESSEL] : ベッセル楕円体(旧日本測地系). [GRS80] : GRS80(世界測地系).
	 *            [WGS84] : WGS84(GPS).
	 * @param srcLat
	 *            元の緯度を設定します.
	 * @param srcLon
	 *            元の経度を設定します.
	 * @param destLat
	 *            先の緯度を設定します.
	 * @param destLon
	 *            先の経度を設定します.
	 * @return double 距離が返却されます.
	 */
	public static double get(final int type, final double srcLat, final double srcLon, final double destLat,
			final double destLon) {
		return _getLatLonLine(srcLat, srcLon, destLat, destLon, _A[type], _E2[type], _MNUM[type]);
	}

}
