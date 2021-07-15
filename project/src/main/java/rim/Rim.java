package rim;

import rim.exception.RimException;
import rim.index.GeneralIndex;
import rim.index.GeoIndex;
import rim.index.NgramIndex;
import rim.util.IndexKeyValueList;

/**
 * Rimデータ.
 */
public class Rim {
	// Bodyデーター.
	private RimBody body;
	
	// インデックス群.
	private final IndexKeyValueList<Integer, GeneralIndex> indexs =
		new IndexKeyValueList<Integer, GeneralIndex>();
	
	// インデックス予定サイズ.
	private final int indexLength;
	
	// Geoインデックス群.
	private final IndexKeyValueList<Long, GeoIndex> geoIndexs =
		new IndexKeyValueList<Long, GeoIndex>();
	
	// Geoインデックス予定サイズ.
	private final int geoIndexLength;
	
	// Ngramインデックス群.
	private final IndexKeyValueList<Integer, NgramIndex> ngramIndexs =
		new IndexKeyValueList<Integer, NgramIndex>();
	
	// Ngramインデックス予定サイズ.
	private final int ngramIndexLength;
	
	// fixフラグ.
	private boolean fixFlag;
	
	/**
	 * コンストラクタ.
	 * @param body RimBodyを設定します.
	 * @param indexLength 登録予定のインデックス数を設定します.
	 * @param geoIndexLength 登録予定のGeoインデックス数を設定します.
	 * @param ngramIndexLength 登録予定のNgramインデックス数を設定します.
	 */
	public Rim(RimBody body, int indexLength, int geoIndexLength,
		int ngramIndexLength) {
		this.body = body;
		this.indexLength = indexLength;
		this.geoIndexLength = geoIndexLength;
		this.ngramIndexLength = ngramIndexLength;
		this.fixFlag = false;
	}
	
	/**
	 * インデックスを登録.
	 * @param columnNo 登録対象の列番号を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 * @return GeneralIndex 登録されたインデックスが返却されます.
	 */
	protected GeneralIndex registerIndex(int columnNo, int planIndexSize) {
		if(indexs.size() >= indexLength) {
			throw new RimException("The number of indexes to be registered ("
				+ indexs.size() + ") has been exceeded: " + indexLength);
		}
		GeneralIndex index = body.createIndex(columnNo, planIndexSize);
		indexs.put(columnNo, index);
		return index;
	}
	
	/**
	 * Geoインデックスを登録.
	 * @param latColumnNo 登録対象の緯度列番号を設定します.
	 * @param lonColumnNo 登録対象の経度列番号を設定します.
	 * @param planIndexSize このGeoインデックスの予定登録行数を設定します.
	 * @return GeoIndex 登録されたGeoインデックスが返却されます.
	 */
	protected GeoIndex registerGeoIndex(int latColumnNo, int lonColumnNo,
		int planIndexSize) {
		if(geoIndexs.size() >= geoIndexLength) {
			throw new RimException(
				"The number of geo indexes to be registered (" +
				geoIndexs.size() + ") has been exceeded: " +
				geoIndexLength);
		}
		GeoIndex index = body.createGeoIndex(
			latColumnNo, lonColumnNo, planIndexSize);
		geoIndexs.put(getGeoKey(latColumnNo, lonColumnNo), index);
		return index;
	}
	
	/**
	 * インデックスを登録.
	 * @param columnNo 登録対象の列番号を設定します.
	 * @param ngramLength Ngram長を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 * @return NgarmIndex 登録されたNgramインデックスが返却されます.
	 */
	protected NgramIndex registerNgramIndex(int columnNo, int ngramLength,
		int planIndexSize) {
		if(indexs.size() >= indexLength) {
			throw new RimException("The number of ngram indexes to be registered ("
				+ indexs.size() + ") has been exceeded: " + indexLength);
		}
		NgramIndex index = body.createNgramIndex(columnNo, ngramLength, planIndexSize);
		ngramIndexs.put(columnNo, index);
		return index;
	}
	
	/**
	 * Bodyと登録インデックス群の追加処理がすべて完了した場合に
	 * 呼び出します.
	 */
	protected void fix() {
		if(fixFlag) {
			return;
		} else if(!body.isFix()) {
			body.fix();
		}
		// インデックスをFixする.
		int len = indexs.size();
		// 長さが一致しない場合.
		if(indexLength != len) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
		// インデックスをFixする.
		for(int i = 0; i < len; i ++) {
			indexs.valueAt(i).fix();
		}
		
		// GeoインデックスをFixする.
		len = geoIndexs.size();
		// 長さが一致しない場合.
		if(geoIndexLength != len) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
		// GeoインデックスをFixする.
		for(int i = 0; i < len; i ++) {
			geoIndexs.valueAt(i).fix();
		}
		
		// NgramインデックスをFixする.
		len = ngramIndexs.size();
		// 長さが一致しない場合.
		if(ngramIndexLength != len) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
		// NgramインデックスをFixする.
		for(int i = 0; i < len; i ++) {
			ngramIndexs.valueAt(i).fix();
		}
		
		// Fixを完了.
		fixFlag = true;
	}

	/**
	 * 追加処理がFixしているか取得.
	 * @return boolean trueの場合Fixしています.
	 */
	public boolean isFix() {
		return fixFlag;
	}

	/**
	 * Rimデーター読み込みが完了しているかチェック.
	 */
	protected void checkFix() {
		if(!isFix()) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
	}

	/**
	 * Body情報を取得.
	 * @return RimBody Body情報が返却されます.
	 */
	public RimBody getBody() {
		checkFix();
		return body;
	}
	
	/**
	 * インデックス数を取得.
	 * @return int インデックス数が返却されます.
	 */
	public int getIndexSize() {
		checkFix();
		return indexs.size();
	}
	
	/**
	 * インデックス列名群を取得.
	 * @return String[] インデックス列名群が返却されます.
	 */
	public String[] getIndexColumns() {
		checkFix();
		final int len = indexs.size();
		String[] ret = new String[len];
		
		for(int i = 0; i < len; i ++) {
			ret[i] = body.getColumnName(indexs.keyAt(i));
		}
		return ret;
	}
	
	/**
	 * 列項番を指定してインデックスを取得.
	 * @param columnNo 取得したいインデックスの列項番を設定します.
	 * @return GeneralIndex インデックスが返却されます.
	 */
	public GeneralIndex getIndex(int columnNo) {
		checkFix();
		GeneralIndex ret = indexs.get(columnNo);
		if(ret == null) {
			throw new RimException(
				"The specified index does not exist (column: " +
				columnNo + ")");
		}
		return ret;
	}
	
	/**
	 * 列名を設定してインデックスを取得.
	 * @param column 列名を設定します.
	 * @return GeneralIndex インデックスが返却されます.
	 */
	public GeneralIndex getIndex(String column) {
		checkFix();
		final int columnNo = body.getColumnNo(column);
		GeneralIndex ret = indexs.get(columnNo);
		if(ret == null) {
			throw new RimException(
					"The specified index does not exist (column: " +
					column + ")");
		}
		return ret;
	}
	
	/**
	 * Geoインデックス数を取得.
	 * @return int Geoインデックス数が返却されます.
	 */
	public int getGeoIndexSize() {
		checkFix();
		return geoIndexs.size();
	}
	
	/**
	 * Geoインデックス列名群を取得.
	 * @return String[] Geoインデックス列名群が返却されます.
	 *                  String[緯度列名, 経度列名, .... ]で返却されます.
	 */
	public String[] getGeoIndexColumns() {
		checkFix();
		final int[] latLon = new int[2];
		final int len = geoIndexs.size();
		String[] ret = new String[len * 2];
		
		for(int i = 0, j = 0; i < len; i ++, j += 2) {
			getLatLon(latLon, geoIndexs.keyAt(i));
			ret[j] = body.getColumnName(latLon[0]);
			ret[j + 1] = body.getColumnName(latLon[1]);
		}
		return ret;
	}

	
	/**
	 * 緯度・経度の列項番を指定してGeoインデックスを取得.
	 * @param latColumnNo 取得したいインデックスの緯度列項番を設定します.
	 * @param lonColumnNo 取得したいインデックスの経度列項番を設定します.
	 * @return RimGeoIndex Geoインデックスが返却されます.
	 */
	public GeoIndex getGeoIndex(int latColumnNo, int lonColumnNo) {
		checkFix();
		GeoIndex ret = geoIndexs.get(getGeoKey(latColumnNo, lonColumnNo));
		if(ret == null) {
			throw new RimException(
				"The specified Geo index does not exist (latitude: " +
				latColumnNo + ", longitude:" + lonColumnNo + ")");
		}
		return ret;
	}
	
	/**
	 * 列名を設定してGeoインデックスを取得.
	 * @param column 列名を設定します.
	 * @return RimGeoIndex Geoインデックスが返却されます.
	 */
	public GeoIndex getGeoIndex(String latColumn, String lonColumn) {
		checkFix();
		final int latColumnNo = body.getColumnNo(latColumn);
		final int lonColumnNo = body.getColumnNo(lonColumn);
		GeoIndex ret = geoIndexs.get(getGeoKey(latColumnNo, lonColumnNo));
		if(ret == null) {
			throw new RimException(
				"The specified Geo index does not exist (latitude: " +
				latColumn + ", longitude:" + lonColumn + ")");
		}
		return ret;
	}
	
	// geoKeyを取得.
	private static final long getGeoKey(int latColumnNo, int lonColumnNo) {
		return ((long)latColumnNo << 32L) | ((long)lonColumnNo);
	}
	
	// geoKeyから緯度、経度を取得.
	private static final void getLatLon(int[] out, long geoKey) {
		out[0] = (int)((geoKey & 0xffffffff00000000L) >> 32L);
		out[1] = (int)(geoKey & 0x00000000ffffffffL);
	}
	
	/**
	 * Ngramインデックス数を取得.
	 * @return int Ngramインデックス数が返却されます.
	 */
	public int getNgramIndexSize() {
		checkFix();
		return ngramIndexs.size();
	}
	
	/**
	 * インデックス列名群を取得.
	 * @return String[] インデックス列名群が返却されます.
	 */
	public String[] getNgramIndexColumns() {
		checkFix();
		final int len = ngramIndexs.size();
		String[] ret = new String[len];
		
		for(int i = 0; i < len; i ++) {
			ret[i] = body.getColumnName(ngramIndexs.keyAt(i));
		}
		return ret;
	}
	
	/**
	 * 列項番を指定してNgramインデックスを取得.
	 * @param columnNo 取得したいNgramインデックスの列項番を設定します.
	 * @return NgramIndex Ngramインデックスが返却されます.
	 */
	public NgramIndex getNgramIndex(int columnNo) {
		checkFix();
		NgramIndex ret = ngramIndexs.get(columnNo);
		if(ret == null) {
			throw new RimException(
				"The specified ngram index does not exist (column: " +
				columnNo + ")");
		}
		return ret;
	}
	
	/**
	 * 列名を設定してNgramインデックスを取得.
	 * @param column 列名を設定します.
	 * @return NgramIndex Ngramインデックスが返却されます.
	 */
	public NgramIndex getNgramIndex(String column) {
		checkFix();
		final int columnNo = body.getColumnNo(column);
		NgramIndex ret = ngramIndexs.get(columnNo);
		if(ret == null) {
			throw new RimException(
				"The specified ngram index does not exist (column: " +
				column + ")");
		}
		return ret;
	}

}
