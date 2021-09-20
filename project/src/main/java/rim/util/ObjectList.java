package rim.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * オブジェクトリスト.
 */
@SuppressWarnings({ "unchecked" })
public final class ObjectList<T> {
	private static final int DEF = 8;
	protected Object[] list;
	protected int length;
	protected int max;

	/**
	 * コンストラクタ.
	 */
	public ObjectList() {
		this(DEF);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param buf 初期配列サイズを設定します.
	 */
	public ObjectList(int buf) {
		if (buf <= 0) {
			// ０以下の場合は空の配列を生成.
			buf = 0;
			// 0以上でデフォルト以下なら、デフォルト値をセット.
		} else if (buf < DEF) {
			buf = DEF;
		}
		max = buf;
		list = new Object[buf];
		length = 0;
	}

	/**
	 * コンストラクタ.
	 *
	 * @param 初期設定情報を設定します.
	 */
	public ObjectList(Object... o) {
		if (o != null && o.length > 0) {
			int oLen = o.length;
			int len = oLen + (oLen >> 1);
			if (len < DEF) {
				len = DEF;
			}
			max = len;
			list = new Object[len];
			length = oLen;
			System.arraycopy(o, 0, list, 0, oLen);
		} else {
			max = DEF;
			list = new Object[max];
			length = 0;
		}
	}

	/**
	 * 情報クリア.
	 */
	public void clear() {
		list = new Object[max];
		length = 0;
	}

	/**
	 * 情報クリア.
	 *
	 * @param buf クリアーする場合の再生成サイズ指定をします.
	 */
	public void clear(int buf) {
		list = new Object[buf];
		length = 0;
	}

	// 追加領域を広げる必要がある場合に広げる処理.
	private void spread() {
		if (length + 1 >= list.length) {
			Object[] tmp = new Object[(length + (length >> 1)) + 4];
			System.arraycopy(list, 0, tmp, 0, length);
			list = tmp;
		}
	}

	/**
	 * 情報追加.
	 *
	 * @param T 対象の要素を設定します.
	 * @return boolean
	 */
	public boolean add(T n) {
		spread();
		list[length++] = n;
		return true;
	}

	/**
	 * 情報追加.
	 * 
	 * @param no 追加対象する項番を設定します.
	 * @param n  追加対象の情報を設定します.
	 */
	public void add(int no, T n) {
		spread();
		System.arraycopy(list, no, list, no + 1, length - no);
		list[no] = n;
		length++;
	}

	/**
	 * すでに存在する位置の情報を再設定.
	 *
	 * @param no 対象の項番を設定します.
	 * @param o  対象の要素を設定します.
	 * @return T 前の情報が返却されます.
	 */
	public T set(int no, T o) {
		if (no < 0 || no >= length) {
			return null;
		}
		T ret = (T) list[no];
		list[no] = o;
		return ret;
	}

	/**
	 * 指定位置の情報を取得.
	 *
	 * @param no 対象の項番を設定します.
	 * @return T 対象の要素が返却されます.
	 */
	public T get(int no) {
		if (no < 0 || no >= length) {
			return null;
		}
		return (T) list[no];
	}

	/**
	 * 情報削除.
	 *
	 * @param no 対象の項番を設定します.
	 * @return T 削除された情報が返却されます.
	 */
	public T remove(int no) {
		if (no < 0 || no >= length) {
			return null;
		}
		T ret = null;
		if (length == 1) {
			length = 0;
			ret = (T) list[0];
			list[0] = null;
		} else {
			// 厳密な削除.
			length--;
			for (int i = no; i < length; i++) {
				list[i] = list[i + 1];
			}
			list[length] = null;
		}
		return ret;
	}

	/**
	 * 現在の情報数を取得.
	 *
	 * @return size 対象のサイズが返却されます.
	 */
	public int size() {
		return length;
	}

	/**
	 * オブジェクト配列情報を取得.
	 * 
	 * @return Object[] 配列情報として取得します.
	 */
	public Object[] toArray() {
		Object[] ret = new Object[length];
		System.arraycopy(list, 0, ret, 0, length);
		return ret;
	}

	/**
	 * 対象要素のクラスを指定して、オブジェクト配列情報を取得.
	 * 
	 * @param o 対象要素のクラスを設定します.
	 * @return T[] 配列情報として取得します.
	 */
	public T[] toArray(Class<T> o) {
		Object ret = Array.newInstance(o, length);
		System.arraycopy(list, 0, ret, 0, length);
		return (T[]) ret;
	}

	/**
	 * オブジェクト配列情報を取得.
	 * 
	 * @return Object[] 配列情報として取得します.
	 */
	public Object[] rawArray() {
		return list;
	}

	/**
	 * 内容を文字列で取得.
	 * 
	 * @return String 文字列が返却されます.
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder("[");
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buf.append(", ");
			}
			buf.append(list[i]);
		}
		return buf.append("]").toString();
	}

	/**
	 * 検索処理.
	 * 
	 * @param n 検索対象の要素を設定します.
	 * @return int 検索結果の位置が返却されます. [-1]の場合は情報は存在しません.
	 */
	public int search(T n) {
		int len = length;
		if (n == null) {
			for (int i = 0; i < len; i++) {
				if (list[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if (n.equals(list[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * ソート処理.
	 */
	public void sort() {
		if (length > 0) {
			Arrays.sort(list, 0, length);
		}
	}

	/**
	 * 現在設定されてる長さに内部配列をあわせます.
	 */
	public void smart() {
		if (length > 0) {
			list = toArray();
		}
	}
}
