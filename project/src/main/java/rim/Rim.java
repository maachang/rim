package rim;

import rim.exception.RimException;
import rim.index.RimGeoIndex;
import rim.index.RimIndex;
import rim.util.IndexKeyValueList;

/**
 * Rimデータ.
 */
public class Rim {
	// Bodyデーター.
	private RimBody body;
	
	// インデックス群.
	private final IndexKeyValueList<Integer, RimIndex> indexs =
		new IndexKeyValueList<Integer, RimIndex>();
	
	// インデックス予定サイズ.
	private final int indexLength;
	
	// Geoインデックス群.
	private final IndexKeyValueList<Long, RimGeoIndex> geoIndexs =
		new IndexKeyValueList<Long, RimGeoIndex>();
	
	// Geoインデックス予定サイズ.
	private final int geoIndexLength;
	
	// fixフラグ.
	private boolean fixFlag;
	
	/**
	 * コンストラクタ.
	 * @param body RimBodyを設定します.
	 * @param indexLength 登録予定のインデックス数を設定します.
	 * @param geoIndexLength 登録予定のGeoインデックス数を設定します.
	 */
	public Rim(RimBody body, int indexLength, int geoIndexLength) {
		this.body = body;
		this.indexLength = indexLength;
		this.geoIndexLength = geoIndexLength;
		this.fixFlag = false;
	}
	
	/**
	 * インデックスを登録.
	 * @param columnNo 登録対象の列番号を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 * @return RimIndex 登録されたインデックスが返却されます.
	 */
	protected RimIndex registerIndex(int columnNo, int planIndexSize) {
		if(indexs.size() >= indexLength) {
			throw new RimException("The number of indexes to be registered ("
				+ indexs.size() + ") has been exceeded: " + indexLength);
		}
		RimIndex index = body.createIndex(columnNo, planIndexSize);
		indexs.put(columnNo, index);
		return index;
	}
	
	/**
	 * Geoインデックスを登録.
	 * @param latColumnNo 登録対象の緯度列番号を設定します.
	 * @param lonColumnNo 登録対象の経度列番号を設定します.
	 * @param planIndexSize このGeoインデックスの予定登録行数を設定します.
	 * @return RimGeoIndex 登録されたGeoインデックスが返却されます.
	 */
	protected RimGeoIndex registerGeoIndex(int latColumnNo, int lonColumnNo,
		int planIndexSize) {
		if(geoIndexs.size() != geoIndexLength) {
			throw new RimException(
				"The number of geo indexes to be registered (" +
				indexs.size() + ") has been exceeded: " +
				geoIndexLength);
		}
		RimGeoIndex index = body.createGeoIndex(
			latColumnNo, lonColumnNo, planIndexSize);
		geoIndexs.put(getGeoKey(latColumnNo, lonColumnNo), index);
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
		len = indexs.size();
		// 長さが一致しない場合.
		if(geoIndexLength != len) {
			throw new RimException(
				"Rim data reading is not complete.");
		}
		// インデックスをFixする.
		for(int i = 0; i < len; i ++) {
			geoIndexs.valueAt(i).fix();
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
	 * 列項番を指定してインデックスを取得.
	 * @param columnNo 取得したいインデックスの列項番を設定します.
	 * @return RimIndex インデックスが返却されます.
	 */
	public RimIndex getIndex(int columnNo) {
		checkFix();
		if(columnNo < 0 || columnNo >= indexs.size()) {
			throw new RimException(
				"The specified index column number (" +
					columnNo + ") is out of range.");
		}
		return indexs.get(columnNo);
	}
	
	/**
	 * 列名を設定してインデックスを取得.
	 * @param column 列名を設定します.
	 * @return RimIndex インデックスが返却されます.
	 */
	public RimIndex getIndex(String column) {
		checkFix();
		final int columnNo = body.getColumnNo(column);
		if(columnNo == -1) {
			throw new RimException(
				"The specified column name \"" +
				column + "\" does not exist in the index.");
		}
		return indexs.get(columnNo);
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
	 * 緯度・経度の列項番を指定してGeoインデックスを取得.
	 * @param latColumnNo 取得したいインデックスの緯度列項番を設定します.
	 * @param lonColumnNo 取得したいインデックスの経度列項番を設定します.
	 * @return RimGeoIndex Geoインデックスが返却されます.
	 */
	public RimGeoIndex getGeoIndex(int latColumnNo, int lonColumnNo) {
		checkFix();
		if(latColumnNo < 0 || latColumnNo >= geoIndexs.size()) {
			throw new RimException(
				"The specified latitude index column number (" +
					latColumnNo + ") is out of range.");
		} else if(lonColumnNo < 0 || lonColumnNo >= geoIndexs.size()) {
			throw new RimException(
				"The specified longitude index column number (" +
					lonColumnNo + ") is out of range.");
		}
		return geoIndexs.get(getGeoKey(latColumnNo, lonColumnNo));
	}
	
	/**
	 * 列名を設定してGeoインデックスを取得.
	 * @param column 列名を設定します.
	 * @return RimGeoIndex Geoインデックスが返却されます.
	 */
	public RimGeoIndex getGeoIndex(String latColumn, String lonColumn) {
		checkFix();
		final int latColumnNo = body.getColumnNo(latColumn);
		if(latColumnNo == -1) {
			throw new RimException(
				"The specified latitude column name \"" +
				latColumn + "\" does not exist in the index.");
		}
		final int lonColumnNo = body.getColumnNo(lonColumn);
		if(lonColumnNo == -1) {
			throw new RimException(
				"The specified latitude column name \"" +
				lonColumn + "\" does not exist in the index.");
		}
		return geoIndexs.get(getGeoKey(latColumnNo, lonColumnNo));
	}
	
	// geoKeyを取得.
	private static final long getGeoKey(int latColumnNo, int lonColumnNo) {
		return ((long)latColumnNo << 32L) | ((long)lonColumnNo);
	}
}
