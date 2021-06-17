package rim.util;

import java.util.Iterator;
import java.util.Map;

/**
 * IndexKeyValueリスト.
 *
 * BinarySearchを使って、データの追加、削除、取得を行います.
 * HashMapと比べると速度は１０倍ぐらいは遅いですが、リソースは
 * List構造のものと同じぐらいしか食わないので、リソースを重視
 * する場合は、こちらを利用することをおすすめします.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class IndexKeyValueList<K, V> {
	private final ObjectList<Entry<K, V>> list;

	/**
	 * コンストラクタ.
	 */
	public IndexKeyValueList() {
		list = new ObjectList<Entry<K, V>>();
	}

	/**
	 * コンストラクタ.
	 * 
	 * @param args args.length == 1 で args[0]にMap属性を設定すると、その内容がセットされます. また、 key,
	 *             value .... で設定することも可能です.
	 */
	public IndexKeyValueList(final Object... args) {
		if (args.length == 1) {
			if (args[0] == null) {
				list = new ObjectList<Entry<K, V>>();
			} else if (args[0] instanceof Map) {
				list = new ObjectList<Entry<K, V>>(((Map) args[0]).size());
				putAll(args[0]);
				return;
			} else if (args[0] instanceof Number) {
				list = new ObjectList<Entry<K, V>>(((Number) args[0]).intValue());
				return;
			}
			throw new IllegalArgumentException("Key and Value need to be set.");
		}
		list = new ObjectList<Entry<K, V>>(args.length >> 1);
		putAll(args);
	}

	/**
	 * データクリア.
	 */
	public final void clear() {
		list.clear();
	}

	/**
	 * データセット.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public final V put(final K key, final V value) {
		if (key == null) {
			return null;
		} else if (list.size() == 0) {
			list.add(new Entry<K, V>(key, value));
			return null;
		}
		int mid, cmp;
		int low = 0;
		int high = list.size() - 1;
		Object[] olst = list.rawArray();
		mid = -1;
		while (low <= high) {
			mid = (low + high) >>> 1;
			if ((cmp = ((Comparable) ((Entry<K, V>) olst[mid]).key).compareTo(key)) < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				// 一致条件が見つかった場合.
				final Entry<K, V> o = (Entry<K, V>) olst[mid];
				final Object ret = o.value;
				o.value = value;
				return (V) ret;
			}
		}
		// 一致条件が見つからない場合.
		mid = (((Comparable) ((Entry<K, V>) olst[mid]).key).compareTo(key) < 0) ? mid + 1 : mid;
		list.add(null);
		final int len = list.size();
		olst = list.rawArray();
		System.arraycopy(olst, mid, olst, mid + 1, len - (mid + 1));
		olst[mid] = new Entry<K, V>(key, value);
		return null;
	}

	/**
	 * 指定データ群の設定.
	 * 
	 * @param args args.length == 1 で args[0]にMap属性を設定すると、その内容がセットされます. また、 key,
	 *             value .... で設定することも可能です.
	 */
	public final void putAll(final Object... args) {
		if (args == null) {
			return;
		} else if (args.length == 1) {
			// mapの場合.
			if (args[0] instanceof Map) {
				Map mp = (Map) args[0];
				if (mp.size() == 0) {
					return;
				}
				K k;
				final Iterator it = mp.keySet().iterator();
				while (it.hasNext()) {
					k = (K) it.next();
					if (k == null) {
						continue;
					}
					put(k, (V) mp.get(k));
				}
				// IndexKeyValueListの場合.
			} else if (args[0] instanceof IndexKeyValueList) {
				int len;
				IndexKeyValueList mp = (IndexKeyValueList) args[0];
				if ((len = mp.size()) == 0) {
					return;
				}
				for (int i = 0; i < len; i++) {
					put(keyAt(i), valueAt(i));
				}
			} else {
				throw new IllegalArgumentException("Key and Value need to be set.");
			}
		} else {
			// key, value ... の場合.
			final int len = args.length;
			for (int i = 0; i < len; i += 2) {
				put((K) args[i], (V) args[i + 1]);
			}
		}
		return;
	}

	/**
	 * データ取得.
	 * 
	 * @param key
	 * @return
	 */
	public final V get(final Object key) {
		final Entry<K, V> e = getEntry((K) key);
		if (e == null) {
			return null;
		}
		return e.value;
	}

	/**
	 * データ確認.
	 * 
	 * @param key
	 * @return
	 */
	public final boolean containsKey(final K key) {
		return searchKey(key) != -1;
	}

	/**
	 * データ削除.
	 * 
	 * @param key
	 * @return
	 */
	public final V remove(final K key) {
		final int no = searchKey(key);
		if (no != -1) {
			return (V) list.remove(no);
		}
		return null;
	}

	/**
	 * データ数を取得.
	 * 
	 * @return
	 */
	public final int size() {
		return list.size();
	}

	/**
	 * キー名一覧を取得.
	 * 
	 * @return
	 */
	public final Object[] names() {
		final int len = list.size();
		final Object[] ret = new Object[len];
		for (int i = 0; i < len; i++) {
			ret[i] = list.get(i).key;
		}
		return ret;
	}

	/**
	 * 指定項番でキー情報を取得.
	 * 
	 * @param no
	 * @return
	 */
	public final K keyAt(int no) {
		return list.get(no).key;
	}

	/**
	 * 指定項番で要素情報を取得.
	 * 
	 * @param no
	 * @return
	 */
	public final V valueAt(int no) {
		return list.get(no).value;
	}

	// 指定キーのEntry情報を取得.
	protected final Entry<K, V> getEntry(final K key) {
		final int no = searchKey(key);
		if (no == -1) {
			return null;
		}
		return list.get(no);
	}

	@Override
	public int hashCode() {
		return list.size();
	}

	/**
	 * 対象オブジェクトと一致するかチェック.
	 * 
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof IndexKeyValueList) {
			final IndexKeyValueList ix = (IndexKeyValueList) o;
			int len = list.size();
			if (len != ix.size()) {
				return false;
			}
			Entry s, d;
			final Object[] lst = list.rawArray();
			for (int i = 0; i < len; i++) {
				s = (Entry) lst[i];
				d = ix.getEntry(s.key);
				if (d == null || !s.equals(d)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 文字列として出力.
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		int len = list.size();
		buf.append("{");
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(", ");
			}
			buf.append(list.get(i));
		}
		return buf.append("}").toString();
	}

	/**
	 * 同じ内容で別のオブジェクトを作成.
	 * 
	 * @param out 指定した場合、このオブジェクトに格納されます.
	 * @return IndexKeyValueList コピーされた内容が返却されます.
	 */
	public IndexKeyValueList copy(IndexKeyValueList<K, V> out) {
		final IndexKeyValueList<K, V> ret = out == null ? new IndexKeyValueList<K, V>(this.size()) : out;
		ret.clear();
		final ObjectList<Entry<K, V>> srcList = this.list;
		final ObjectList<Entry<K, V>> retList = ret.list;
		final int len = srcList.size();
		for (int i = 0; i < len; i++) {
			retList.add(srcList.get(i).copy());
		}
		return ret;
	}

	// バイナリサーチ.
	private final int searchKey(final K n) {
		if (n != null) {
			final Object[] olst = list.rawArray();
			int low = 0;
			int high = list.size() - 1;
			int mid, cmp;
			while (low <= high) {
				mid = (low + high) >>> 1;
				if ((cmp = ((Comparable) ((Entry<K, V>) olst[mid]).key).compareTo(n)) < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid; // key found
				}
			}
		}
		return -1;
	}

	/**
	 * Index用KeyValue.
	 */
	protected static final class Entry<K, V> implements Comparable<K> {
		K key;
		V value;

		public Entry(K k, V v) {
			key = k;
			value = v;
		}

		public Entry<K, V> copy() {
			return new Entry(key, value);
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		@Override
		public int compareTo(K n) {
			return ((Comparable) key).compareTo(n);
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Entry) {
				Entry e = (Entry) o;
				return key.equals(e.key) && (value == null ? e.value == null : value.equals(e.value));
			}
			return false;
		}

		@Override
		public String toString() {
			if (value instanceof CharSequence || value instanceof Character) {
				return new StringBuilder().append("\"").append(key).append("\": \"").append(value).append("\"")
						.toString();
			}
			return new StringBuilder().append("\"").append(key).append("\": \"").append(value).append("\"").toString();
		}
	}
}
