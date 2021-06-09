package rim.core;

/**
 * 検索ユーティリティ.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class SearchUtil {
	private SearchUtil() {}
	
	/**
	 * バイナリサーチによるインデックスからの一致検索.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int indexEq(
		final Comparable[] index, final Comparable value) {
		if (value != null) {
			int low = 0;
			int high = index.length - 1;
			int mid, cmp;
			// 一致検索.
			while (low <= high) {
				mid = (low + high) >>> 1;
				if ((cmp = index[mid].compareTo(value)) < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid;
				}
			}
		}
		return -1;
	}
	
	/**
	 * バイナリサーチによるインデックスからの大なり小なりの大まか検索.
	 * 大まかな検索なので、実際には
	 *  searchGT, searchGE, searchLT, searchLE
	 * で検索してください.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param big 大なり検索の場合は true.
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	private static final int indexBS(
		final boolean big, final Comparable[] index, final Comparable value) {
		if (value != null) {
			int len = index.length;
			int low = 0;
			int high = len - 1;
			int mid = -1;
			int cmp;
			// 一致検索.
			while (low <= high) {
				mid = (low + high) >>> 1;
				if ((cmp = index[mid].compareTo(value)) < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid;
				}
			}
			// 一致条件が存在しない場合
			// 大なり検索の場合.
			if(big) {
				if ((cmp = index[mid].compareTo(value)) > 0) {
					return mid;
				} else if(len <= mid + 1) {
					return len - 1;
				}
				return mid + 1;
			}
			// 小なり検索の場合.
			if ((cmp = index[mid].compareTo(value)) > 0) {
				if(mid <= 0) {
					return 0;
				}
				return mid - 1;
			}
			return mid;
		}
		return -1;
	}
	
	/**
	 * バイナリサーチによるインデックスからの大なり[>]検索.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int indexGT(
		final Comparable[] index, final Comparable value) {
		int p = indexBS(true, index, value);
		if(p == -1) {
			return -1;
		} else if(index[p].compareTo(value) <= 0) {
			p ++;
			if(p >= index.length) {
				return -1;
			}
		}
		return p;
	}
	
	/**
	 * バイナリサーチによるインデックスからの大なり[>=]検索.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int indexGE(
		final Comparable[] index, final Comparable value) {
		int p = indexBS(true, index, value);
		if(p == -1) {
			return -1;
		} else if(index[p].compareTo(value) < 0) {
			p ++;
			if(p >= index.length) {
				return -1;
			}
		}
		return p;
	}
	
	/**
	 * バイナリサーチによるインデックスからの小なり[<]検索.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int indexLT(
		final Comparable[] index, final Comparable value) {
		int p = indexBS(true, index, value);
		if(p == -1) {
			return -1;
		} else if(index[p].compareTo(value) >= 0) {
			p --;
			if(p < 0) {
				return -1;
			}
		}
		return p;
	}

	/**
	 * バイナリサーチによるインデックスからの小なり[<=]検索.
	 * インデックスでの検索なので高速に検索が可能です.
	 * 
	 * @param index インデックス群を設定します.
	 * @param value 検索要素を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int indexLE(
		final Comparable[] index, final Comparable value) {
		int p = indexBS(true, index, value);
		if(p == -1) {
			return -1;
		} else if(index[p].compareTo(value) > 0) {
			p --;
			if(p < 0) {
				return -1;
			}
		}
		return p;
	}
	
	/**
	 * 全件検索での一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param value 検索する検索条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalEq(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable value) {
		Object[] row;
		final int len = rows.length;
		if(value == null) {
			return -1;
		} else {
			Comparable src;
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) == 0) != notEq) {
						return i;
					}
				}
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) == 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * 全件検索での大なり[>]一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param value 検索する検索条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalGT(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable value) {
		Object[] row;
		final int len = rows.length;
		if(value == null) {
			return -1;
		} else {
			Comparable src;
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) > 0) != notEq) {
						return i;
					}
				}
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) > 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * 全件検索での大なり[>=]一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param value 検索する検索条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalGE(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable value) {
		Object[] row;
		final int len = rows.length;
		if(value == null) {
			return -1;
		} else {
			Comparable src;
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) >= 0) != notEq) {
						return i;
					}
				}
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) >= 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 全件検索での小なり[<]一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param value 検索する検索条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalLT(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable value) {
		Object[] row;
		final int len = rows.length;
		if(value == null) {
			return -1;
		} else {
			Comparable src;
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) < 0) != notEq) {
						return i;
					}
				}
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) < 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * 全件検索での小なり[<=]一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param value 検索する検索条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalLE(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable value) {
		Object[] row;
		final int len = rows.length;
		if(value == null) {
			return -1;
		} else {
			Comparable src;
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) <= 0) != notEq) {
						return i;
					}
				}
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(value) <= 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * 全件検索での小なり[<=]一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param start 開始条件を設定します.
	 * @param end 終了条件を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalBetween(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, Comparable start, Comparable end) {
		Object[] row;
		final int len = rows.length;
		if(start == null || end == null) {
			return -1;
		} else {
			Comparable src;
			// 昇順検索.
			if(ascFlag) {
				for(int i = startPos; i < len; i ++) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(start) >= 0 &&
						src.compareTo(end) <= 0) != notEq) {
						return i;
					}
				}
			// 降順検索.
			} else {
				for(int i = startPos; i >= 0; i --) {
					row = (Object[])rows[i];
					src = (Comparable)row[columnNo];
					if(src != null &&
						(src.compareTo(start) <= 0 &&
						src.compareTo(end) >= 0) != notEq) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 全件検索での複数一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param values 検索する検索条件群を設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalIn(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final Comparable... values) {
		int j;
		Object[] row;
		final int len = rows.length;
		Comparable src;
		
		// inで設定される一致条件が存在しない場合.
		final int valueLength = values.length;
		if(valueLength == 0) {
			// 処理しない.
			return -1;
		}
		if(ascFlag) {
			for(int i = startPos; i < len; i ++) {
				row = (Object[])rows[i];
				if((src = (Comparable)row[columnNo]) != null) {
					for(j = 0; j < valueLength; j ++) {
						if((src.compareTo(values[j]) == 0) != notEq) {
							return i;
						}
					}
				}
			}
		} else {
			for(int i = startPos; i >= 0; i --) {
				row = (Object[])rows[i];
				if((src = (Comparable)row[columnNo]) != null) {
					for(j = valueLength - 1; j >= 0; j --) {
						if((src.compareTo(values[j]) == 0) != notEq) {
							return i;
						}
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 全件検索でLike一致情報を取得.
	 * 全件検索を行うので、速度は遅くなります.
	 * 
	 * @param rows 列行情報を設定します.
	 * @param columnNo 対象の列番号を設定します.
	 * @param ascFlag 昇順で検索する場合はtrueを設定します.
	 * @param notEq 不一致条件で検索する場合は true を設定します.
	 * @param startPos 検索開始位置を設定します.
	 * @param parser Like検索するLikeParserを設定します.
	 * @return int -1の場合、情報は見つかりませんでした.
	 */
	public static final int normalLike(
		final Object[] rows, final int columnNo, final boolean ascFlag,
		final boolean notEq, final int startPos, final LikeParser parser) {
		Object[] row;
		final int len = rows.length;
		String src;
		if(ascFlag) {
			for(int i = startPos; i < len; i ++) {
				row = (Object[])rows[i];
				src = (String)row[columnNo];
				if(src != null &&
					parser.match(src) != notEq) {
					return i;
				}
			}
		} else {
			for(int i = startPos; i >= 0; i --) {
				row = (Object[])rows[i];
				src = (String)row[columnNo];
				if(src != null &&
					parser.match(src) != notEq) {
					return i;
				}
			}
		}
		return -1;
	}

}
