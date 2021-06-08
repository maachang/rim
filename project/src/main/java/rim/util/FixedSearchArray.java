package rim.util;

import java.util.Arrays;

/**
 * 検索リスト.
 *
 * バイナリサーチで、リスト情報を高速検索.
 */
public final class FixedSearchArray<K> {
	// 検索キー.
	@SuppressWarnings("rawtypes")
	private static class SearchKey<K> implements Comparable {
		public final K key;
		public final int no;

		public SearchKey(K k, int n) {
			key = k;
			no = n;
		}

		@Override
		@SuppressWarnings("unchecked")
		public final int compareTo(final Object o) {
			if (o instanceof SearchKey) {
				return ((Comparable) key).compareTo(((SearchKey) o).key);
			}
			return ((Comparable) key).compareTo(o);
		}

		@Override
		public final boolean equals(final Object o) {
			if (o instanceof SearchKey) {
				return key.equals(((SearchKey) o).key);
			}
			return key.equals(o);
		}
	}

	// 検索キーで検索.
	@SuppressWarnings("rawtypes")
	private final int searchKey(final SearchKey[] keys, final K target) {
		int ret = binarySearch(keys, target);
		if (ret == -1) {
			return -1;
		}
		return keys[ret].no;
	}

	// バイナリサーチ.
	@SuppressWarnings("rawtypes")
	private final int binarySearch(final SearchKey[] keys, final K n) {
		int low = 0;
		int high = keys.length - 1;
		int mid, cmp;
		while (low <= high) {
			mid = (low + high) >>> 1;
			if ((cmp = keys[mid].compareTo(n)) < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}
		return -1;
	}

	// ソートされたキー情報.
	private SearchKey<K>[] keys = null;
	private int[] nonSortKeys = null;
	private int count = 0;

	protected FixedSearchArray() {
	}

	/**
	 * コンストラクタ.
	 * 
	 * @param len 格納データ数を設定します.
	 */
	@SuppressWarnings("unchecked")
	public FixedSearchArray(final int len) {
		keys = new SearchKey[len];
	}

	/**
	 * コンストラクタ.
	 * 
	 * @param args 追加情報群を設定します.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FixedSearchArray(final K... args) {
		final int len = args.length;
		final SearchKey[] k = new SearchKey[len];
		for (int i = 0; i < len; i++) {
			k[count++] = new SearchKey<K>(args[i], i);
		}
		Arrays.sort(k);
		keys = k;
		createNonSortKeys();
	}
	
	/**
	 * リスト追加.
	 * 
	 * @param key
	 * @param no
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final FixedSearchArray<K> add(final K key, final int no) {
		if (key == null) {
			return this;
		}
		// 初期指定長より追加はできるが、毎回配列再生成なので遅い.
		if (count >= keys.length) {
			int len = keys.length;
			SearchKey[] k = new SearchKey[len + 1];
			if (len > 0) {
				System.arraycopy(keys, 0, k, 0, len);
			}
			k[count++] = new SearchKey<K>(key, no);
			Arrays.sort(k);
			keys = k;
			createNonSortKeys();
		} else {
			keys[count++] = new SearchKey<K>(key, no);
			if (count >= keys.length) {
				Arrays.sort(keys);
				createNonSortKeys();
			}
		}
		return this;
	}
	
	// 設定順の情報取得用.
	private final void createNonSortKeys() {
		final int len = keys.length;
		int[] n = new int[len];
		for(int i = 0; i < len; i ++) {
			n[keys[i].no] = i;
		}
		nonSortKeys = n;
	}

	/**
	 * リスト追加途中でFixさせる.
	 * リスト追加は指定したデータ数分データを追加するとFixするが、
	 * この処理は途中で強制的にFixさせます.
	 * 
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final boolean fix() {
		if (count == keys.length) {
			return false;
		}
		SearchKey[] nkeys = new SearchKey[count];
		System.arraycopy(keys, 0, nkeys, 0, count);
		keys = nkeys;
		Arrays.sort(keys);
		createNonSortKeys();
		return true;
	}
	
	/**
	 * 追加したリストが全件設定されているかチェック.
	 * この内容がtrueで無い場合は、検索出来ません.
	 * 
	 * @return
	 */
	public final boolean isFix() {
		return count == keys.length;
	}

	/**
	 * データ数を取得.
	 * 
	 * @return
	 */
	public final int size() {
		return keys.length;
	}

	/**
	 * 現在カウント数を取得.
	 * 
	 * @return
	 */
	public final int getCount() {
		return count;
	}
	
	/**
	 * 検索処理.
	 * 
	 * @param target
	 * @return
	 */
	public final int search(final K target) {
		if (count == keys.length) {
			return searchKey(keys, target);
		}
		return -1;
	}
	
	/**
	 * キー情報を取得.
	 * 
	 * @param no
	 * @return
	 */
	public final K get(final int no) {
		return keys[nonSortKeys[no]].key;
	}
}
