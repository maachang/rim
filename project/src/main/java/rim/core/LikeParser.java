package rim.core;

import rim.exception.RimException;

public class LikeParser {
	/**
	 * 開始ポジション.
	 */
	protected static final int FIRST = 0;

	/**
	 * 終了ポジション.
	 */
	protected static final int LAST = -9;

	/**
	 * 間ポジション.
	 */
	protected static final int BETWEEN = -1;
	
	/**
	 * 不明条件.
	 */
	private static final int UNKNOWN = -99;


	// パーサー情報.
	private Object[] parser;
	
	// 元のLike文.
	private String src;

	/**
	 * コンストラクタ.
	 * 
	 * @param parser LikeAnalysisで解析された情報を設定します.
	 * @param src 元のLike文を設定します.
	 */
	protected LikeParser(final Object[] parser, final String src) {
		this.parser = parser;
		this.src = src;
	}

	/**
	 * マッチしているかチェック.
	 * 
	 * @param value  文字列を設定します.
	 * @return boolean [true]の場合、マッチしています.
	 * @exception Exception 例外.
	 */
	public final boolean match(final String value) {
		if (value == null || value.length() <= 0) {
			return false;
		}
		final int len = parser.length;
		// [特殊条件]内容が１文字の場合は、％指定のみか、_文字数定義.
		if (len == 1) {
			final int n = (Integer) parser[0];
			if (n == LikeParser.BETWEEN) {
				return true;
			}
			// 長さ一致.
			return value.length() == n;
		// [特殊条件]パース条件が3文字の場合.
		} else if (len == 3) {
			final Object o1 = parser[0],
				o2 = parser[1],
				o3 = parser[2];
			// Like構文ではなく、完全一致条件の場合.
			if (o1 instanceof Integer &&
				o2 instanceof Integer &&
				o3 instanceof String && 
				(Integer)o1 == LikeParser.FIRST &&
				(Integer)o2 == LikeParser.LAST) {
				return value.equals(o3);
			}
		}
		// 通常パース条件.
		int p = 0;
		int b = 0;
		
		Object o;
		String str;
		int cnd;
		int event = UNKNOWN; // 定義不明.
		int targetLen;
		boolean last = false;
		String bw = null;
		for (int i = 0; i < len; i++) {
			o = parser[i];
			// 条件指定.
			if (o instanceof Integer) {
				cnd = (Integer)o;
				// 最後方一致条件の場合.
				if (cnd == LikeParser.LAST) {
					last = true;
				}
				// 通常条件の場合.
				else {
					event = cnd;
				}
			}
			// 文字指定.
			else {
				str = (String)o;
				// 文字指定の場合.
				if (event > 0) {
					targetLen = event;
					if (bw == null) {
						if (value.length() < b + targetLen + str.length() ||
							!str.equals(value.substring(
								b + targetLen, b + targetLen + str.length()))) {
							return false;
						}
					} else {
						while (true) {
							if (value.length() < b + targetLen + str.length() ||
								!str.equals(value.substring(
									b + targetLen, b + targetLen + str.length()))) {
								// 見つからない場合は、以前の部分検索条件が見つかるまで検索.
								// 見つかった場合は、再度文字指定検索を再開.
								if ((p = value.indexOf(bw, b)) == -1) {
									return false;
								}
								b = p + bw.length();
							} else {
								break;
							}
						}
					}
					// 最後方一致条件の場合は、現在の検索長と、文字列長が一致チェックで終了.
					if (last) {
						return (b + targetLen + str.length() == value.length());
					} else {
						b = b + targetLen + str.length();
					}
					event = UNKNOWN; // 定義不明.
					bw = null; // 前回のindexof条件をクリア.
				// 条件指定の場合.
				// ただし最後方一致の場合は、そちらを優先させる.
				} else if (!last) {
					switch (event) {
					case LikeParser.FIRST: // 先頭一致.
						if (!value.startsWith(str)) {
							return false;
						}
						b = str.length();
						break;
					case LikeParser.BETWEEN: // 次の文字列が一致.
						if ((p = value.indexOf(str, b)) == -1) {
							return false;
						}
						bw = str; // 前回のindexof条件を保持.
						b = p + str.length();
						break;
					case UNKNOWN:
						throw new RimException("Illegal Like syntax: " + src);
					}
				}
				// 最後方一致条件の場合.
				if (last) {
					return value.endsWith(str);
				}
			}
		}
		// 文字指定の場合.
		if (event > 0) {
			targetLen = event;
			if (bw == null) {
				if (value.length() != b + targetLen) {
					return false;
				}
			} else {
				while (true) {
					if (value.length() != b + targetLen) {
						// 見つからない場合は、以前の部分検索条件が見つかるまで検索.
						// 見つかった場合は、文字長一致検索を再開.
						if ((p = value.indexOf(bw, b)) == -1) {
							return false;
						}
						b = p + bw.length();
					} else {
						break;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Like文の取得.
	 * 
	 * @return String Like文が返却されます.
	 */
	public final String getSrc() {
		return src;
	}

	/**
	 * パーサー内容を文字列化.
	 * 
	 * @return String 文字列内容が返却されます.
	 */
	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder();
		int len = parser.length;
		Object n;
		int nn;
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(", ");
			}
			n = parser[i];
			if (n instanceof Integer) {
				nn = (Integer) n;
				buf.append("condisions: ");
				if (nn == LikeParser.FIRST) {
					buf.append("first");
				} else if (nn == LikeParser.LAST) {
					buf.append("last");
				} else if (nn == LikeParser.BETWEEN) {
					buf.append("*");
				} else {
					buf.append(nn);
				}
			} else {
				buf.append("string; \"").append(n).append("\"");
			}
		}
		return buf.toString();
	}
}
