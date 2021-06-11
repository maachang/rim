package rim.util;

/**
 * 指定長のフラグ配列を管理します.
 */
public class Flags {
	private int[] flags;
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
		return (flags[no >> 5] & (1L << (no & 0x001f))) != 0;
	}
	
	/**
	 * フラグ管理長を取得.
	 * @return int フラグ管理数が返却されます.
	 */
	public int getLength() {
		return length;
	}
}
