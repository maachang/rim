package rim.core;

import rim.exception.RimException;
import rim.util.ObjectList;

public final class LikeAnalysis {
	private LikeAnalysis() {}
	
	/**
	 * Like文の解析.
	 * Like検索は、[%,_]をサポート.
	 * [%][*]は不明な複数文字が設定されている内容.
	 * [_][?]は不明な１文字が設定されている内容.
	 * 
	 * @param like Like構文を設定します.
	 * @return LikeParser Like構文がパースされた内容が返却されます.
	 */
	public static final LikeParser analysis(final String like) {
		if (like == null || like.length() <= 0) {
			throw new RimException("Like parameter does not exist.");
		}

		char c;
		int type = -1;
		int pos = 0;
		boolean yenFlag = false;
		StringBuilder buf = null;
		final int len = like.length();
		final ObjectList<Object> ret = new ObjectList<Object>();

		for (int i = 0; i < len; i++) {

			c = like.charAt(i);

			// ターゲットポイント.
			if (!yenFlag && (c == '%' || c == '*' || c == '_' || c == '?')) {

				// データが存在する場合.
				if (buf != null) {
					if (ret.size() == 0) {
						ret.add(LikeParser.FIRST);
						// 初期化.
						type = -1;
						pos = 0;
					}
					ret.add(buf.toString());
					buf = null;
				}
				if (c == '%' || c == '*') {
					if (type != 0) {
						ret.add(LikeParser.BETWEEN);
					}
					type = 0; // 文字数無限.
					pos = 0;
				} else if (c == '_' || c == '?') {
					if (type == -1) {
						type = 1; // 文字数指定.
						pos = 1;
					}
					// 連続文字数指定.
					else if (type == 1) {
						pos++;
					}
				}
			}
			// 検索文字列.
			else {

				if (c == '\\') {
					yenFlag = true;
					continue;
				} else if (buf == null) {
					if (type == 1) { // 文字数指定
						ret.add(pos);
					}
					buf = new StringBuilder();
				}
				buf.append(c);

				// 初期化.
				type = -1;
				pos = 0;
			}
			// \\ではない.
			yenFlag = false;
		}
		// 後処理.
		if (type == 0) { // 文字数無限.
			if (buf != null) {
				ret.add(buf.toString());
				buf = null;
			}
			if ((Integer) ret.get(ret.size() - 1) != LikeParser.BETWEEN) {
				ret.add(LikeParser.BETWEEN);
			}
		} else if (type == 1 && pos > 0) { // 文字数指定.
			if (buf != null) {
				ret.add(buf.toString());
				buf = null;
			}
			ret.add(pos);
		} else if (buf != null) { // 検索文字が残っている.
			if (ret.size() == 0) {
				ret.add(LikeParser.FIRST);
			}
			ret.add(LikeParser.LAST);
			ret.add(buf.toString());
			buf = null;
		}
		return new LikeParser(ret.toArray(), like);
	}
	
	/**
	public static final void main(String[] args) {
		LikeParser parser = null;
		
		final String[] rows = new String[] {
			"suzuki"
			,"tanaka"
			,"sato"
		};
		final int lenRow = rows.length;
		
		final String[] likes = new String[] {
			"tana%"
			,"%a%"
			,"%ki"
			,"%t_"
			
		};
		final int lenLike = likes.length;
		
		for(int i = 0; i < lenLike; i ++) {
			if(i != 0) {
				System.out.println();
			}
			parser = LikeAnalysis.analysis(likes[i]);
			
			for(int j = 0; j < lenRow; j ++) {
				if(parser.match(rows[j])) {
					System.out.println("[" + j + "] " + parser.getSrc() + " " + rows[j]);
				}
			}
		}
	}
	**/
}
