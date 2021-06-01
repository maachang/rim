package rim.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 日付関連ユーティリティ.
 */
public class DateUtil {
	private DateUtil() {}

	/** グリニッジ標準時タイムゾーン. **/
	protected static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("Europe/London");

	/** HTTPタイムスタンプ条件を生成. **/
	private static final String[] _TIMESTAMP_TO_WEEK;
	private static final String[] _TIMESTAMP_TO_MONTH;
	static {
		String[] n = new String[9];
		n[1] = "Sun, ";
		n[2] = "Mon, ";
		n[3] = "Tue, ";
		n[4] = "Wed, ";
		n[5] = "Thu, ";
		n[6] = "Fri, ";
		n[7] = "Sat, ";

		String[] nn = new String[12];
		nn[0] = "Jan";
		nn[1] = "Feb";
		nn[2] = "Mar";
		nn[3] = "Apr";
		nn[4] = "May";
		nn[5] = "Jun";
		nn[6] = "Jul";
		nn[7] = "Aug";
		nn[8] = "Sep";
		nn[9] = "Oct";
		nn[10] = "Nov";
		nn[11] = "Dec";

		_TIMESTAMP_TO_WEEK = n;
		_TIMESTAMP_TO_MONTH = nn;
	}

	/*
	 * HTTPタイムスタンプを取得.
	 *
	 * @param date 出力対象の日付オブジェクトを設定します.
	 * @return String タイムスタンプ値が返却されます.
	 */
	public static final String toRfc822(java.util.Date date) {
		return toRfc822(true, date);
	}

	/**
	 * HTTPタイムスタンプを取得.
	 *
	 * @param mode [true]の場合、ハイフン区切りの条件で出力します.
	 * @param date 出力対象の日付オブジェクトを設定します.
	 * @return String タイムスタンプ値が返却されます.
	 */
	public static final String toRfc822(boolean mode, java.util.Date date) {
		StringBuilder buf = new StringBuilder();
		toRfc822(buf, mode, date);
		return buf.toString();
	}

	/**
	 * HTTPタイムスタンプを取得.
	 *
	 * @param buf  出力先のStringBuilderを設定します.
	 * @param date 出力対象の日付オブジェクトを設定します.
	 */
	public static final void toRfc822(StringBuilder buf, java.util.Date date) {
		toRfc822(buf, true, date);
	}

	/**
	 * HTTPタイムスタンプを取得.
	 *
	 * @param buf  出力先のStringBuilderを設定します.
	 * @param mode [true]の場合、ハイフン区切りの条件で出力します.
	 * @param date 出力対象の日付オブジェクトを設定します.
	 */
	public static final void toRfc822(StringBuilder buf, boolean mode, java.util.Date date) {
		try {
			String tmp;
			Calendar cal = new GregorianCalendar(GMT_TIMEZONE);
			cal.setTime(date);
			buf.append(_TIMESTAMP_TO_WEEK[cal.get(Calendar.DAY_OF_WEEK)]);
			tmp = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
			if (mode) {
				buf.append("00".substring(tmp.length())).append(tmp).append("-");
				buf.append(_TIMESTAMP_TO_MONTH[cal.get(Calendar.MONTH)]).append("-");
			} else {
				buf.append("00".substring(tmp.length())).append(tmp).append(" ");
				buf.append(_TIMESTAMP_TO_MONTH[cal.get(Calendar.MONTH)]).append(" ");
			}
			tmp = String.valueOf(cal.get(Calendar.YEAR));
			buf.append("0000".substring(tmp.length())).append(tmp).append(" ");
			tmp = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
			buf.append("00".substring(tmp.length())).append(tmp).append(":");

			tmp = String.valueOf(cal.get(Calendar.MINUTE));
			buf.append("00".substring(tmp.length())).append(tmp).append(":");

			tmp = String.valueOf(cal.get(Calendar.SECOND));
			buf.append("00".substring(tmp.length())).append(tmp).append(" ");
			buf.append("GMT");
		} catch (Exception e) {
			throw new DateException("Timestamp generation failed.", e);
		}
	}


	/**
	 * Web上の日付フォーマットをjava.util.Dateに変換できるかチェック.
	 *
	 * @param value
	 *            変換対象のHTMLタイムスタンプを設定します.
	 * @return boolean [true]の場合は変換が可能です.
	 */
	public static final boolean isRfc822(String value) {
		try {
			return toRfc822(value) != null;
		} catch(Exception e) {
			return false;
		}
	}

	/**
	 * Web上の日付フォーマットをjava.util.Dateに変換.
	 * こんな感じのフォーマット[Wed, 19-Mar-2014 03:55:33 GMT]を解析して
	 * 日付フォーマットに変換します.
	 *
	 * @param value
	 *            変換対象のHTMLタイムスタンプを設定します.
	 * @return Date 変換された時間が返されます.
	 */
	public static final java.util.Date toRfc822(String value) {
		if (value == null || (value = value.trim()).length() <= 0) {
			return null;
		}
		char c;
		int len = value.length();
		List<String> list = new ArrayList<String>();
		StringBuilder buf = null;
		for (int i = 0; i < len; i++) {
			c = value.charAt(i);
			if (c != ' ' && c != '\t' && c != ',' && c != ':' && c != '-') {
				if (buf == null) {
					buf = new StringBuilder();
				}
				buf.append(c);
			} else if (buf != null) {
				list.add(buf.toString());
				buf = null;
			}
		}
		if (buf != null) {
			list.add(buf.toString());
			buf = null;
		}
		len = list.size();
		if (len == 8) {
			Calendar cal = new GregorianCalendar(GMT_TIMEZONE);
			cal.clear();
			cal.set(Calendar.DAY_OF_MONTH, TypesUtil.getInteger(list.get(1)));
			String month = list.get(2).toLowerCase();
			if ("jan".equals(month)) {
				cal.set(Calendar.MONTH, 0);
			} else if ("feb".equals(month)) {
				cal.set(Calendar.MONTH, 1);
			} else if ("mar".equals(month)) {
				cal.set(Calendar.MONTH, 2);
			} else if ("apr".equals(month)) {
				cal.set(Calendar.MONTH, 3);
			} else if ("may".equals(month)) {
				cal.set(Calendar.MONTH, 4);
			} else if ("jun".equals(month)) {
				cal.set(Calendar.MONTH, 5);
			} else if ("jul".equals(month)) {
				cal.set(Calendar.MONTH, 6);
			} else if ("aug".equals(month)) {
				cal.set(Calendar.MONTH, 7);
			} else if ("sep".equals(month)) {
				cal.set(Calendar.MONTH, 8);
			} else if ("oct".equals(month)) {
				cal.set(Calendar.MONTH, 9);
			} else if ("nov".equals(month)) {
				cal.set(Calendar.MONTH, 10);
			} else if ("dec".equals(month)) {
				cal.set(Calendar.MONTH, 11);
			}
			cal.set(Calendar.YEAR, TypesUtil.getInteger(list.get(3)));
			cal.set(Calendar.HOUR_OF_DAY, TypesUtil.getInteger(list.get(4)));
			cal.set(Calendar.MINUTE, TypesUtil.getInteger(list.get(5)));
			cal.set(Calendar.SECOND, TypesUtil.getInteger(list.get(6)));
			return new java.util.Date(cal.getTime().getTime());
		}
		throw new DateException("Incorrect webTime format:" + value);
	}

	/**
	 * DateオブジェクトをISO8601形式(yyyy-MM-dd'T'HH:mm:ssXXX)で文字列変換.
	 * @param d 日付オブジェクトを設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String toISO8601(java.util.Date d) {
		OffsetDateTime odm = OffsetDateTime.ofInstant(
			d.toInstant(), ZoneId.systemDefault());
		return odm.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	/**
	 * ISO8601形式(yyyy-MM-dd'T'HH:mm:ssXXX)の文字列から
	 * Dateオブジェクトに変換.
	 * @param s ISO8601形式の文字列を設定します.
	 * @return Date Dateオブジェクトが返却されます.
	 */
	public static final java.util.Date toISO8601(String s) {
		OffsetDateTime odm = null;
		if(s.indexOf("+") == -1) {
			// +09:00 などが無い場合は
			// LocalDateTimeのフォーマッタで変換を試みる.
			odm = OffsetDateTime.parse(
				s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} else {
			// +09:00 がある場合は
			// OffsetDateTimeのフォーマッタで変換を試みる.
			odm = OffsetDateTime.parse(
				s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		}
		return java.util.Date.from(odm.toInstant());
	}

	/**
	 * 指定文字がISO8601形式かチェック.
	 * @param s
	 * @return
	 */
	public static final boolean isISO8601(String s) {
		int code = 0;
		int len = s.length();
		char c;
		for (int i = 0; i < len; i++) {
			c = s.charAt(i);
			switch (code) {
			case 0:
			case 1:
				if (c == '-') {
					code++;
				} else if (!(c >= '0' && c <= '9')) {
					return false;
				}
				break;
			case 2:
				if (c == 'T') {
					code++;
				} else if (!(c >= '0' && c <= '9')) {
					return false;
				}
				break;
			case 3:
			case 4:
				if (c == ':') {
					code++;
				} else if (!(c >= '0' && c <= '9')) {
					return false;
				}
				break;
			case 5:
				return true;
			}
		}
		return false;
	}

	/**
	 * Dateオブジェクトをで文字列変換.
	 * @param d 日付オブジェクトを設定します.
	 * @param fm DateTimeFormatter を設定します.
	 * @return String 文字列が返却されます.
	 */
	public static final String toString(java.util.Date d, DateTimeFormatter fm) {
		OffsetDateTime odm = OffsetDateTime.ofInstant(
			d.toInstant(), ZoneId.systemDefault());
		return odm.format(fm);
	}

	/**
	 * 文字列からDateオブジェクトに変換.
	 * @param s 文字列を設定します.
	 * @param fm DateTimeFormatter を設定します.
	 * @return Date Dateオブジェクトが返却されます.
	 */
	public static final java.util.Date toDate(String s, DateTimeFormatter fm) {
		OffsetDateTime odm = OffsetDateTime.parse(s, fm);
		return java.util.Date.from(odm.toInstant());
	}

	// 汎用変換処理.
	private static final DateTimeFormatter[] BASIC_DTF = new DateTimeFormatter[] {
		DateTimeFormatter.ofPattern("yyyy-MM-dd")
		,DateTimeFormatter.ofPattern("yyyy/MM/dd")
		,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		,DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
	};

	// 数値文字列.
	private static final DateTimeFormatter[] NUMBER_DTF = new DateTimeFormatter[] {
		DateTimeFormatter.ofPattern("yyyyMMdd")
		,DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
	};

	// 拡張変換文字から日付変換フォーマット.
	private static final Queue<DateTimeFormatter> EXTENTION_DTF =
		new ConcurrentLinkedQueue<DateTimeFormatter>();

	/**
	 * parseDateでの拡張文字列から日付変換フォーマットを設定します.
	 * @param format 日付フォーマットを設定します.
	 */
	public static final void setExtensionDtf(String format) {
		if(format == null || !format.isEmpty()) {
			return;
		}
		EXTENTION_DTF.offer(DateTimeFormatter.ofPattern(format));
	}

	/**
	 * オブジェクトをDateオブジェクトに変換.
	 * @param v 対象のオブジェクトを設定します.
	 * @return java.util.Date Dateオブジェクトが返却されます.
	 */
	public static final java.util.Date parseDate(Object v) {
		if(v == null) {
			return null;
		} else if(v instanceof java.util.Date) {
			return (java.util.Date)v;
		} else if(v instanceof Number) {
			return new java.util.Date(((Number)v).longValue());
		} else if(v instanceof String) {
			String s = (String)v;
			if(isISO8601(s)) {
				return toISO8601(s);
			} else if(TypesUtil.isNumeric(s)) {
				int len = NUMBER_DTF.length;
				for(int i = 0; i < len; i ++) {
					try {
						return toDate(s, NUMBER_DTF[i]);
					} catch(Exception e) {}
				}
				return new java.util.Date(TypesUtil.getLong(s));
			}
			try {
				return toRfc822(s);
			} catch(Exception e) {}
			int len = BASIC_DTF.length;
			for(int i = 0; i < len; i ++) {
				try {
					return toDate(s, BASIC_DTF[i]);
				} catch(Exception e) {}
			}
			if(EXTENTION_DTF.size() > 0) {
				Iterator<DateTimeFormatter> it = EXTENTION_DTF.iterator();
				while(it.hasNext()) {
					try {
						return toDate(s, it.next());
					} catch(Exception e) {}
				}
			}
		} else if(v instanceof LocalDateTime) {
			return java.util.Date.from(
				((LocalDateTime)v).atZone(ZoneId.systemDefault()).toInstant());
		} else if(v instanceof OffsetDateTime) {
			return java.util.Date.from(((OffsetDateTime)v).toInstant());
		} else if(v instanceof ZonedDateTime) {
			return java.util.Date.from(((ZonedDateTime)v).toInstant());
		}
		throw new DateException("Failed to convert Date object.");
	}
}
