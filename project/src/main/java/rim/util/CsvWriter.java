package rim.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvWriter implements Closeable, AutoCloseable {
	private Writer writer;
	private String cutCode;
	private String[] columns;
	private int count;

	/**
	 * コンストラクタ.
	 *
	 * @param n    対象のファイル名を設定します.
	 * @param cset 文字charsetを設定します.
	 * @param c    Csvのカットコードを設定します.
	 * @throws IOException
	 */
	public CsvWriter(String n, String c) throws IOException {
		this(n, "UTF8", c);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param n    対象のファイル名を設定します.
	 * @param cset 文字charsetを設定します.
	 * @param c    Csvのカットコードを設定します.
	 * @throws IOException
	 */
	public CsvWriter(String n, String cset, String c) throws IOException {
		this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(n), cset)), c);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param w writerオブジェクトを設定します.
	 */
	public CsvWriter(Writer w) {
		this(w, ",");
	}

	/**
	 * コンストラクタ.
	 *
	 * @param w writerオブジェクトを設定します.
	 * @param c 区切り文字を設定します.
	 */
	public CsvWriter(Writer w, String c) {
		this.writer = w;
		this.cutCode = c;
	}

	/**
	 * オブジェクトクローズ.
	 *
	 * @exception IOException
	 */
	@Override
	public void close() throws IOException {
		cutCode = null;
		columns = null;
		count = -1;
		if (writer != null) {
			writer.close();
			writer = null;
		}
	}

	/**
	 * 初期化.
	 *
	 * @param c ヘッダカラム名リストを設定します.
	 * @return CsvWriter
	 * @throws IOException
	 */
	public CsvWriter init(List<String> c) throws IOException {
		int len = c.size();
		String[] params = new String[len];
		for (int i = 0; i < len; i++) {
			params[i] = c.get(i);
		}
		return init(params);
	}

	/**
	 * 初期化.
	 *
	 * @param c ヘッダカラム名リストを設定します.
	 * @return CsvWriter
	 * @throws IOException
	 */
	public CsvWriter init(String... c) throws IOException {
		if (c == null || c.length == 0) {
			throw new IOException("Header information is invalid.");
		}
		if (columns != null) {
			throw new IOException("Header is already set.");
		}
		int len = c.length;
		columns = c;
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(cutCode);
			}
			buf.append(c[i]);
		}
		writer.write(buf.toString());
		return this;
	}

	/**
	 * １行情報を書き込み.
	 *
	 * @param line
	 * @return CsvWriter
	 * @exception IOException
	 */
	public CsvWriter writeLine(Map<String, Object> line) throws IOException {
		if (columns == null) {
			throw new IOException("Not initialized.");
		}
		int len = columns.length;
		Object value;
		String string;
		StringBuilder buf = new StringBuilder("\n");
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(cutCode);
			}
			value = line.get(columns[i]);
			if (value != null) {
				string = value.toString();
				if (string.indexOf(cutCode) != -1) {
					buf.append("\"").append(string).append("\"");
				} else {
					buf.append(string);
				}
			}
		}
		count++;
		writer.write(buf.toString());
		return this;
	}

	/**
	 * １行情報を書き込み.
	 *
	 * @param line [key],[value],[key],[value]....の順で設定します.
	 * @return CsvWriter
	 * @exception IOException
	 */
	public CsvWriter writeLine(Object... line) throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		int len = line.length;
		for (int i = 0; i < len; i += 2) {
			if (line[i] == null) {
				throw new IOException("Column name is null.");
			}
			map.put(line[i].toString(), line[i + 1]);
		}
		writeLine(map);
		return this;
	}

	/**
	 * 現在の書き込み行数を取得.
	 *
	 * @return int 行数が返却されます.
	 */
	public int getRowCount() {
		return count;
	}

	public String toString() {
		return "csvWriter";
	}
}
