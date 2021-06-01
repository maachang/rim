package rim.util;

import java.util.List;

/**
 * １行のデータを扱う場合の行情報.
 */
public final class CsvRow implements TypesKeyValue<Object, String> {
	private FixedSearchArray<String> header;
	private List<String> rowData;
	private int[] types;

	public CsvRow() {
	}

	public CsvRow(FixedSearchArray<String> h) {
		set(h);
	}

	public CsvRow set(FixedSearchArray<String> h) {
		header = h;
		types = null;
		if (rowData != null) {
			int len = rowData.size();
			types = new int[len];
			for (int i = 0; i < len; i++) {
				types[i] = -1;
			}
		}
		return this;
	}

	public CsvRow set(List<String> r) {
		rowData = r;
		int len = r.size();
		if (types == null || types.length != len) {
			types = new int[len];
			for (int i = 0; i < len; i++) {
				types[i] = -1;
			}
		}
		return this;
	}

	public FixedSearchArray<String> getHeader() {
		return header;
	}

	private int getColumnNo(Object key) {
		Integer n;
		if (key == null) {
			return -1;
		}
		// 数値だった場合は、番号で処理.
		else if ((n = TypesUtil.getInteger(key)) != null) {
			if (n >= 0 && n < header.size()) {
				return n;
			}
			return -1;
		}
		return header.search(key.toString());
	}

	@Override
	public String get(Object key) {
		int n = getColumnNo(key);
		if (n == -1) {
			return null;
		}
		return rowData.get(n);
	}

	public boolean containsKey(Object key) {
		return (getColumnNo(key) == -1) ? false : true;
	}

	public int size() {
		return header.size();
	}

	@Override
	public String toString() {
		final int len = header.size();
		StringBuilder buf = new StringBuilder("{");
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				buf.append(",");
			}
			String v = rowData.get(i);
			buf.append(header.get(i)).append(": ");
			buf.append(v);
		}
		buf.append("}");
		return buf.toString();
	}
}
