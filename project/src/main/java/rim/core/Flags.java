package rim.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import rim.exception.RimException;

/**
 * 指定長のフラグ配列を管理します.
 */
public class Flags implements BaseFlags<Flags> {
	// フラグを管理する配列.
	private int[] flags;
	// フラグ配列長.
	private int length;
	
	/**
	 * コンストラクタ.
	 * @param length 管理するフラグ配列数を設定します.
	 */
	public Flags(int length) {
		final int flagsLength = (length >> 5) +
			((length & 0x001f) == 0 ? 0 : 1);
		this.length = length;
		this.flags = new int[flagsLength];
	}
	
	/**
	 * フラグのクリア.
	 * @return Flags このオブジェクトが返却されます.
	 */
	public Flags clear() {
		Arrays.fill(flags, 0);
		return this;
	}
	
	/**
	 * フラグのセット.
	 * @param no 番号を設定します.
	 * @param flag 設定するフラグ条件を設定します.
	 * @return Flags このオブジェクトが返却されます.
	 */
	public Flags put(int no, boolean flag) {
		if(flag) {
			flags[no >> 5] |= 1 << (no & 0x001f);
		} else {
			flags[no >> 5] &= ~(1 << (no & 0x001f));
		}
		return this;
	}
	
	/**
	 * フラグの取得.
	 * @param no 番号を設定します.
	 * @return boolean 設定されているフラグが返却されます.
	 */
	public boolean get(int no) {
		return (flags[no >> 5] &
			(1 << (no & 0x001f))) != 0;
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
	 * @param value このオブジェクトとANDするためのFlagsを設定します.
	 * @return Flags このオブジェクトが返却されます.
	 */
	public Flags and(Flags value) {
		int[] v = value.flags;
		int len = v.length;
		if(len != flags.length) {
			throw new RimException("Must be the same length.");
		}
		for(int i = 0; i < len; i ++) {
			flags[i] &= v[i];
		}
		return this;
	}
	
	/**
	 * OR処理.
	 * 処理結果は呼び出し元のオブジェクトに反映されます.
	 * @param value このオブジェクトとORするためのFlagsを設定します.
	 * @return Flags このオブジェクトが返却されます.
	 */
	public Flags or(Flags value) {
		int[] v = value.flags;
		int len = v.length;
		if(len != flags.length) {
			throw new RimException("Must be the same length.");
		}
		for(int i = 0; i < len; i ++) {
			flags[i] |= v[i];
		}
		return this;
	}
	
	/**
	 * オブジェクトを別のインスタンスでコピー.
	 * @return Flags コピーされたオブジェクトが返却されます.
	 */
	public Flags copy() {
		Flags ret = new Flags(length);
		System.arraycopy(flags, 0, ret.flags, 0, flags.length);
		return ret;
	}
	
	/**
	 * 有効な行番号を取得するIteratorを取得.
	 * @param ascFlag 昇順取得の場合は true.
	 * @return Iterator<Integer> 有効な行番号を返却する
	 *                           Iteratorが返却されます.
	 */
	public Iterator<Integer> iterator(boolean ascFlag) {
		return new FlagsIterator(ascFlag, this);
	}
	
	// FlagsIterator.
	protected static final class FlagsIterator
		implements Iterator<Integer> {
		private Flags flags;
		private int arrayPos;
		private int oneFlagPos;
		
		private boolean ascFlag;
		private boolean nowGetFlag;
		private boolean eofFlag;
		
		protected FlagsIterator() {
		}
		
		/**
		 * 生成処理.
		 * @param ascFlag 昇順の場合は true.
		 * @param flags Flagsオブジェクトを設定します.
		 */
		protected FlagsIterator(boolean ascFlag, Flags flags) {
			create(ascFlag, flags);
		}
		
		/**
		 * 生成処理.
		 * @param ascFlag 昇順の場合は true.
		 * @param flags Flagsオブジェクトを設定します.
		 * @return FlagsIterator このオブジェクトが返却されます.
		 */
		protected final FlagsIterator create(boolean ascFlag, Flags flags) {
			this.ascFlag = ascFlag;
			this.flags = flags;
			this.arrayPos = ascFlag ? -1 : flags.flags.length;
			this.oneFlagPos = -2;
			
			this.nowGetFlag = false;
			this.eofFlag = false;
			
			nowGet();
			return this;
		}
		
		/**
		 * 次の情報を取得.
		 * @return boolean true の場合次の情報は存在します.
		 */
		private boolean nowGet() {
			if(nowGetFlag) {
				return true;
			}
			int oneFlag;
			final int[] lst = flags.flags;
			final int len = lst.length;
			while(!eofFlag) {
				if(oneFlagPos != -2) {
					oneFlag = lst[arrayPos];
					if(ascFlag) {
						for(int i = oneFlagPos + 1; i < 32; i ++) {
							if((oneFlag & (1 << i)) != 0) {
								oneFlagPos = i;
								nowGetFlag = true;
								return true;
							}
						}
					} else {
						for(int i = oneFlagPos - 1; i >= 0; i --) {
							if((oneFlag & (1 << i)) != 0) {
								oneFlagPos = i;
								nowGetFlag = true;
								return true;
							}
						}
					}
					oneFlagPos = -2;
				}
				eofFlag = true;
				if(ascFlag) {
					for(int i = arrayPos + 1; i < len; i ++) {
						if(lst[i] != 0) {
							arrayPos = i;
							oneFlagPos = -1;
							eofFlag = false;
							break;
						}
					}
				} else {
					for(int i = arrayPos - 1; i >= 0; i --) {
						if(lst[i] != 0) {
							arrayPos = i;
							oneFlagPos = 32;
							eofFlag = false;
							break;
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
			return (arrayPos << 5) + oneFlagPos;
		}
	}
}
