package rim;

import rim.core.RimBody;
import rim.core.RimIndex;
import rim.exception.RimException;
import rim.util.FixedSearchArray;

/**
 * Rimデータ.
 */
public class Rim {
	// Bodyデーター.
	private RimBody body;
	// インデックスデーター.
	private RimIndex[] indexs;
	// インデックスを列名に紐付けた情報.
	private FixedSearchArray<String> indexColumnNameList;
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
	public Rim(RimBody body, int indexLength) {
		this.body = body;
		this.indexs = new RimIndex[indexLength];
		this.createIndexAddCount = 0;
		this.fixFlag = false;
	}
	
	/**
	 * インデックスを登録.
	 * @param columnNo 登録対象のインデックス列番号を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 * @return RimIndex 登録されたインデックスが返却されます.
	 */
	public RimIndex registerIndex(int columnNo, int planIndexSize) {
		if(indexs.length <= createIndexAddCount) {
			throw new RimException("The number of indexes to be registered ("
				+ indexs.length + ") has been exceeded: " + createIndexAddCount);
		}
		RimIndex index = body.createIndex(columnNo, planIndexSize);
		indexs[createIndexAddCount ++] = index;
		return index;
	}
	
	/**
	 * Bodyと登録インデックス群の追加処理がすべて完了した場合に
	 * 呼び出します.
	 */
	public void fix() {
		if(fixFlag) {
			return;
		} else if(!body.isFix()) {
			body.fix();
		}
		final int len = indexs.length;
		final FixedSearchArray<String> cnames =
			new FixedSearchArray<String>(len);
		for(int i = 0; i < len; i ++) {
			if(indexs[i] == null) {
				throw new RimException(
					"Rim data reading is not complete.");
			} else if(!indexs[i].isFix()) {
				indexs[i].fix();
				cnames.add(indexs[i].getColumnName(), i);
			}
		}
		indexColumnNameList = cnames;
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
			throw new RimException("Rim data reading is not complete.");
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
		return indexs.length;
	}
	
	/**
	 * 番号を指定してインデックスを取得.
	 * @param no 取得したいインデックスの項番を設定します.
	 * @return RimIndex インデックスが返却されます.
	 */
	public RimIndex getIndex(int no) {
		checkFix();
		if(no < 0 || no >= indexs.length) {
			throw new RimException(
				"The specified index number (" +
				no + ") is out of range.");
		}
		return indexs[no];
	}
	
	/**
	 * 列名を設定してインデックスを取得.
	 * @param column 列名を設定します.
	 * @return RimIndex インデックスが返却されます.
	 */
	public RimIndex getIndex(String column) {
		checkFix();
		int no = indexColumnNameList.search(column);
		if(no == -1) {
			throw new RimException(
				"The specified column name \"" +
				column + "\" does not exist in the index. ");
		}
		return indexs[no];
	}

}
