package rim;

import rim.exception.RimException;
import rim.util.ObjectList;

/**
 * Rim-インデックス情報.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RimIndex {

	/**
	 * 1つのインデックス要素.
	 */
	protected static interface RimIndexElement extends Comparable<RimIndexElement> {
		/**
		 * Valueを取得.
		 * @return
		 */
		public Comparable getValue();

		/**
		 * Valueに対する行番号の管理数を取得.
		 * @return
		 */
		public int getRowSize();

		/**
		 * 指定された位置の行番号を取得.
		 * @param no
		 * @return
		 */
		public int getRow(int no);

		@Override
		default int compareTo(RimIndexElement o) {
			return getValue().compareTo(o.getValue());
		}
	}

	/**
	 * 行数が２５５以下でのインデックス要素.
	 */
	private static final class RimIndexElement1 implements RimIndexElement {
		private Comparable value;
		private byte[] rows;

		public RimIndexElement1(Comparable value, int[] list, int len) {
			final byte[] r = new byte[len];
			for(int i = 0; i < len; i ++) {
				r[i] = (byte)(list[i] & 0x000000ff);
			}
			this.rows = r;
			this.value = value;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getRowSize() {
			return rows.length;
		}

		@Override
		public int getRow(int no) {
			return rows[no] & 0x000000ff;
		}
	}

	/**
	 * 行数が６５５３５以下でのインデックス要素.
	 */
	private static final class RimIndexElement2 implements RimIndexElement {
		private Comparable value;
		private short[] rows;

		public RimIndexElement2(Comparable value, int[] list, int len) {
			final short[] r = new short[len];
			for(int i = 0; i < len; i ++) {
				r[i] = (short)(list[i] & 0x0000ffff);
			}
			this.rows = r;
			this.value = value;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getRowSize() {
			return rows.length;
		}

		@Override
		public int getRow(int no) {
			return rows[no] & 0x0000ffff;
		}
	}

	/**
	 * 行数が６５５３５以上でのインデックス要素.
	 */
	private static final class RimIndexElement4 implements RimIndexElement {
		private Comparable value;
		private int[] rows;

		public RimIndexElement4(Comparable value, int[] list, int len) {
			int[] r = new int[len];
			System.arraycopy(list, 0, r, 0, len);
			this.rows = r;
			this.value = value;
		}

		@Override
		public Comparable getValue() {
			return value;
		}

		@Override
		public int getRowSize() {
			return rows.length;
		}

		@Override
		public int getRow(int no) {
			return rows[no];
		}
	}

	// 列番号.
	private int columnNo;
	// 列名.
	private String columnName;
	// インデックスの列型.
	private ColumnType columnType;
	// インデックス内の行を管理するバイト数.
	private int indexByte;
	// インデックス情報.
	private ObjectList<RimIndexElement> index = new ObjectList<RimIndexElement>();
	// fixしたインデックス情報.
	RimIndexElement[] fixIndex = null;
	// インデックス総数.
	private int indexSize = 0;
	// 登録予定のインデックス総行数.
	private int planIndexSize;

	/**
	 * コンストラクタ.
	 * @param columnNo このインデックスの列番号が設定されます.
	 * @param columnName このインデックスの列名が設定されます.
	 * @param columnType このインデックスの列型が設定されます.
	 * @param rowsSize Bodyデータの総行数を設定されます.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 */
	protected RimIndex(int columnNo, String columnName, ColumnType columnType,
		int rowsSize, int planIndexSize) {
		this.columnNo = columnNo;
		this.columnName = columnName;
		this.columnType = columnType;
		this.indexByte = getRowByteLength(rowsSize);
		this.planIndexSize = planIndexSize;
	}

	// Bodyの行数に対するバイト数を取得します.
	private static final int getRowByteLength(final int len) {
		if(len > 65535) {
			return 4;
		} else if(len > 255) {
			return 2;
		}
		return 1;
	}

	/**
	 * １つのインデックス要素を追加.
	 * @param value 要素を設定します.
	 * @param rowIds 行Id群を設定します.
	 * @param len 有効な長さを設定します.
	 * @return int 現在までの追加件数を取得.
	 */
	protected int add(Comparable value, int[] rowIds, int len) {
		if(index == null) {
			throw new RimException("It has already been confirmed.");
		}
		final RimIndexElement em;
		switch(indexByte) {
		case 1:
			em = new RimIndexElement1(value, rowIds, len);
			break;
		case 2:
			em = new RimIndexElement2(value, rowIds, len);
			break;
		case 4:
			em = new RimIndexElement4(value, rowIds, len);
			break;
		default :
			em = new RimIndexElement4(value, rowIds, len);
			break;
		}
		index.add(em);
		indexSize += len;
		return indexSize;
	}

	/**
	 * 追加処理が完了した場合に呼び出します.
	 */
	protected void fix() {
		if(index == null) {
			return;
		} else if(planIndexSize != indexSize) {
			throw new RimException(
				"It does not match the expected number of index rows(" +
				planIndexSize + "/" + indexSize + ")");
		}
		fixIndex = index.toArray(RimIndexElement.class);
		index = null;
	}

	/**
	 * 追加処理がFixしているか取得.
	 * @return boolean trueの場合Fixしています.
	 */
	protected boolean isFix() {
		return index == null;
	}

	/**
	 * 行追加が完了しているかチェック.
	 */
	protected void checkFix() {
		if(!isFix()) {
			throw new RimException("Index addition is not complete.");
		}
	}

	/**
	 * インデックス対象の列番号を取得.
	 * @return int インデックス対象の列番号が返却されます.
	 */
	public int getColumnNo() {
		return columnNo;
	}

	/**
	 * インデックス対象の列名を取得.
	 * @return String インデックス対象の列名が返却されます.
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * インデックス対象の列型を取得.
	 * @return ColumnType インデックス対象の列型が返却されます.
	 */
	public ColumnType getColumnType() {
		return columnType;
	}

	/**
	 * 設定されているインデックス行数を取得.
	 * @return int インデックス行数が返却されます.
	 */
	public int getLength() {
		return indexSize;
	}
}
