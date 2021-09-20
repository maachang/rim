package rim.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

import rim.core.Flags.FlagsIterator;
import rim.exception.RimException;
import rim.util.IndexKeyValueList;

/**
 * メモリー効率を考慮したフラグ管理.
 * 内部でIndexKeyValueListを使ってるので、追加系は遅くなります.
 * その代わり配列で管理じゃないので、AndやOr等は高速動作して
 * メモリの消費も抑えられます.
 */
public class SmartFlags implements BaseFlags<SmartFlags> {
	// 1つのフラグ管理サイズは５１２単位.
	private static final int FLAGS_MASK = 0x000001ff;
	private static final int FLAGS_BIT = 9;
	
	// フラグを管理するオブジェクト.
	private IndexKeyValueList<Integer, Flags> flagArray;
	// フラグを管理するオブジェクトの最大数.
	private int flagsLength;
	// フラグ全体の配列長.
	private int length;
	
	/**
	 * コンストラクタ.
	 * @param length 全行数を設定します..
	 */
	public SmartFlags(int length) {
		this.length = length;
		this.flagsLength = (length >> FLAGS_BIT) +
				((length & FLAGS_MASK) == 0 ? 0 : 1);
		int initLen = flagsLength / 16;
		if(initLen < 8) {
			initLen = 8;
		}
		this.flagArray = new IndexKeyValueList<Integer, Flags>(
			initLen);
	}

	/**
	 * クリア.
	 * @return SmartFlags このオブジェクトが返却されます.
	 */
	public SmartFlags clear() {
		this.flagArray = new IndexKeyValueList<Integer, Flags>();
		return this;
	}
	
	/**
	 * 指定行番号に対するON / OFFをセット.
	 * @param no 番号を設定します.
	 * @param flag 設定するフラグ条件を設定します.
	 * @return SmartFlags このオブジェクトが返却されます.
	 */
	public SmartFlags put(int no, boolean flag) {
		if(no < 0 || no >= length) {
			throw new RimException("Out of range (" +
				length + " : " + no + "). ");
		}
		Flags flags = flagArray.get(no >> FLAGS_BIT);
		if(flag) {
			if(flags == null) {
				if(flagsLength == (no >> FLAGS_BIT) + 1) {
					flags = new Flags(length & FLAGS_MASK);
				} else {
					flags = new Flags(FLAGS_MASK);
				}
				flagArray.put(no >> FLAGS_BIT, flags);
			}
			flags.put(no - ((no >> FLAGS_BIT) << FLAGS_BIT), true);
		} else if(flags != null) {
			flags.put(no - ((no >> FLAGS_BIT) << FLAGS_BIT), false);
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
		final Flags flags = flagArray.get(no >> FLAGS_BIT);
		if(flags == null) {
			return false;
		}
		return flags.get(no - ((no >> FLAGS_BIT) << FLAGS_BIT));
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
	 * @return SmartFlags このオブジェクトが返却されます.
	 */
	public SmartFlags and(SmartFlags value) {
		if(value.flagsLength != flagsLength) {
			throw new RimException("Must be the same length.");
		}
		Integer key;
		Flags vValue;
		final IndexKeyValueList<Integer, Flags> v = value.flagArray;
		final int len = v.size();
		for(int i = len - 1; i >= 0; i --) {
			key = flagArray.keyAt(i);
			if((vValue = v.get(key)) == null) {
				flagArray.remove(key);
			} else {
				flagArray.get(key).and(vValue);
			}
		}
		return this;
	}
	
	/**
	 * OR処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとORするためのRowsFlagを設定します.
	 * @return SmartFlags このオブジェクトが返却されます.
	 */
	public SmartFlags or(SmartFlags value) {
		if(value.flagsLength != flagsLength) {
			throw new RimException("Must be the same length.");
		}
		Integer key;
		Flags f;
		final IndexKeyValueList<Integer, Flags> v = value.flagArray;
		final int len = v.size();
		for(int i = 0; i < len; i ++) {
			if((f = flagArray.get(key = v.keyAt(i))) == null) {
				flagArray.put(key, v.valueAt(i).copy());
			} else {
				f.or(v.valueAt(i));
			}
		}
		return this;
	}
	
	/**
	 * オブジェクトを別のインスタンスでコピー.
	 * @return SmartFlags コピーされたオブジェクトが返却されます.
	 */
	public SmartFlags copy() {
		final SmartFlags ret = new SmartFlags(length);
		final IndexKeyValueList<Integer, Flags> lst = ret.flagArray;
		final int len = flagArray.size();
		for(int i = 0; i < len; i ++) {
			lst.put(flagArray.keyAt(i), flagArray.valueAt(i).copy());
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
		return new RowsIterator(ascFlag, flagArray);
	}

	// RowsIterator.
	private static final class RowsIterator
		implements Iterator<Integer> {
		// 昇順の場合 true.
		private boolean ascFlag;
		// RowsFlagオブジェクトが管理しているFlasg配列.
		private IndexKeyValueList<Integer, Flags> flagArray;
		// 現在読込中のFlags配列位置.
		private int rowsPos;
		private int srcPos;
		// nowGetで情報取得が正しく行われた場合 true.
		private boolean nowGetFlag;
		// 元のFlagIterator.
		private FlagsIterator src;
		// 利用中のFlagsIterator.
		private FlagsIterator inUse;
		
		/**
		 * コンストラクタ.
		 * @param ascFlag 昇順の場合は true.
		 * @param lst Flags[] を設定します.
		 */
		RowsIterator(boolean ascFlag, IndexKeyValueList<Integer, Flags> lst) {
			this.ascFlag = ascFlag;
			this.flagArray = lst;
			this.nowGetFlag = false;
			this.rowsPos = ascFlag ? -1 : lst.size();
			this.src = new FlagsIterator();
			this.inUse = null;
			nowGet();
		}
		
		// 次の情報を読み込む.
		private final boolean nowGet() {
			// next呼び出し以外でこの呼出を行った場合.
			if(nowGetFlag) {
				return true;
			}
			// 利用中のFlagsのIteratorが存在する場合.
			if(inUse != null) {
				if(inUse.hasNext()) {
					nowGetFlag = true;
					return true;
				}
				// 終端の場合は新しく取り出す.
				inUse = null;
			}
			// 昇順.
			if(ascFlag) {
				final int len = flagArray.size();
				for(int i = rowsPos + 1; i < len; i ++) {
					if(src.create(ascFlag, flagArray.valueAt(i))
						.hasNext()) {
						inUse = src;
						rowsPos = i;
						srcPos = flagArray.keyAt(i);
						nowGetFlag = true;
						return true;
					}
				}
			// 降順.
			} else {
				for(int i = rowsPos - 1; i >= 0; i --) {
					if(src.create(ascFlag, flagArray.valueAt(i))
						.hasNext()) {
						inUse = src;
						rowsPos = i;
						srcPos = flagArray.keyAt(i);
						nowGetFlag = true;
						return true;
					}
				}
			}
			return false;
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
			return (srcPos << FLAGS_BIT) +
				inUse.next();
		}
	}
}
