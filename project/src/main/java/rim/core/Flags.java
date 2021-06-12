package rim.core;

import java.util.Arrays;

import rim.exception.RimException;

/**
 * 指定長のフラグ配列を管理します.
 */
public class Flags {
	private long[] flags;
	private int length;
	
	/**
	 * コンストラクタ.
	 * @param length 管理するフラグ配列数を設定します.
	 */
	public Flags(int length) {
		final int flagsLength = (length >> 6) +
			((length & 0x003f) == 0 ? 0 : 1);
		this.length = length;
		this.flags = new long[flagsLength];
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
			flags[no >> 6] |= 1L << (long)(no & 0x003f);
		} else {
			flags[no >> 6] &= ~(1L << (long)(no & 0x003f));
		}
		return this;
	}
	
	/**
	 * フラグの取得.
	 * @param no 番号を設定します.
	 * @return boolean 設定されているフラグが返却されます.
	 */
	public boolean get(int no) {
		return (flags[no >> 6] & (1L << (long)(no & 0x003f))) != 0;
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
		long[] v = value.flags;
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
		long[] v = value.flags;
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
	 * Flag配列長を取得.
	 * @return int Flag配列長が返却されます.
	 */
	protected int getArrayLength() {
		return flags.length;
	}
	
	/**
	 * フラグONが含まれるFlag配列位置を検索して取得.
	 * @param ascFlag trueの場合、昇順で検索します.
	 * @param offArrayPos Flag配列開始位置を設定します.
	 * @return int フラグONが含まれるFlag配列位置が返却されます.
	 *             -1の場合、存在しません.
	 */
	protected int searchOnByGetArrayPos(boolean ascFlag, int offArrayPos) {
		if(ascFlag) {
			final int len = flags.length;
			for(int i = offArrayPos; i < len; i ++) {
				if(flags[i] != 0L) {
					return i;
				}
			}
		} else {
			for(int i = offArrayPos; i >= 0; i --) {
				if(flags[i] != 0L) {
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Flag配列位置と１つのFlagに対する開始位置を設定して
	 * 次のFlagがONの位置を取得.
	 * @param ascFlag true の場合昇順.
	 * @param arrayPos Flag配列位置を設定します.
	 * @param offOneFlagPos １つのFlagに対する開始位置を設定します.
	 * @return int フラグONの１つのFlagに対する位置が返却されます.
	 *             -1の場合、存在しません.
	 */
	protected int searchOnByGetOneFlag(boolean ascFlag, int arrayPos, int offOneFlagPos) {
		long value = flags[arrayPos];
		if(ascFlag) {
			final int len = getOneArrayLength();
			for(int i = offOneFlagPos; i < len; i ++) {
				if((value & (1L << (long)i)) != 0) {
					return i;
				}
			}
		} else {
			for(int i = offOneFlagPos; i >= 0; i --) {
				if((value & (1L << (long)i)) != 0) {
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * 番号からFlag配列位置番号に変換.
	 * @param no 番号を設定します.
	 * @return int Flag配列位置番号が返却されます.
	 */
	protected static final int convertNumberByArrayPos(int no) {
		return no >> 6;
	}
	
	/**
	 * Flag配列位置から番号に変換.
	 * @param arrayNo Flag配列位置番号を設定します.
	 * @return 番号が返却されます.
	 */
	protected static final int convertArrayPosByNumber(int arrayNo) {
		return arrayNo << 6;
	}
	
	/**
	 * １つのFlag配列の長さを取得.
	 * @return int １つのFlag配列の長さが返却されます.
	 */
	protected static final int getOneArrayLength() {
		return 0x0040;
	}
}
