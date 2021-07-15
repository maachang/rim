package rim.index;

import java.util.NoSuchElementException;

import rim.RimBody;
import rim.RimResultGeo;
import rim.RimRow;
import rim.core.ColumnType;
import rim.core.SearchUtil;
import rim.exception.RimException;
import rim.geo.GeoFastLine;
import rim.geo.GeoLine;
import rim.geo.GeoQuad;
import rim.util.IndexKeyList;
import rim.util.ObjectList;

/**
 * Geo(緯度経度から半径検索)インデックス情報.
 */
@SuppressWarnings("rawtypes")
public class GeoIndex {
	// インデックス型(long).
	private static final ColumnType VALUE_TYPE = ColumnType.Long;
	
	// RimBody.
	private RimBody body;
	// Bodyに対する緯度を示す列番号.
	private int latColumnNo;
	// Bodyに対する経度を示す列番号.
	private int lonColumnNo;
	
	// インデックス内の行を管理するバイト数.
	private int indexByte;
	// インデックス情報.
	private ObjectList<RimIndexElement> index = new ObjectList<RimIndexElement>();
	// 登録予定のインデックス総行数.
	private int planIndexSize;
	
	// fixしたインデックス情報.
	RimIndexElement[] fixIndex;
	// インデックス総数.
	private int indexSize;
	
	/**
	 * コンストラクタ.
	 * @param rimBody RimBodyを設定します.
	 * @param latColumnNo このインデックスの緯度列番号が設定されます.
	 * @param lonColumnNo このインデックスの経度列番号が設定されます.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 */
	public GeoIndex(RimBody body, int latColumnNo, int lonColumnNo,
		int planIndexSize) {
		this.body = body;
		this.latColumnNo = latColumnNo;
		this.lonColumnNo = lonColumnNo;
		this.indexByte = RimIndexUtil.getRowByteLength(body.getRowLength());
		this.planIndexSize = planIndexSize;
		this.indexSize = 0;
	}
	
	/**
	 * １つのインデックス要素を追加.
	 * @param value 要素を設定します.
	 * @param rowIds 行Id群を設定します.
	 * @param len 有効な長さを設定します.
	 * @return int 現在までの追加件数を取得.
	 */
	public int add(Long value, int[] rowIds, int len) {
		checkFixToError();
		final RimIndexElement em = RimIndexUtil.createRimIndexElement(
			indexByte, VALUE_TYPE, value, rowIds, len);
		index.add(em);
		indexSize += len;
		return indexSize;
	}
	
	/**
	 * 追加処理が完了した場合に呼び出します.
	 */
	public void fix() {
		if(index == null) {
			return;
		} else if(planIndexSize != indexSize) {
			throw new RimException(
				"It does not match the expected number of geo index rows(" +
				planIndexSize + "/" + indexSize + ")");
		}
		fixIndex = index.toArray(RimIndexElement.class);
		index = null;
	}

	/**
	 * 追加処理がFixしているか取得.
	 * @return boolean trueの場合Fixしています.
	 */
	public boolean isFix() {
		return index == null;
	}

	/**
	 * 行追加が完了してない場合エラー出力.
	 */
	protected void checkNoFixToError() {
		if(!isFix()) {
			throw new RimException(
				"Geo index addition is not complete.");
		}
	}
	
	/**
	 * 行追加が完了してる場合エラー出力.
	 */
	protected void checkFixToError() {
		if(isFix()) {
			throw new RimException(
				"Geo index addition is complete.");
		}
	}
	
	/**
	 * インデックス対象の緯度を示す列番号を取得.
	 * @return int インデックス対象の列番号が返却されます.
	 */
	public int getLatColumnNo() {
		return latColumnNo;
	}

	/**
	 * インデックス対象の緯度を示す列名を取得.
	 * @return String インデックス対象の列名が返却されます.
	 */
	public String getLatColumnName() {
		return body.getColumnName(latColumnNo);
	}
	
	/**
	 * インデックス対象の経度を示す列番号を取得.
	 * @return int インデックス対象の列番号が返却されます.
	 */
	public int getLonColumnNo() {
		return lonColumnNo;
	}

	/**
	 * インデックス対象の経度を示す列名を取得.
	 * @return String インデックス対象の列名が返却されます.
	 */
	public String getLonColumnName() {
		return body.getColumnName(lonColumnNo);
	}
	
	/**
	 * インデックス対象の列型を取得.
	 * @return ColumnType インデックス対象の列型が返却されます.
	 */
	public ColumnType getColumnType() {
		return VALUE_TYPE;
	}

	/**
	 * 設定されているインデックス行数を取得.
	 * @return int インデックス行数が返却されます.
	 */
	public int getLength() {
		return indexSize;
	}
	
	/**
	 * 中心点の緯度・経度と半径（メートル）を設定して、
	 * その範囲内の検索結果を取得.
	 * @param lat 中心点の緯度を設定します.
	 * @param lon 中心点の経度を設定します.
	 * @param distance 半径（メートル）を設定します.
	 * @return ResultSearch 検索結果が返却されます.
	 */
	public RimResultGeo searchRadius(
		double lat, double lon, int distance) {
		checkNoFixToError();
		return new ResultRadiusSearch(
			body, latColumnNo, lonColumnNo, fixIndex,
			lat, lon, distance);
	}
	
	/**
	 * 中心点の緯度・経度と半径（メートル）を設定して、
	 * その範囲内の検索結果をソート済みで取得.
	 * @param ascFlag 昇順で結果を取得したい場合は true.
	 * @param lat 中心点の緯度を設定します.
	 * @param lon 中心点の経度を設定します.
	 * @param distance 半径（メートル）を設定します.
	 * @return ResultSearch 検索結果が返却されます.
	 */
	public RimResultGeo searchRadius(
		boolean ascFlag, double lat, double lon, int distance) {
		checkNoFixToError();
		return new ResultAscRadiusSearch(ascFlag,
			new ResultRadiusSearch(
				body, latColumnNo, lonColumnNo, fixIndex,
				lat, lon, distance)
			);
	}
	
	// 中心の緯度経度と半径を指定して範囲内の条件を検索.
	private static final class ResultRadiusSearch
		implements RimResultGeo {
		// body情報.
		private final RimBody body;
		
		// 緯度の列番号.
		private final int latColumnNo;
		// 経度の列番号.
		private final int lonColumnNo;
		
		// 中心点の緯度.
		private final double srcLat;
		// 中心点の緯度（メートル）.
		private final int srcLatM;
		// 中心点の経度.
		private final double srcLon;
		// 中心点の経度（メートル）.
		private final int srcLonM;
		// 検索対象半径（メートル）.
		private final int targetRedius;
		
		// インデックス情報.
		private final RimIndexElement[] index;
		// 検索結果の範囲情報群.
		private final long[] betweenList;
		// 現在読み込み中の検索結果のポジション.
		private int betweenPos;
		// 現在取得中のIndex位置.
		private int indexPos;
		// 現在取得中のIndex終了位置.
		private int indexEndPos;
		
		// 現在取得中のIndex要素.
		private RimIndexElement element;
		// 取得中Indexの行取得ポジション.
		private int elementPos;
		
		// nextGetで取得済み条件.
		private boolean nextGetFlag;
		// 今回取得情報.
		private int nowLineNo;
		private int nowP2pRadius;
		
		// 取得同一行情報管理.
		private IndexKeyList acquiredRowIdList;
		
		/**
		 * コンストラクタ.
		 * @param body RimBodyを設定します.
		 * @param index インデックス情報を設定します.
		 * @param lat 中心点の緯度を設定します.
		 * @param lon 中心点の経度を設定します.
		 * @param redius 検索する半径（メートル）を設定します.
		 */
		public ResultRadiusSearch(
			RimBody body, int latColumnNo, int lonColumnNo,
			RimIndexElement[] index, double lat, double lon, int redius) {
			// rimbody.
			this.body = body;
			// index情報.
			this.index = index;
			// 緯度列番号.
			this.latColumnNo = latColumnNo;
			// 経度列番号.
			this.lonColumnNo = lonColumnNo;
			
			// 緯度.
			this.srcLat = lat;
			this.srcLatM = GeoFastLine.calcLat(lat);
			// 経度.
			this.srcLon = lon;
			this.srcLonM = GeoFastLine.calcLon(lon);
			// 検索対象の半径(メートル).
			this.targetRedius = redius;
			
			// elementを初期化.
			this.element = null;
			// 検索範囲群の条件を生成.
			this.betweenList = new long[GeoQuad.SEARCH_QUAD_KEY_LENGTH];
			// 検索範囲を生成.
			GeoQuad.searchCode(
				this.betweenList, GeoQuad.getDetail(redius), lat, lon);
			
			// 読み込み位置の初期化.
			this.betweenPos = 0;
			this.indexPos = -2;
			this.indexEndPos = -1;
			this.elementPos = -1;
			
			// 今回取得情報をクリア.
			this.nowLineNo = -1;
			this.nowP2pRadius = -1;
			
			// 取得済み行管理情報.
			this.acquiredRowIdList = new IndexKeyList();
		}
		
		// 次の情報を読み込む.
		private final boolean nextGet() {
			// 既に読み込まれている場合.
			if(nextGetFlag) {
				return true;
			}
			int p2p;
			RimRow row;
			// 次の検索が見つかるまでループ.
			while(true) {
				
				// 要素が存在する場合はその要素の範囲を検索.
				if(element != null) {
					// 要素の範囲内の場合.
					if(elementPos + 1 < element.getLineLength() &&
						!acquiredRowIdList.add(element.getLineNo(elementPos + 1))) {
						elementPos ++;
						nowLineNo = element.getLineNo(elementPos);
						nextGetFlag = true;
						return true;
					}
					// element初期化.
					element = null;
					elementPos = -1;
				}
				
				// indexPosが有効な場合はその要素を取得.
				if(indexPos != -2) {
					// 今回のbetweenListの読み込みの範囲内の場合.
					if(indexPos < indexEndPos) {
						// 今回のElementを取得.
						element = index[++ indexPos];
						// 対象行のBody行情報を取得.
						row = body.getRow(element.getLineNo(0));
						// 中心点から、対象の緯度・経度までの距離を取得.
						p2p = GeoFastLine.get(srcLatM, srcLonM,
							GeoFastLine.calcLat(row.getDouble(latColumnNo)),
							GeoFastLine.calcLon(row.getDouble(lonColumnNo)));
						// 取得条件が指定した半径の範囲外の場合.
						if(p2p > targetRedius) {
							// このElementは処理しない.
							element = null;
							elementPos = -1;
						} else {
							// 今回分の中心点から対象点までの長さをセット.
							nowP2pRadius = p2p;
						}
						continue;
					}
					// 次の検索範囲群で処理.
					indexPos = -2;
					indexEndPos = -1;
				}
				
				// betweenPos が範囲外の場合.
				if(betweenPos >= GeoQuad.SEARCH_QUAD_KEY_LENGTH) {
					acquiredRowIdList = null;
					return false;
				}
				// インデックスの開始から終了位置まで取得.
				indexPos = SearchUtil.indexGE(index, betweenList[betweenPos]) - 1;
				// インデックス開始が正しく取得できた場合.
				if(indexPos != -2) {
					indexEndPos = SearchUtil.indexLE(index, betweenList[betweenPos + 1]);
				} else {
					indexPos = -2;
				}
				// 次のポジションに移動.
				betweenPos += 2;
			}
		}

		@Override
		public boolean hasNext() {
			return nextGet();
		}

		@Override
		public Integer next() {
			if(!nextGet()) {
				throw new NoSuchElementException();
			}
			nextGetFlag = false;
			return nowLineNo;
		}
		
		@Override
		public RimRow nextRow() {
			next();
			return body.getRow(nowLineNo);
		}

		@Override
		public Comparable getValue() {
			return nowP2pRadius;
		}

		@Override
		public int getLineNo() {
			return nowLineNo;
		}

		@Override
		public double getLat() {
			return body.getRow(nowLineNo)
				.getDouble(latColumnNo);
		}

		@Override
		public double getLon() {
			return body.getRow(nowLineNo)
				.getDouble(lonColumnNo);
		}
		
		@Override
		public double getStrictRedius() {
			return GeoLine.get(srcLat, srcLon, getLat(), getLon());
		}
	}
	
	/**
	 * 半径でソートを行う要素.
	 */
	private static final class ResultAscRadiusElement
		implements Comparable<ResultAscRadiusElement> {
		
		// 半径.
		protected int radius;
		// 行番号.
		protected int rowId;
		
		/**
		 * コンストラクタ.
		 * @param radius
		 * @param rowId
		 */
		public ResultAscRadiusElement(int radius, int rowId) {
			this.radius = radius;
			this.rowId = rowId;
		}
		
		@Override
		public int compareTo(ResultAscRadiusElement o) {
			return radius - o.radius;
		}
	}
	
	// 中心の緯度経度と半径を指定して範囲内の条件を
	// 半径ソートして検索.
	private static final class ResultAscRadiusSearch
		implements RimResultGeo {
		// RimBody.
		private final RimBody body;
		
		// 緯度の列番号.
		private final int latColumnNo;
		
		// 経度の列番号.
		private final int lonColumnNo;
		
		// 中心点の緯度.
		private final double srcLat;
		// 中心点の経度.
		private final double srcLon;
		
		// 昇順の場合はtrue.
		private final boolean ascFlag;
		
		// 半径でソートされた情報.
		private ObjectList<ResultAscRadiusElement> result;
		// resultの長さ.
		private final int resultLength;
		
		// 読み込み位置.
		private int position;
		
		// 現在読込中の要素.
		private ResultAscRadiusElement nowElement;
		
		
		/**
		 * コンストラクタ.
		 * @param ascFlag 昇順で情報を取得する場合は true.
		 * @param src ResultRadiusSearchを設定します.
		 */
		private ResultAscRadiusSearch(boolean ascFlag, ResultRadiusSearch src) {
			this.result = sortList(src);
			this.resultLength = result.size();
			this.body = src.body;
			this.latColumnNo = src.latColumnNo;
			this.lonColumnNo = src.lonColumnNo;
			this.srcLat = src.srcLat;
			this.srcLon = src.srcLon;
			this.ascFlag = ascFlag;
			this.position = (ascFlag) ? -1 :
				this.resultLength;
		}
		
		// ソート済みの検索結果リストを取得.
		private static final ObjectList<ResultAscRadiusElement> sortList(
			ResultRadiusSearch src) {
			ObjectList<ResultAscRadiusElement> ret =
				new ObjectList<ResultAscRadiusElement>();
			while(src.hasNext()) {
				src.next();
				ret.add(new ResultAscRadiusElement(
					(Integer)src.getValue(), src.getLineNo()));
			}
			if(ret.size() > 0) {
				ret.smart();
				ret.sort();
			}
			return ret;
		}
		
		@Override
		public boolean hasNext() {
			final boolean ret = ascFlag ?
				position + 1 < resultLength :
				position - 1 >= 0;
			if(!ret) {
				result = null;
			}
			return ret;
		}

		@Override
		public Integer next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}
			nowElement = ascFlag ?
				result.get(++ position) :
				result.get(-- position);
			return nowElement.rowId;
		}

		@Override
		public RimRow nextRow() {
			next();
			return body.getRow(nowElement.rowId);
		}

		@Override
		public Comparable getValue() {
			return nowElement.radius;
		}

		@Override
		public int getLineNo() {
			return nowElement.rowId;
		}

		@Override
		public double getLat() {
			RimRow row = body.getRow(nowElement.rowId);
			return row.getDouble(latColumnNo);
		}

		@Override
		public double getLon() {
			RimRow row = body.getRow(nowElement.rowId);
			return row.getDouble(lonColumnNo);
		}
		
		@Override
		public double getStrictRedius() {
			return GeoLine.get(srcLat, srcLon, getLat(), getLon());
		}
	}
}
