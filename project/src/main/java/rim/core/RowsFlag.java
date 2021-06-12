package rim.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import rim.exception.RimException;

/**
 * 行番号群のON / OFF管理.
 */
public class RowsFlag {
	private Flags[] rowsFlag;
	private int length;
	
	/**
	 * コンストラクタ.
	 * @param length 全行数を設定します..
	 */
	public RowsFlag(int length) {
		final int flagsLength = (length >> 14) +
			((length & 0x00003fff) == 0 ? 0 : 1);
		this.length = length;
		this.rowsFlag = new Flags[flagsLength];
	}

	/**
	 * クリア.
	 * @return RowsFlag このオブジェクトが返却されます.
	 */
	public RowsFlag clear() {
		Arrays.fill(rowsFlag, null);
		return this;
	}
	
	/**
	 * 指定行番号に対するON / OFFをセット.
	 * @param no 番号を設定します.
	 * @param flag 設定するフラグ条件を設定します.
	 * @return RowsFlag このオブジェクトが返却されます.
	 */
	public RowsFlag put(int no, boolean flag) {
		if(no < 0 || no >= length) {
			throw new RimException("Out of range (" +
				length + " : " + no + "). ");
		}
		Flags flags = rowsFlag[no >> 14];
		if(flag) {
			if(flags == null) {
				if(rowsFlag.length == (no >> 14) + 1) {
					flags = new Flags(length & 0x00003fff);
				} else {
					flags = new Flags(0x00003fff);
				}
				rowsFlag[no >> 14] = flags;
			}
			flags.put(no - ((no >> 14) << 14), true);
		} else {
			if(flags != null) {
				flags.put(no - ((no >> 14) << 14), false);
			}
		}
		return this;
	}
	
	/**
	 * 指定行番号のON / OFFの取得.
	 * @param no 番号を設定します.
	 * @return boolean 設定されているフラグが返却されます.
	 */
	public boolean get(int no) {
		if(no < 0 || no >= length) {
			throw new RimException("Out of range (" +
				length + " : " + no + "). ");
		}
		final Flags flags = rowsFlag[no >> 14];
		if(flags == null) {
			return false;
		}
		return flags.get(no - ((no >> 14) << 14));
	}
	
	/**
	 * フラグ管理長を取得.
	 * @return int フラグ管理数が返却されます.
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * AND処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとANDするためのRowsFlagを設定します.
	 * @return RowsFlag このオブジェクトが返却されます.
	 */
	public RowsFlag and(RowsFlag value) {
		Flags[] v = value.rowsFlag;
		int len = v.length;
		if(len != rowsFlag.length) {
			throw new RimException("Must be the same length.");
		}
		for(int i = 0; i < len; i ++) {
			if(rowsFlag[i] != null) {
				if(v[i] == null) {
					rowsFlag[i] = null;
				} else {
					rowsFlag[i].and(v[i]);
				}
			}
		}
		return this;
	}
	
	/**
	 * OR処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとORするためのRowsFlagを設定します.
	 * @return RowsFlag このオブジェクトが返却されます.
	 */
	public RowsFlag or(RowsFlag value) {
		Flags[] v = value.rowsFlag;
		int len = v.length;
		if(len != rowsFlag.length) {
			throw new RimException("Must be the same length.");
		}
		for(int i = 0; i < len; i ++) {
			if(rowsFlag[i] == null) {
				if(v[i] != null) {
					rowsFlag[i] = v[i].copy();
				}
			} else if(v[i] != null) {
				rowsFlag[i].or(v[i]);
			}
		}
		return this;
	}
	
	/**
	 * オブジェクトを別のインスタンスでコピー.
	 * @return RowsFlag コピーされたオブジェクトが返却されます.
	 */
	public RowsFlag copy() {
		final RowsFlag ret = new RowsFlag(length);
		final Flags[] lst = ret.rowsFlag;
		final int len = rowsFlag.length;
		for(int i = 0; i < len; i ++) {
			if(rowsFlag[i] != null) {
				lst[i] = rowsFlag[i].copy();
			}
		}
		return ret;
	}
	
	/**
	 * 有効な行番号を取得するIteratorを取得.
	 * @param ascFlag 昇順取得の場合は true.
	 * @return Iterator<Integer> 有効な行番号を返却する
	 *                           Iteratorが返却されます.
	 */
	public Iterator<Integer> iterator(boolean ascFlag) {
		return new RowsIterator(ascFlag, rowsFlag);
	}
	
	private static final class RowsIterator
		implements Iterator<Integer> {
		// 昇順の場合 true.
		private boolean ascFlag;
		// RowsFlagオブジェクトが管理しているFlasg配列.
		private Flags[] rowsFlag;
		// 現在読込中のFlags配列位置.
		private int rowsPos;
		// 現在読込中のFlags内の位置.
		private int arrayPos;
		// 現在読み込み中のFlags内の位置内の６４フラグの取得位置.
		private int oneFlagPos;
		// nowGetで情報取得が正しく行われた場合 true.
		private boolean nowGetFlag;
		// rowsPosとarrayPosを受け取る情報.
		private final int[] out = new int[2];
		
		/**
		 * コンストラクタ.
		 * @param ascFlag 昇順の場合は true.
		 * @param lst Flags[] を設定します.
		 */
		RowsIterator(boolean ascFlag, Flags[] lst) {
			final int len = lst.length;
			// 最初のON条件を取得.
			nextRowsPosAndArrayPos(
				out, ascFlag, lst, ascFlag ? 0 : len - 1
			);
			this.ascFlag = ascFlag;
			this.rowsFlag = lst;
			this.rowsPos = out[0];
			this.arrayPos = out[1];
			this.oneFlagPos = ascFlag ? - 1 : Flags.getOneArrayLength();
			this.nowGetFlag = false;
		}
		
		/**
		 * 次のRowPosとArrayPosを取得.
		 * @param out [0]: RowPos, [1]: ArrayPos.
		 * @param ascFlag 昇順の場合は true.
		 * @param rowsFlag Flags[] を設定します.
		 * @param nextRowsPos 次のRowPosを設定します.
		 * @return boolean trueの場合は条件が見つかりました.
		 */
		private static final boolean nextRowsPosAndArrayPos(
			int[] out, boolean ascFlag, Flags[] rowsFlag, int nextRowsPos) {
			int pos;
			if(ascFlag) {
				final int len = rowsFlag.length;
				// rowsFlagから設定されているFlagsを取得.
				for(int i = nextRowsPos; i < len; i ++) {
					// 空で無いFlagsが見つかった場合.
					if(rowsFlag[i] != null) {
						// 次のON条件があるFlag配列位置条件を探す.
						pos = rowsFlag[i].searchOnByGetArrayPos(true, 0);
						if(pos != -1) {
							// 存在する場合.
							out[0] = i;
							out[1] = pos;
							return true;
						}
					}
				}
				// 終端を検出.
				out[0] = len;
				out[1] = -1;
			} else {
				// rowsFlagから設定されているFlagsを取得.
				for(int i = nextRowsPos; i >= 0; i --) {
					// 空で無いFlagsが見つかった場合.
					if(rowsFlag[i] != null) {
						// 次のON条件があるFlag配列位置条件を探す.
						pos = rowsFlag[i].searchOnByGetArrayPos(
							false, rowsFlag[i].getArrayLength() - 1);
						if(pos != -1) {
							// 存在する場合.
							out[0] = i;
							out[1] = pos;
							return true;
						}
					}
				}
				// 終端を検出.
				out[0] = -1;
				out[1] = -1;
			}
			return false;
		}
		
		// 次の情報を読み込む.
		private final boolean nowGet() {
			// next呼び出し以外でこの呼出を行った場合.
			if(nowGetFlag) {
				return true;
			}
			int pos;
			Flags flags;
			// 昇順.
			if(ascFlag) {
				final int len = rowsFlag.length;
				while(true) {
					// 終端まで読み込んでる場合.
					if(rowsPos >= len) {
						return false;
					}
					// 対象のフラグ情報を取得.
					flags = rowsFlag[rowsPos];
					
					// フラグがONの位置を取得.
					pos = flags.searchOnByGetOneFlag(
						true, arrayPos, oneFlagPos + 1);
					if(pos != -1) {
						// 存在する場合.
						oneFlagPos = pos;
						nowGetFlag = true;
						return true;
					}
					// 初期位置.
					oneFlagPos = -1;
					
					// 取得できない場合、次のON条件があるFlag配列位置条件を探す.
					pos = flags.searchOnByGetArrayPos(true, arrayPos + 1);
					if(pos != -1) {
						// 存在する場合.
						arrayPos = pos;
						continue;
					}
					
					// 次のRowsPosとArrayPos位置を取得.
					if(nextRowsPosAndArrayPos(out, ascFlag, rowsFlag, rowsPos + 1)) {
						// 存在する場合.
						rowsPos = out[0];
						arrayPos = out[1];
					} else {
						// 終端まで読み込んだ場合.
						rowsPos = len;
						return false;
					}
				}
			// 降順.
			} else {
				while(true) {
					// 終端まで読み込んでる場合.
					if(rowsPos < 0) {
						return false;
					}
					// 対象のフラグ情報を取得.
					flags = rowsFlag[rowsPos];
					
					// フラグがONの位置を取得.
					pos = flags.searchOnByGetOneFlag(
						false, arrayPos, oneFlagPos - 1);
					if(pos != -1) {
						// 存在する場合.
						oneFlagPos = pos;
						nowGetFlag = true;
						return true;
					}
					// 初期位置.
					oneFlagPos = Flags.getOneArrayLength();
					
					// 取得できない場合、次のON条件があるFlag配列位置条件を探す.
					pos = flags.searchOnByGetArrayPos(false, arrayPos - 1);
					if(pos != -1) {
						// 存在する場合.
						arrayPos = pos;
						continue;
					}
					
					// 次のRowsPosとArrayPos位置を取得.
					if(nextRowsPosAndArrayPos(out, ascFlag, rowsFlag, rowsPos - 1)) {
						// 存在する場合.
						rowsPos = out[0];
						arrayPos = out[1];
					} else {
						// 終端まで読み込んだ場合.
						rowsPos = -1;
						return false;
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return nowGet();
		}

		@Override
		public Integer next() {
			if(!nowGet()) {
				throw new NoSuchElementException();
			}
			nowGetFlag = false;
			return ((rowsPos << 14) + (arrayPos << 6) + oneFlagPos);
		}
	}
	
	/**
	public static final void main(String[] args) {
		final RowsFlag rowsFlag = new RowsFlag(5000000);
		
		final int[] posList = new int[] {
			0, 3332, 10000, 50000, 88000, 325442, 988824, 3152001, 4243562, 4999999
		};
		final int len = posList.length;
		for(int i = 0; i < len; i ++) {
			rowsFlag.put(posList[i], true);
		}
		
		Iterator<Integer> itr = rowsFlag.iterator(false);
		while(itr.hasNext()) {
			System.out.println(itr.next());
		}
		
		System.out.println();
		
		int rowsLen = rowsFlag.getLength();
		for(int i = 0; i < rowsLen; i ++) {
			if(rowsFlag.get(i)) {
				System.out.println(i);
			}
		}
	}
	**/
}
