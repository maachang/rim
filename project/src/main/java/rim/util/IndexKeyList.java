package rim.util;

/**
 * IndexKeyリスト.
 *
 * BinarySearchを使って登録、削除、存在確認を行います.
 * HashSetと比べると速度は１０倍ぐらいは遅いですが、リソースは
 * List構造のものと同じぐらいしか食わないので、リソースを重視
 * する場合は、こちらを利用することをおすすめします.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class IndexKeyList {
	private final ObjectList<Comparable> list;

	/**
	 * コンストラクタ.
	 */
	public IndexKeyList() {
		list = new ObjectList<Comparable>();
	}
	
	/**
	 * コンストラクタ.
	 * @param initLen 初期配列長を設定.
	 */
	public IndexKeyList(int initLen) {
		list = new ObjectList<Comparable>(initLen);
	}

	/**
	 * データクリア.
	 */
	public final void clear() {
		list.clear();
	}

	/**
	 * データー登録.
	 * 
	 * @param value 追加対象の条件を設定します.
	 * @return boolean 既に存在する場合は true.
	 */
	public final boolean add(final Comparable value) {
		if (value == null) {
			return false;
		} else if (list.size() == 0) {
			list.add(value);
			return false;
		}
		int mid, cmp;
		int low = 0;
		int high = list.size() - 1;
		Object[] olst = list.rawArray();
		mid = -1;
		while (low <= high) {
			mid = (low + high) >>> 1;
			if ((cmp = ((Comparable)olst[mid]).compareTo(value)) < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				// 一致条件が見つかった場合.
				return true;
			}
		}
		// 一致条件が見つからない場合.
		mid = (((Comparable)olst[mid]).compareTo(value) < 0) ? mid + 1 : mid;
		list.add(null);
		final int len = list.size();
		olst = list.rawArray();
		System.arraycopy(olst, mid, olst, mid + 1, len - (mid + 1));
		olst[mid] = value;
		return false;
	}
	
	// バイナリサーチ.
	private final int searchKey(final Comparable n) {
		if (n != null) {
			final Object[] olst = list.rawArray();
			int low = 0;
			int high = list.size() - 1;
			int mid, cmp;
			while (low <= high) {
				mid = (low + high) >>> 1;
				if ((cmp = ((Comparable)olst[mid]).compareTo(n)) < 0) {
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
	 * 存在確認
	 * 
	 * @param value 確認条件を設定します.
	 * @return boolean 存在する場合は true.
	 */
	public final boolean contains(final Comparable value) {
		return searchKey(value) != -1;
	}

	/**
	 * データ削除.
	 * 
	 * @param value 削除条件を設定します.
	 * @return boolean 削除できた場合は true.
	 */
	public final boolean remove(final Comparable value) {
		final int no = searchKey(value);
		if (no != -1) {
			list.remove(no);
			return true;
		}
		return false;
	}

	/**
	 * データ数を取得.
	 * 
	 * @return int 登録データ数が返却されます.
	 */
	public final int size() {
		return list.size();
	}
	

	/**
	 * 指定項番で要素情報を取得.
	 * 
	 * @param no 項番を設定します.
	 * @return Comparable 要素情報が返却されます.
	 */
	public final Comparable valueAt(int no) {
		return list.get(no);
	}
	
	@Override
	public boolean equals(Object o) {
		final IndexKeyList ix = (IndexKeyList) o;
		int len = list.size();
		if (len != ix.size()) {
			return false;
		}
		Comparable c2 = (Comparable)o;
		final Object[] lst = list.rawArray();
		for (int i = 0; i < len; i++) {
			if(!c2.equals(lst[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		int len = list.size();
		buf.append("[");
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(", ");
			}
			buf.append(list.get(i));
		}
		return buf.append("]").toString();
	}
}
