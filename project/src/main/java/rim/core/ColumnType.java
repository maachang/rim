package rim.core;

import rim.exception.RimException;
import rim.util.Alphabet;
import rim.util.DateUtil;
import rim.util.TypesUtil;

/**
 * Rimの列の型定義.
 */
public enum ColumnType {
	Boolean(1),
	Byte(10),
	Short(11),
	Integer(12),
	Long(13),
	Float(14),
	Double(15),
	String(20),
	Date(30);

	private int no;
	private ColumnType(int no) {
		this.no = no;
	}

	/**
	 * 型番号を取得.
	 * @return
	 */
	public int getNo() {
		return no;
	}

	/**
	 * 型変換.
	 * @param v
	 * @return
	 */
	public Object convert(Object v) {
		if(v != null) {
			switch(no) {
			case 1: return TypesUtil.getBoolean(v);
			case 10: return TypesUtil.getByte(v);
			case 11: return TypesUtil.getShort(v);
			case 12: return TypesUtil.getInteger(v);
			case 13: return TypesUtil.getLong(v);
			case 14: return TypesUtil.getFloat(v);
			case 15: return TypesUtil.getDouble(v);
			case 20: return TypesUtil.getString(v);
			case 30: return DateUtil.parseDate(v);
			}
		}
		return null;
	}

	/**
	 * インデックス番号から取得.
	 * @param no
	 * @return
	 */
	public static final ColumnType get(int no) {
		switch(no) {
		case 1: return Boolean;
		case 10: return Byte;
		case 11: return Short;
		case 12: return Integer;
		case 13: return Long;
		case 14: return Float;
		case 15: return Double;
		case 20: return String;
		case 30: return Date;
		}
		throw new RimException("Index type numbers do not match.");
	}

	/**
	 * 文字列から取得.
	 * @param type
	 * @return
	 */
	public static final ColumnType get(String type) {
		if(type == null || type.isEmpty()) {
			throw new RimException("No string specified.");
		}
		if(Alphabet.eqArray(type, "bool", "boolean", "flag") != -1) {
			return Boolean;
		} else if(Alphabet.eqArray(type, "byte", "number8") != -1) {
			return Byte;
		} else if(Alphabet.eqArray(type, "short", "number16") != -1) {
			return Short;
		} else if(Alphabet.eqArray(type, "int", "integer", "number32") != -1) {
			return Integer;
		} else if(Alphabet.eqArray(type, "long", "bigint", "biginteger", "number64") != -1) {
			return Long;
		} else if(Alphabet.eqArray(type, "float") != -1) {
			return Float;
		} else if(Alphabet.eqArray(type, "double", "decimal") != -1) {
			return Double;
		} else if(Alphabet.eqArray(type, "string") != -1) {
			return String;
		} else if(Alphabet.eqArray(type, "date", "datetime") != -1) {
			return Date;
		}
		throw new RimException("Specified character types do not match: " + type);
	}
}
