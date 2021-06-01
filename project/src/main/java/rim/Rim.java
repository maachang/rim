package rim;

import rim.exception.RimException;

/**
 * Rimデータ.
 */
public class Rim {
	// Bodyデーター.
	private RimBody body;
	// インデックスデーター.
	private RimIndex[] indexs;
	// インデックス生成項番.
	private int createIndexAddCount;
	// fixフラグ.
	private boolean fixFlag;
	
	/**
	 * コンストラクタ.
	 * @param columns 列名群を設定します.
	 * @param types 列タイプを設定します.
	 * @param rowLength 総行数を設定します.
	 * @param indexLength 登録されてるインデックス数を設定します.
	 */
	protected Rim(String[] columns, ColumnType[] types, int rowLength,
		int indexLength) {
		this.body = new RimBody(columns, types, rowLength);
		this.indexs = new RimIndex[indexLength];
		this.createIndexAddCount = 0;
		this.fixFlag = false;
	}
	
	/**
	 * Body情報を取得.
	 * @return RimBody Body情報が返却されます.
	 */
	protected RimBody getBody() {
		return body;
	}
	
	/**
	 * インデックスを登録.
	 * @param columnNo 登録対象のインデックス列番号を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 * @return RimIndex 登録されたインデックスが返却されます.
	 */
	protected RimIndex registerIndex(int columnNo, int planIndexSize) {
		if(indexs.length <= createIndexAddCount) {
			throw new RimException("The number of indexes to be registered ("
				+ indexs.length + ") has been exceeded: " + createIndexAddCount);
		}
		RimIndex index = new RimIndex(columnNo,
			body.getColumnName(columnNo),
			body.getColumnType(columnNo),
			body.getColumnLength(),
			planIndexSize);
		indexs[createIndexAddCount ++] = index;
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
		int len = indexs.length;
		for(int i = 0; i < len; i ++) {
			if(indexs[i] == null) {
				throw new RimException(
					"Rim data reading is not complete.");
			} else if(!indexs[i].isFix()) {
				indexs[i].fix();
			}
		}
		fixFlag = true;
	}

	/**
	 * 追加処理がFixしているか取得.
	 * @return boolean trueの場合Fixしています.
	 */
	protected boolean isFix() {
		return fixFlag;
	}

	/**
	 * Rimデーター読み込みが完了しているかチェック.
	 */
	protected void checkFix() {
		if(!isFix()) {
			throw new RimException("Rim data reading is not complete.");
		}
	}

}
