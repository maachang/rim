package rim.rimdb.geo;

/**
 * QuadKey.
 *
 * このQuadKeyは、Microsoftの提唱している、Google地図やBing地図で使われている
 * 地図画像のタイル配信のインデックス仕様.
 *
 * http://msdn.microsoft.com/en-us/library/bb259689.aspx
 *
 * ただし、本来のMicrosoftのQuadKeyは文字なのだが、このQuadKeyは、2ビット単位で
 * 情報を保持しており、数値で扱えるので、文字列での検索と比べて、高速に検索が可能.
 */
public final class GeoQuad {
	protected GeoQuad() {}

	/////////////////////////

	// detail 条件.

	// zoom 1Dotの距離(約) 矩形距離(約) 1Dotの距離(m) 矩形距離(m)

	// 1 78 km 20037km 78,271.52 m 20037508.35200 m
	// 2 39 km 10018km 39,135.76 m 10018754.17600 m
	// 3 19.6 km 5009km 19,567.88 m 5009377.07520 m
	// 4 9.8 km 2504km 9,783.94 m 2504688.53760 m
	// 5 4.9 km 1252km 4,891.97 m 1252344.26880 m
	// 6 2.4 km 626km 2,445.98 m 626172.13440 m
	// 7 1.2 km 313km 1,222.99 m 313086.08000 m
	// 8 611 m 156km 611.4962 m 156543.02720 m
	// 9 306 m 78km 305.7481 m 78271.51360 m
	// 10 153 m 39km 152.8741 m 39135.76960 m
	// 11 76 m 19km 76.437 m 19567.87200 m
	// 12 39 m 9km 38.2185 m 9783.93600 m
	// 13 19 m 4km 19.1093 m 4891.98080 m
	// 14 10 m 2km 9.5546 m 2445.97760 m
	// 15 5 m 1km 4.7773 m 1222.98880 m
	// 16 2.4 m 600m 2.3887 m 611.50720 m
	// 17 1.2 m 300m 1.1943 m 305.74080 m
	// 18 60 cm 150m 0.5972 m 152.88320 m
	// 19 30 cm 75m 0.2986 m 76.4416 m
	// 20 15 cm 40m 0.1493 m 38.2208 m
	// 21 7 cm 20m 0.0746 m 19.0976 m
	// 22 3 cm 10m 0.0373 m 9.5488 m
	// 23 1.5 cm 5m 0.01865 m 4.7744 m

	/////////////////////////

	/** 最大detail値. **/
	public static final int MAX_DETAIL = 23;

	/** 距離(メートル)別情報. **/
	private static final int[] DISTANCE_LIST = { 40075017, 20037508, 10018754, 5009377, 2504689, 1252344, 626172,
		313086, 156543, 78272, 39136, 19568, 9784, 4892, 2446, 1223, 612, 306, 153, 76, 38, 19, 10, 5 };

	/** PI. **/
	private static final double PI = 3.14159265358979323846d;

	/** SINを求めるときのPI値. **/
	private static final double SIN_LAT_PI = PI / 180d;

	/** PI * 4. **/
	private static final double PI4 = 4d * PI;

	/** 緯度経度を四捨五入. **/
	protected static final double r(final double n) {
		return (double)((long)((n + 0.0000005d) * 1000000d)) / 1000000d;
	}

	/** 緯度経度から、QuadKeyを生成. **/
	private static final long latLonToQuadKey(final int detail, final double lat, final double lon) {
		final double sLat = Math.sin(r(lat) * SIN_LAT_PI);
		final double x = (r(lon) + 180d) / 360d;
		final double y = 0.5d - Math.log((1d + sLat) / (1d - sLat)) / PI4;
		final double mapSize = (double) (256L << (long) detail);
		final int xx = ((int) (x * mapSize + 0.5d)) >> 8;
		final int yy = ((int) (y * mapSize + 0.5d)) >> 8;
		long ret = 0;
		long cnt = (long) (detail << 1);
		int i, n, mask;
		for (i = detail; i > 0; i--) {
			n = i - 1;
			mask = 1 << n;
			cnt -= 2L;
			ret |= (long) (((xx & mask) >> n) + (((yy & mask) >> n) << 1)) << cnt;
		}
		return ret;
	}

	/** QuadKeyから緯度経度変換. **/
	private static final void quadKeyToLatLon(final double[] out, final long key, final int detail) {
		int i, mask;
		int xx = 0;
		int yy = 0;
		long cnt = (long) (detail << 1);
		for (i = detail; i > 0; i--) {
			cnt -= 2L;
			mask = 1 << (i - 1);
			switch ((int) ((key & (3L << cnt)) >> cnt)) {
			case 1:
				xx |= mask;
				break;
			case 2:
				yy |= mask;
				break;
			case 3:
				xx |= mask;
				yy |= mask;
				break;
			}
		}
		final double mapSize = (double) (256L << (long) detail);
		final double x = ((double) (xx << 8) / mapSize) - 0.5d;
		final double y = 0.5d - ((double) (yy << 8) / mapSize);
		out[0] = r(90 - 360 * Math.atan(Math.exp(-y * 2 * PI)) / PI);
		out[1] = r(360 * x);
	}

	/** 検索用QuadKeyを生成. **/
	private static final void createSearchData(final long[] out, final int no, final int detail, final int tileX,
			final int tileY) {
		int i, n, mask;
		long key = 0;
		long cnt = (long) (detail << 1);
		for (i = detail; i > 0; i--) {
			n = i - 1;
			mask = 1 << n;
			cnt -= 2L;
			key |= (long) (((tileX & mask) >> n) + (((tileY & mask) >> n) << 1)) << cnt;
		}
		final long shift = ((MAX_DETAIL - detail) << 1L);
		out[no] = key << shift;
		if (detail == MAX_DETAIL) {
			out[no + 1] = out[no];
		} else {
			out[no + 1] = out[no] | (0x7fffffffffffffffL & ((1L << shift) - 1L));
		}
	}

	/**
	 * 指定距離(メートル)から、detailを取得.
	 *
	 * @param distance 対象の距離(メートル)を設定します.
	 * @return detail 距離に対するDetailが返却されます.
	 */
	public static final int getDetail(final int distance) {
		final int[] list = DISTANCE_LIST;
		final int len = MAX_DETAIL;
		for (int i = len; i > 0; i--) {
			if (list[i] >= distance) {
				return i;
			}
		}
		return 1;
	}

	/**
	 * 指定拡大率から、距離を取得.
	 *
	 * @param detail 拡大率を設定します.
	 *               この値は[1]～[23]の範囲で設定します.
	 * @return int 距離(メートル)が返却されます.
	 */
	public static final int getDistance(final int detail) {
		return DISTANCE_LIST[detail];
	}

	/**
	 * QuadKeyを生成.
	 *
	 * @param lat 緯度を設定します.
	 * @param lon 経度を設定します.
	 * @return long QuadKeyが返却されます.
	 */
	public static final long create(double lat, double lon) {
		return latLonToQuadKey(MAX_DETAIL, r(lat), r(lon));
	}

	/**
	 * QuadKeyを生成.
	 *
	 * @param detail
	 *            拡大率を設定します.
	 *            この値は[1]～[23]の範囲で設定します.
	 * @param lat
	 *            緯度を設定します.
	 * @param lon
	 *            経度を設定します.
	 * @return long QuadKeyが返却されます.
	 */
	public static final long create(int detail, double lat, double lon) {
		if (detail != MAX_DETAIL) {
			return latLonToQuadKey(detail, r(lat), r(lon)) << (long) ((MAX_DETAIL - detail) << 1);
		}
		return latLonToQuadKey(detail, r(lat), r(lon));
	}

	/**
	 * QuadKeyから緯度経度を取得.
	 *
	 * @param out [0]緯度,[1]経度 が格納されます.
	 * @param key 対象のQuadKeyを設定します.
	 */
	public static final void latLon(double[] out, long key) {
		quadKeyToLatLon(out, key, MAX_DETAIL);
	}

	/**
	 * QuadKeyから緯度経度を取得.
	 *
	 * @param key 対象のQuadKeyを設定します.
	 * @return double[] [0]緯度,[1]経度 が格納されます.
	 */
	public static final double[] latLon(long key) {
		final double[] ret = new double[2];
		quadKeyToLatLon(ret, key, MAX_DETAIL);
		return ret;
	}

	/**
	 * QuadKeyの終端キーを取得.
	 * また、[quadKey]でキーを作成した場合、detailを23(最大値)で生成した場合は、
	 * この条件に意味を持ちません.
	 *
	 * @param detail 拡大率を設定します.
	 *               この値は[1]～[23]の範囲で設定します.
	 * @param key [quadKey]で生成したキーを設定します.
	 */
	public static final long endKey(int detail, long key) {
		if (detail == MAX_DETAIL) {
			return key;
		}
		return key | (0x7fffffffffffffffL & ((1L << (long) ((MAX_DETAIL - detail) << 1)) - 1L));
	}

	/**
	 * 範囲検索用データの作成.
	 *
	 * @param out 範囲検索用QueadKeyを格納する条件を設定します. [n]最小値. [n+1]最大値 ...
	 *            検索範囲のQuadKeyの最小値、最大値 合計で18個が格納されます.
	 * @param detail 拡大率を設定します. この値は[1]～[23]の範囲で設定します.
	 * @param lat 緯度を設定します.
	 * @param lon 経度を設定します.
	 */
	public static final void searchCode(long[] out, int detail, double lat, double lon) {
		// 検索データの中心データを作成.
		final double sLat = Math.sin(r(lat) * SIN_LAT_PI);
		final double x = (r(lon) + 180d) / 360d;
		final double y = 0.5d - Math.log((1d + sLat) / (1d - sLat)) / PI4;
		final double mapSize = (double) (256L << (long) detail);
		final int xx = ((int) (x * mapSize + 0.5d)) >> 8;
		final int yy = ((int) (y * mapSize + 0.5d)) >> 8;

		//□□□
		//□■□
		//□□□
		// 中心データを作成.
		createSearchData(out, 0, detail, xx, yy);

		//□□□
		//□□■
		//□□□
		// 中心より右のデータを作成.
		createSearchData(out, 2, detail, xx + 1, yy);

		//□□□
		//□□□
		//□■□
		// 中心より下のデータを作成.
		createSearchData(out, 4, detail, xx, yy + 1);

		//□□□
		//■□□
		//□□□
		// 中心より左のデータを作成.
		createSearchData(out, 6, detail, xx - 1, yy);

		//□■□
		//□□□
		//□□□
		// 中心より上のデータを作成.
		createSearchData(out, 8, detail, xx, yy - 1);

		//□□□
		//□□□
		//□□■
		// 中心より右下のデータを作成.
		createSearchData(out, 10, detail, xx + 1, yy + 1);

		//□□□
		//□□□
		//■□□
		// 中心より左下のデータを作成.
		createSearchData(out, 12, detail, xx - 1, yy + 1);

		//■□□
		//□□□
		//□□□
		// 中心より左上のデータを作成.
		createSearchData(out, 14, detail, xx - 1, yy - 1);

		//□□■
		//□□□
		//□□□
		// 中心より右上のデータを作成.
		createSearchData(out, 16, detail, xx + 1, yy - 1);
	}

	/**
	 * 範囲検索用データの作成.
	 *
	 * @param detail 拡大率を設定します.
	 *               この値は[1]～[23]の範囲で設定します.
	 * @param lat 緯度を設定します.
	 * @param lon 経度を設定します.
	 * @return long[] 範囲検索のQuadKeyが返却されます. [n]最小値. [n+1]最大値 ... 検索範囲のQuadKeyの最小値、最大値
	 *                合計で18個が格納されます.
	 */
	public static final long[] searchCode(int detail, double lat, double lon) {
		final long[] ret = new long[18];
		searchCode(ret, detail, lat, lon);
		return ret;
	}
}
