package rim.index;

import rim.core.ColumnType;

/**
 * RimIndexユーティリティ.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class RimIndexUtil {
	private RimIndexUtil() {}
	
	/**
	 * Bodyの行数に対するバイト数を取得します.
	 * @param len 行数を設定します.
	 * @return int １～４バイトの長さが返却されます.
	 */
	public static final int getRowByteLength(final int len) {
		if(len > 65535) {
			return 4;
		} else if(len > 255) {
			return 2;
		}
		return 1;
	}
	
	/**
	 * インデックス要素を作成.
	 * @param indexByte 行番号が格納できる最低限のバイト数を設定します.
	 * @param columnType 行型を設定します.
	 * @param value 要素を設定します.
	 * @param rowIds 行Id群を設定します.
	 * @param len 有効な長さを設定します.
	 * @return int 現在までの追加件数を取得.
	 */
	public static final RimIndexElement createRimIndexElement(
		int indexByte, ColumnType columnType, Comparable value,
		int[] rowIds, int len) {
		switch(indexByte) {
		case 1:
			return new RimIndexElement1(columnType, value, rowIds, len);
		case 2:
			return new RimIndexElement2(columnType, value, rowIds, len);
		case 4:
			return new RimIndexElement4(columnType, value, rowIds, len);
		}
		return new RimIndexElement4(columnType, value, rowIds, len);
	}
	
	/**
	 * 行数が２５５以下でのインデックス要素.
	 */
	protected static final class RimIndexElement1 implements RimIndexElement {
		private ColumnType columnType;
		private Comparable value;
		private byte[] rows;

		public RimIndexElement1(
			ColumnType columnType, Comparable value, int[] list, int len) {
			final byte[] r = new byte[len];
			for(int i = 0; i < len; i ++) {
				r[i] = (byte)(list[i] & 0x000000ff);
			}
			this.columnType = columnType;
			this.rows = r;
			this.value = value;
		}
		
		@Override
		public ColumnType getColumnType() {
			return columnType;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getLineLength() {
			return rows.length;
		}

		@Override
		public int getLineNo(int no) {
			return rows[no] & 0x000000ff;
		}
		
		@Override
		public int compareTo(Object o) {
			if(o instanceof RimIndexElement1) {
				return value.compareTo(
					((RimIndexElement1)o).value);
			} else {
				return value.compareTo(
					(Comparable)getColumnType().convert(o));
			}
		}
	}

	/**
	 * 行数が６５５３５以下でのインデックス要素.
	 */
	protected static final class RimIndexElement2 implements RimIndexElement {
		private ColumnType columnType;
		private Comparable value;
		private short[] rows;

		public RimIndexElement2(
			ColumnType columnType, Comparable value, int[] list, int len) {
			final short[] r = new short[len];
			for(int i = 0; i < len; i ++) {
				r[i] = (short)(list[i] & 0x0000ffff);
			}
			this.columnType = columnType;
			this.rows = r;
			this.value = value;
		}
		
		@Override
		public ColumnType getColumnType() {
			return columnType;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getLineLength() {
			return rows.length;
		}

		@Override
		public int getLineNo(int no) {
			return rows[no] & 0x0000ffff;
		}
		
		@Override
		public int compareTo(Object o) {
			if(o instanceof RimIndexElement2) {
				return value.compareTo(
					((RimIndexElement2)o).value);
			} else {
				return value.compareTo(
					(Comparable)getColumnType().convert(o));
			}
		}
	}

	/**
	 * 行数が６５５３５以上でのインデックス要素.
	 */
	protected static final class RimIndexElement4 implements RimIndexElement {
		private ColumnType columnType;
		private Comparable value;
		private int[] rows;

		public RimIndexElement4(
			ColumnType columnType, Comparable value, int[] list, int len) {
			int[] r = new int[len];
			System.arraycopy(list, 0, r, 0, len);
			this.columnType = columnType;
			this.rows = r;
			this.value = value;
		}
		
		@Override
		public ColumnType getColumnType() {
			return columnType;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getLineLength() {
			return rows.length;
		}

		@Override
		public int getLineNo(int no) {
			return rows[no];
		}
		
		@Override
		public int compareTo(Object o) {
			if(o instanceof RimIndexElement4) {
				return value.compareTo(
					((RimIndexElement4)o).value);
			} else {
				return value.compareTo(
					(Comparable)getColumnType().convert(o));
			}
		}
	}

}
