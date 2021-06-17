package rim.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import rim.core.Flags.FlagsIterator;
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
		final int flagsLength = (length >> 12) +
			((length & 0x00000fff) == 0 ? 0 : 1);
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
		Flags flags = rowsFlag[no >> 12];
		if(flag) {
			if(flags == null) {
				if(rowsFlag.length == (no >> 12) + 1) {
					flags = new Flags(length & 0x00000fff);
				} else {
					flags = new Flags(0x00000fff);
				}
				rowsFlag[no >> 12] = flags;
			}
			flags.put(no - ((no >> 12) << 12), true);
		} else {
			if(flags != null) {
				flags.put(no - ((no >> 12) << 12), false);
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
		final Flags flags = rowsFlag[no >> 12];
		if(flags == null) {
			return false;
		}
		return flags.get(no - ((no >> 12) << 12));
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
	
	// RowsIterator.
	private static final class RowsIterator
		implements Iterator<Integer> {
		// 昇順の場合 true.
		private boolean ascFlag;
		// RowsFlagオブジェクトが管理しているFlasg配列.
		private Flags[] rowsFlag;
		// 現在読込中のFlags配列位置.
		private int rowsPos;
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
		RowsIterator(boolean ascFlag, Flags[] lst) {
			this.ascFlag = ascFlag;
			this.rowsFlag = lst;
			this.nowGetFlag = false;
			this.rowsPos = ascFlag ? -1 : lst.length;
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
				final int len = rowsFlag.length;
				for(int i = rowsPos + 1; i < len; i ++) {
					if(rowsFlag[i] != null) {
						if(src.create(ascFlag, rowsFlag[i])
							.hasNext()) {
							inUse = src;
							rowsPos = i;
							nowGetFlag = true;
							return true;
						}
					}
				}
			// 降順.
			} else {
				for(int i = rowsPos - 1; i >= 0; i --) {
					if(rowsFlag[i] != null) {
						if(src.create(ascFlag, rowsFlag[i])
							.hasNext()) {
							inUse = src;
							rowsPos = i;
							nowGetFlag = true;
							return true;
						}
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
			return (rowsPos << 12) + inUse.next();
		}
	}
	
	/*
	// main.
	public static final void main(String[] args) {
		bench(args);
		//check(args);
	}
	
	// チェック処理.
	private static final void check(String[] args) {
		boolean ascFlag = false;
		int allLen = 5000000;
		
		final int[] posList = new int[] {
			0, 3332, 10000, 50000, 88000, 325442, 988824, 999999
		};
		final RowsFlag rowsFlag = new RowsFlag(allLen);
		
		final int len = posList.length;
		for(int i = 0; i < len; i ++) {
			rowsFlag.put(posList[i], true);
		}
		
		final Iterator<Integer> itr = rowsFlag.iterator(ascFlag);
		while(itr.hasNext()) {
			System.out.println(itr.next());
		}
	}
	
	// 計測系ベンチマーク.
	private static final void bench(String[] args) {
		long t, a, b, c, d, e, f, g, h;
		a = b = c = d = e = f = g = h = 0L;
		
		int allLen = 1000000;
		
		//final int[] posList = new int[] {
		//	0, 3332, 10000, 50000, 88000, 325442, 988824, 999999
		//};
		int cnt = 0;
		int waru = 100;
		Integer[] posList = new Integer[(allLen / waru) + ((allLen % waru) != 0 ? 1 : 0)];
		for(int i = 0; i < allLen; i += waru) {
			posList[cnt ++] = i;
		}
		
		for(int xx = 0; xx < 5; xx ++) {
		for(int r = 0; r < 1000; r ++) {
			
			t = System.currentTimeMillis();
			final RowsFlag rowsFlag = new RowsFlag(allLen);
			a += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final int len = posList.length;
			for(int i = 0; i < len; i ++) {
				rowsFlag.put(posList[i], true);
			}
			b += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final Iterator<Integer> itr = rowsFlag.iterator(false);
			while(itr.hasNext()) {
				//System.out.println(itr.next());
				itr.next();
			}
			c += System.currentTimeMillis() - t;
			
			//System.out.println();
			
//			t = System.currentTimeMillis();
//			final int rowsLen = rowsFlag.getLength();
//			for(int i = 0; i < rowsLen; i ++) {
//				if(rowsFlag.get(i)) {
//					//System.out.println(i);
//				}
//			}
//			d += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final boolean[] testFlag = new boolean[allLen];
			for(int i = 0; i < len; i ++) {
				testFlag[posList[i]] = true;
			}
			e += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final int testFlagLength = testFlag.length;
			for(int i = 0; i < testFlagLength; i ++) {
				if(testFlag[i]) {
					//System.out.println(i);
				}
			}
			f += System.currentTimeMillis() - t;
		}}
		
		System.out.println("a: " + a + " msec");
		System.out.println("b: " + b + " msec");
		System.out.println("c: " + c + " msec");
//		System.out.println("d: " + d + " msec");
		System.out.println("e: " + e + " msec");
		System.out.println("f: " + f + " msec");

	}
	*/
}
