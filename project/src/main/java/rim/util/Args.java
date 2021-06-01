package rim.util;

import java.util.List;

/**
 * コマンドライン引数管理オブジェクト.
 *
 * たとえば args = ["-a", "hoge"] のような実行引数が設定されていた場合.
 *
 * String[] args = ...;
 * Args argsObject = new Args(args);
 * String value = argsObject.get("-a");
 * value.equals("hoge") == true;
 *
 * のような形で取得が出来ます.
 */
public class Args {
	// Argsオブジェクトの内容.
	private String[] args;

	/**
	 * コンストラクタ.
	 */
	public Args() {
		this(new String[0]);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param args
	 *            mainの引数を設定します.
	 */
	public Args(String... args) {
		this.args = args;
	}

	/**
	 * コンストラクタ.
	 *
	 * @param list 引数群を設定します.
	 */
	public Args(List<String> list) {
		int len = list.size();
		args = new String[len];
		for(int i = 0; i < len; i ++) {
			args[i] = list.get(i);
		}
	}

	/**
	 * このオブジェクトに設定されたコマンド引数を取得.
	 *
	 * @return String[]
	 */
	public String[] getArgs() {
		return args;
	}

	/**
	 * 指定ヘッダ名を設定して、要素を取得します.
	 *
	 * @param names 対象のヘッダ名を設定します.
	 * @return String 文字列が返却されます.
	 */
	public String get(String... names) {
		return next(0, names);
	}

	/**
	 * 番号指定での指定ヘッダ名を指定した要素取得.
	 *
	 * たとえば
	 * > -i abc -i def -i xyz
	 *
	 * このような情報が定義されてる場合にたとえば
	 * next(0, "-i") なら "abc" が返却され
	 * next(1, "-i") なら "def" が返却されます.
	 *
	 * @param no 取得番目番号を設定します.
	 * @param names 対象のヘッダ名を設定します.
	 * @return String 文字列が返却されます.
	 */
	public String next(int no, String... names) {
		final int len = names.length;
		if(len == 1 && TypesUtil.isNumeric(names[0])) {
			final int pos = TypesUtil.getInteger(names[0]);
			if(pos >= 0 && pos < args.length) {
				return args[pos];
			}
			return null;
		}
		int cnt = 0;
		final int lenJ = args.length - 1;
		for(int i = 0; i < len; i ++) {
			for (int j = 0; j < lenJ; j++) {
				if (names[i].equals(args[j])) {
					if(no <= cnt) {
						return args[j + 1];
					}
					cnt ++;
				}
			}
		}
		return null;
	}

	/**
	 * 指定ヘッダ名を指定して、そのヘッダ名が存在するかチェックします.
	 *
	 * @param names
	 * @return boolean
	 */
	public boolean isValue(String... names) {
		final int len = names.length;
		final int lenJ = args.length;
		for(int i = 0; i < len; i ++) {
			if(TypesUtil.isNumeric(names[i])) {
				final int no = TypesUtil.getInteger(names[i]);
				if(no >= 0 && no < args.length) {
					return true;
				}
			} else {
				for (int j = 0; j < lenJ; j++) {
					if (names[i].equals(args[j])) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 最初のパラメータを取得.
	 * @return
	 */
	public String getFirst() {
		if(args.length == 0) {
			return "";
		}
		return args[0];
	}

	/**
	 * 一番うしろのパラメータを取得.
	 * @return
	 */
	public String getLast() {
		if(args.length == 0) {
			return "";
		}
		return args[args.length - 1];
	}

	/**
	 * パラメータ数を取得.
	 * @return
	 */
	public int size() {
		return args.length;
	}

	/**
	 * boolean情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Boolean 情報が返却されます.
	 */
	public Boolean getBoolean(String... n) {
		return TypesUtil.getBoolean(get(n));
	}

	/**
	 * int情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Integer 情報が返却されます.
	 */
	public Integer getInt(String... n) {
		return TypesUtil.getInteger(get(n));
	}

	/**
	 * long情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Long 情報が返却されます.
	 */
	public Long getLong(String... n) {
		return TypesUtil.getLong(get(n));
	}

	/**
	 * float情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Float 情報が返却されます.
	 */
	public Float getFloat(String... n) {
		return TypesUtil.getFloat(get(n));
	}

	/**
	 * double情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Double 情報が返却されます.
	 */
	public Double getDouble(String... n) {
		return TypesUtil.getDouble(get(n));
	}

	/**
	 * String情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return String 情報が返却されます.
	 */
	public String getString(String... n) {
		return TypesUtil.getString(get(n));
	}

	/**
	 * Date情報を取得.
	 *
	 * @parma n 対象の条件を設定します.
	 * @return Date 情報が返却されます.
	 */
	public java.util.Date getDate(String... n) {
		return DateUtil.parseDate(get(n));
	}

	/**
	 * boolean情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Boolean 情報が返却されます.
	 */
	public Boolean nextBoolean(int no, String... n) {
		return TypesUtil.getBoolean(next(no, n));
	}

	/**
	 * int情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Integer 情報が返却されます.
	 */
	public Integer nextInt(int no, String... n) {
		return TypesUtil.getInteger(next(no, n));
	}

	/**
	 * long情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Long 情報が返却されます.
	 */
	public Long nextLong(int no, String... n) {
		return TypesUtil.getLong(next(no, n));
	}

	/**
	 * float情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Float 情報が返却されます.
	 */
	public Float nextFloat(int no, String... n) {
		return TypesUtil.getFloat(next(no, n));
	}

	/**
	 * double情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Double 情報が返却されます.
	 */
	public Double nextDouble(int no, String... n) {
		return TypesUtil.getDouble(next(no, n));
	}

	/**
	 * String情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return String 情報が返却されます.
	 */
	public String nextString(int no, String... n) {
		return TypesUtil.getString(next(no, n));
	}

	/**
	 * Date情報を取得.
	 *
	 * @param no 取得番目番号を設定します.
	 * @parma n 対象の条件を設定します.
	 * @return Date 情報が返却されます.
	 */
	public java.util.Date nextDate(int no, String... n) {
		return DateUtil.parseDate(next(no, n));
	}
}

