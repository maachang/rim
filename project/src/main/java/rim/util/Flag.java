package rim.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atomicなフラグ情報(true or false).
 */
public class Flag {
	private final AtomicInteger ato = new AtomicInteger(0);

	/**
	 * コンストラクタ.
	 */
	public Flag() {

	}

	/**
	 * コンストラクタ.
	 * 
	 * @param n
	 */
	public Flag(boolean n) {
		ato.set(n ? 1 : 0);
	}

	/**
	 * 取得.
	 * 
	 * @return
	 */
	public boolean get() {
		return ato.get() == 1;
	}

	/**
	 * 設定.
	 * 
	 * @param n
	 */
	public void set(boolean n) {
		while (!ato.compareAndSet(ato.get(), n ? 1 : 0))
			;
	}

	/**
	 * 設定.
	 * 
	 * @param n
	 * @return
	 */
	public boolean setToGetBefore(boolean n) {
		int ret;
		while (!ato.compareAndSet((ret = ato.get()), n ? 1 : 0))
			;
		return ret == 1;
	}

	/**
	 * 文字変換.
	 * 
	 * @return String 文字に変換します.
	 */
	public String toString() {
		return String.valueOf(ato.get());
	}
}
