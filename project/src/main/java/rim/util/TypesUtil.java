package rim.util;

/**
 * オブジェクトからの型変換処理系.
 */
public class TypesUtil {

	/**
	 * 対象が数字情報かチェック.
	 * @param o オブジェクトを設定します.
	 * @return boolean true の場合数値情報です.
	 */
	public static final boolean isNumeric(Object o) {
		if(o instanceof String) {
			try {
				Double.parseDouble((String)o);
				return true;
			} catch(Exception e) {
			}
		} else if(o instanceof Number) {
			return true;
		}
		return false;
	}

	/**
	 * Booleanで取得.
	 * @param o オブジェクトを設定します.
	 * @return Boolean 変換できない場合はnull返却.
	 */
	public static final Boolean getBoolean(Object o) {
		if(o instanceof Boolean) {
			return (Boolean)o;
		} else if(o instanceof String) {
			final String s = (String)o;
			if(Alphabet.eq("true", s)) {
				return true;
			} else if(Alphabet.eq("false", s)) {
				return false;
			}
		} else if(o instanceof Number) {
			return ((Number)o).intValue() != 0;
		}
		return null;
	}

	/**
	 * Byteで取得.
	 * @param o オブジェクトを設定します.
	 * @return Byte 変換できない場合はnull返却.
	 */
	public static final Byte getByte(Object o) {
		if(o instanceof Number) {
			return ((Number)o).byteValue();
		} else if(o instanceof String) {
			try {
				return (byte)Integer.parseInt((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Shortで取得.
	 * @param o オブジェクトを設定します.
	 * @return Short 変換できない場合はnull返却.
	 */
	public static final Short getShort(Object o) {
		if(o instanceof Number) {
			return ((Number)o).shortValue();
		} else if(o instanceof String) {
			try {
				return (short)Integer.parseInt((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Integerで取得.
	 * @param o オブジェクトを設定します.
	 * @return Integer 変換できない場合はnull返却.
	 */
	public static final Integer getInteger(Object o) {
		if(o instanceof Number) {
			return ((Number)o).intValue();
		} else if(o instanceof String) {
			try {
				return Integer.parseInt((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Longで取得.
	 * @param o オブジェクトを設定します.
	 * @return Long 変換できない場合はnull返却.
	 */
	public static final Long getLong(Object o) {
		if(o instanceof Number) {
			return ((Number)o).longValue();
		} else if(o instanceof String) {
			try {
				return Long.parseLong((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Floatで取得.
	 * @param o オブジェクトを設定します.
	 * @return Float 変換できない場合はnull返却.
	 */
	public static final Float getFloat(Object o) {
		if(o instanceof Number) {
			return ((Number)o).floatValue();
		} else if(o instanceof String) {
			try {
				return Float.parseFloat((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Doubleで取得.
	 * @param o オブジェクトを設定します.
	 * @return Double 変換できない場合はnull返却.
	 */
	public static final Double getDouble(Object o) {
		if(o instanceof Number) {
			return ((Number)o).doubleValue();
		} else if(o instanceof String) {
			try {
				return Double.parseDouble((String)o);
			} catch(Exception e) {}
		}
		return null;
	}

	/**
	 * Stringで取得.
	 * @param o オブジェクトを設定します.
	 * @return String 変換できない場合はnull返却.
	 */
	public static final String getString(Object o) {
		if(o == null) {
			return null;
		} else if(o instanceof String) {
			return (String)o;
		}
		return String.valueOf(o);
	}
}
