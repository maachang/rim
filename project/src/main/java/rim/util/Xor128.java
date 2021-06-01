package rim.util;

/**
 * xor128bitランダム発生.
 */
public final class Xor128 {
	private int a = 123456789;
	private int b = 362436069;
	private int c = 521288629;
	private int d = 88675123;

	/**
	 * コンストラクタ.
	 */
	public Xor128() {
	}

	/**
	 * コンストラクタ.
	 *
	 * @param s 乱数初期係数を設定します.
	 */
	public Xor128(int s) {
		this.setSeet(s);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param s 乱数初期係数を設定します.
	 */
	public Xor128(long s) {
		this.setSeet(s);
	}

	/**
	 * ランダム係数を設定.
	 *
	 * @param s ランダム係数を設定します.
	 */
	public final void setSeet(int s) {
		setSeet((long) s);
	}

	/**
	 * ランダム係数を設定.
	 *
	 * @param ss ランダム係数を設定します.
	 */
	public final void setSeet(long ss) {
		int s = (int) (ss & 0x00000000ffffffffL);
		a = s = 1812433253 * (s ^ (s >> 30)) + 1;
		b = s = 1812433253 * (s ^ (s >> 30)) + 2;
		c = s = 1812433253 * (s ^ (s >> 30)) + 3;
		d = s = 1812433253 * (s ^ (s >> 30)) + 4;
	}

	/**
	 * 32ビット乱数を取得.
	 *
	 * @return int 32ビット乱数が返されます.
	 */
	public final int nextInt() {
		int t, r;
		t = a;
		r = t;
		t <<= 11;
		t ^= r;
		r = t;
		r >>= 8;
		t ^= r;
		r = b;
		a = r;
		r = c;
		b = r;
		r = d;
		c = r;
		t ^= r;
		r >>= 19;
		r ^= t;
		d = r;
		return r;
	}
}
