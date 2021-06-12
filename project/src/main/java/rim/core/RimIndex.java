package rim.core;

import java.util.Arrays;
import java.util.NoSuchElementException;

import rim.exception.RimException;
import rim.util.ObjectList;

/**
 * Rim-インデックス情報.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RimIndex {
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
			em = new RimIndexElement1(columnType, value, rowIds, len);
			break;
		case 2:
			em = new RimIndexElement2(columnType, value, rowIds, len);
			break;
		case 4:
			em = new RimIndexElement4(columnType, value, rowIds, len);
			break;
		default :
			em = new RimIndexElement4(columnType, value, rowIds, len);
			break;
		}
		index.add(em);
		indexSize += len;
		return indexSize;
	}

	/**
	 * 追加処理が完了した場合に呼び出します.
	 */
	public void fix() {
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
	public boolean isFix() {
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
	
	/**
	 * 番号を指定してRimIndexElementを取得.
	 * @param indexPos RimIndexElementを取得する番号を設定します.
	 * @return RimIndexElement RimIndexElementが返却されます.
	 */
	public RimIndexElement getRimIndexElement(int indexPos) {
		checkFix();
		if(indexPos >= 0 && indexPos < fixIndex.length) {
			return fixIndex[indexPos];
		}
		return null;
	}
	
	/**
	 * 一致した検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> eq(boolean ascFlag, boolean notEq, Object value) {
		checkFix();
		value = columnType.convert(value);
		final int pos = SearchUtil.indexEq(fixIndex, (Comparable)value);
		// not条件での検索.
		if(notEq) {
			if(ascFlag) {
				// 0 から posを除く fixIndex.length - 1 まで.
				return new ResultSearchIndexNot(ascFlag, this, 0, pos ,pos);
			}
			// fixIndex.length - 1 からposを除く 0 まで.
			return new ResultSearchIndexNot(
				ascFlag, this, fixIndex.length - 1, pos ,pos);
		}
		// 通常検索.
		// posのみ.
		return new ResultSearchIndex(false, ascFlag, this, pos, NOT_END);
	}
	
	/**
	 * 大なり[>]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> gt(boolean ascFlag, boolean notEq, Object value) {
		checkFix();
		value = columnType.convert(value);
		final int pos = SearchUtil.indexGT(fixIndex, (Comparable)value);
		// not条件での検索.
		if(notEq) {
			if(ascFlag) {
				// 0 から pos まで.
				return new ResultSearchIndexNot(
					ascFlag, this, 0, pos, fixIndex.length - 1);
			}
			// pos から 0 まで.
			return new ResultSearchIndexNot(ascFlag, this, pos, pos, pos);
		}
		// 通常検索.
		if(ascFlag) {
			// pos から fixIndex.length - 1 まで.
			return new ResultSearchIndex(true, ascFlag, this, pos, NOT_END);
		}
		// fixIndex.length - 1 から pos まで.
		return new ResultSearchIndex(
			true, ascFlag, this, fixIndex.length - 1, new RowIdResultEnd(pos));
	}
	
	/**
	 * 大なり[>=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> ge(boolean ascFlag, boolean notEq, Object value) {
		checkFix();
		value = columnType.convert(value);
		final int pos = SearchUtil.indexGE(fixIndex, (Comparable)value);
		// not条件での検索.
		if(notEq) {
			if(ascFlag) {
				// 0 から pos まで.
				return new ResultSearchIndexNot(
					ascFlag, this, 0, pos, fixIndex.length - 1);
			}
			// pos から 0 まで.
			return new ResultSearchIndexNot(ascFlag, this, pos, pos, pos);
		}
		// 通常検索.
		if(ascFlag) {
			// pos から fixIndex.length - 1 まで.
			return new ResultSearchIndex(true, ascFlag, this, pos, NOT_END);
		}
		// fixIndex.length から pos まで.
		return new ResultSearchIndex(
			true, ascFlag, this, fixIndex.length - 1, new RowIdResultEnd(pos));
	}
	
	/**
	 * 小なり[<]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> lt(boolean ascFlag, boolean notEq, Object value) {
		checkFix();
		value = columnType.convert(value);
		final int pos = SearchUtil.indexLT(fixIndex, (Comparable)value);
		// not条件での検索.
		if(notEq) {
			if(ascFlag) {
				// pos から fixIndex.length - 1 まで.
				return new ResultSearchIndexNot(ascFlag, this, pos, pos, pos);
			}
			// fixIndex.length - 1 から pos まで.
			return new ResultSearchIndexNot(ascFlag, this, fixIndex.length - 1, pos, 0);
		}
		// 通常検索.
		if(ascFlag) {
			// 0 から pos まで.
			return new ResultSearchIndex(true, ascFlag, this, 0, new RowIdResultEnd(pos));
		} else {
			// pos から fixIndex.length - 1 まで.
			return new ResultSearchIndex(true, ascFlag, this, pos, NOT_END);
		}
	}
	
	/**
	 * 小なり[<=]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param value 条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> le(boolean ascFlag, boolean notEq, Object value) {
		checkFix();
		value = columnType.convert(value);
		final int pos = SearchUtil.indexLE(fixIndex, (Comparable)value);
		// not条件での検索.
		if(notEq) {
			if(ascFlag) {
				// pos から fixIndex.length - 1 まで.
				return new ResultSearchIndexNot(ascFlag, this, pos, pos, pos);
			}
			// fixIndex.length - 1 から pos まで.
			return new ResultSearchIndexNot(ascFlag, this, fixIndex.length - 1, pos, 0);
		}
		// 通常検索.
		if(ascFlag) {
			// 0 から pos まで.
			return new ResultSearchIndex(true, ascFlag, this, 0, new RowIdResultEnd(pos));
		} else {
			// pos から fixIndex.length - 1 まで.
			return new ResultSearchIndex(true, ascFlag, this, pos, NOT_END);
		}
	}
	
	/**
	 * 指定した start から end まで範囲検索.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param startObj 開始条件を設定します.
	 * @param endObj 終了条件を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> between(
		boolean ascFlag, boolean notEq, Object startObj, Object endObj) {
		checkFix();
		Comparable start = (Comparable)columnType.convert(startObj);
		Comparable end = (Comparable)columnType.convert(endObj);
		if(start == null || end == null) {
			throw new RimException("The between condition is not set correctly.");
		}
		int pos, endPos;
		// 昇順の場合.
		if(ascFlag) {
			// startの方が大きい場合入れ替える.
			if(start.compareTo(end) > 0) {
				Comparable t = start;
				start = end;
				end = t;
			}
			// 大なり[>=]検索.
			pos = SearchUtil.indexGE(fixIndex, start);
			// 小なり[<=]検索.
			endPos= SearchUtil.indexLE(fixIndex, end);
			if(!notEq) {
				// pos から posEndまで.
				return new ResultSearchIndex(
					true, ascFlag, this, pos, new RowIdResultEnd(endPos));
			} else {
				// 0 から pos -> endPos を飛ばして fixIndex.length - 1 まで.
				return new ResultSearchIndexNot(
					ascFlag, this, 0, pos, endPos);
			}
		// 降順の場合.
		} else {
			// startの方が小さい場合入れ替える.
			if(start.compareTo(end) < 0) {
				Comparable t = start;
				start = end;
				end = t;
			}
			// 小なり[<=]検索.
			pos = SearchUtil.indexLE(fixIndex, start);
			// 大なり[>=]検索.
			endPos = SearchUtil.indexGE(fixIndex, end);
			if(!notEq) {
				// pos から posEndまで.
				return new ResultSearchIndex(
					true, ascFlag, this, pos, new RowIdResultEnd(endPos));
			} else {
				// fixIndex.length - 1 から pos -> endPos 飛ばして 0 まで.
				return new ResultSearchIndexNot(
					ascFlag, this, fixIndex.length - 1, pos, endPos);
			}
		}
	}
	
	/**
	 * 複数一致条件[in]の検索条件を取得.
	 * @param ascFlag 昇順で情報取得する場合は true.
	 * @param notEq Not条件で検索する場合は true.
	 * @param values In条件群を設定します.
	 * @return SearchResult<Integer> 検索結果が返却されます.
	 */
	public ResultSearch<Integer> in(boolean ascFlag, boolean notEq, Object... values) {
		checkFix();
		return new ResultSearchIndexIn(ascFlag, notEq, this, values);
	}
	
	/**
	 * Rimインデックス要素.
	 */
	private static interface RimIndexElement extends Comparable {
		/**
		 * インデックス型を取得.
		 * @return
		 */
		public ColumnType getColumnType();
		
		/**
		 * Valueを取得.
		 * @return Comparable 要素のValueが返却されます.
		 */
		public Comparable getValue();

		/**
		 * Valueに対する行番号の管理数を取得.
		 * @return int 行番号が返却されます.
		 */
		public int getLineLength();

		/**
		 * 指定された位置の行番号を取得.
		 * @param no 取得項番を設定します.
		 * @return int 行番号が返却されます.
		 */
		public int getLineNo(int no);
	}

	/**
	 * 行数が２５５以下でのインデックス要素.
	 */
	private static final class RimIndexElement1 implements RimIndexElement {
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
	private static final class RimIndexElement2 implements RimIndexElement {
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
	private static final class RimIndexElement4 implements RimIndexElement {
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
	
	/**
	 * ResultSearchIndexの終端を判別する条件.
	 */
	private static interface ResultEnd {
		/**
		 * この条件が終端かチェック.
		 * @param ascFlag 昇順の場合は true.
		 * @param nowRowId 現在の行番号が設定されます.
		 * @param nowValue 現在の要素が設定されます.
		 * @return boolean trueの場合は終端です.
		 */
		public boolean isEnd(boolean ascFlag, int nowRowId, Comparable nowValue);
	}
	
	// 終端なしの設定を行う場合に利用.
	private static final NotResultEnd NOT_END = new NotResultEnd();
	
	/**
	 * 終端なし.
	 */
	private static final class NotResultEnd implements ResultEnd {
		@Override
		public boolean isEnd(boolean ascFlag, int nowRowId, Comparable nowValue) {
			return false;
		}
	}
	
	/**
	 * 行番号での終端定義.
	 */
	private static final class RowIdResultEnd implements ResultEnd {
		// 終端行番号.
		private int endRowId;

		/**
		 * コンストラクタ.
		 * @param endVRowId 読み込み終了行番号を設定します.
		 */
		RowIdResultEnd(int endRowId) {
			this.endRowId = endRowId;
		}
		
		@Override
		public boolean isEnd(boolean ascFlag, int nowRowId, Comparable nowValue) {
			return (ascFlag && nowRowId > endRowId) ||
				(!ascFlag && nowRowId < endRowId);	
		}
	}
	
	
	// 標準インデックス検索結果情報.
	private static final class ResultSearchIndex
		implements ResultSearch<Integer> {
		
		// 次の情報が取得可能な場合 true.
		private boolean nextFlag;
		// このインデックス管理情報.
		private RimIndex rimIndex;
		// 現在取得中のIndex位置.
		private int indexPos;
		
		// 現在取得中のIndex要素.
		private RimIndexElement element;
		// 取得中Indexの行取得ポジション.
		private int elementPos;
		
		// 昇順フラグ.
		private boolean ascFlag;
		
		// 検索終了条件.
		private ResultEnd resultEnd;
		
		// 今回取得情報.
		private Comparable nowValue;
		private int nowLineNo;
		
		/**
		 * コンストラクタ.
		 * @param nextFlag １要素で情報の取得を中断する場合は false.
		 * @param ascFlag 昇順で情報取得する場合は true.
		 * @param rimIndex RimIndexオブジェクトを設定します.
		 * @param indexPos インデックス項番の位置を設定します.
		 * @param resultEnd 読み込み終了条件を設定します.
		 */
		public ResultSearchIndex(boolean nextFlag, boolean ascFlag, RimIndex rimIndex,
			int indexPos, ResultEnd resultEnd) {
			// posが範囲外の場合はnullが返却される.
			int elementPos = -1;
			final RimIndexElement element = rimIndex.getRimIndexElement(indexPos);
			// nullでない場合は、要素の開始位置をセット.
			if(element != null) {
				elementPos = ascFlag ? 0 : element.getLineLength() - 1;
			}
			
			// データーセット.
			this.rimIndex = rimIndex;
			this.indexPos = indexPos;
			this.element = element;
			this.elementPos = elementPos;
			this.ascFlag = ascFlag;
			this.nextFlag = nextFlag;
			this.resultEnd = resultEnd;
		}
		
		// 次の情報を読み込む.
		private final boolean getNextElement() {
			// 読み込み情報が存在しない場合.
			if(element == null) {
				return false;
			// 現在取得中の要素にある行番号の読み込みが完了した場合.
			// 次の情報を読み込む.
			} else if(
				(ascFlag && element.getLineLength() <= elementPos) ||
				(!ascFlag && elementPos < 0)) {
				
				// 次の読み込みを行わない設定の場合.
				if(!nextFlag) {
					// 処理終了.
					element = null;
					return false;
				}
				
				// 昇順の場合.
				if(ascFlag) {
					
					// 次のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(++ indexPos)) == null) {
						// 情報が存在しない場合処理終了.
						nextFlag = false;
						element = null;
						return false;
					}
					
					// 終了条件にマッチする場合.
					if(resultEnd.isEnd(ascFlag, indexPos, element.getValue())) {
						// 処理終了.
						nextFlag = false;
						element = null;
						return false;
					}
					// ポジション初期化.
					elementPos = 0;
					
				// 降順の場合.
				} else {
					
					// 前のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(-- indexPos)) == null) {
						// 情報が存在しない場合処理終了.
						nextFlag = false;
						element = null;
						return false;
					}
					
					// 終了条件にマッチする場合.
					if(resultEnd.isEnd(ascFlag, indexPos, element.getValue())) {
						// 処理終了.
						nextFlag = false;
						element = null;
						return false;
					}
					// ポジション初期化.
					elementPos = element.getLineLength() - 1;
				}
			}
			return true;
		}

		@Override
		public boolean hasNext() {
			return getNextElement();
		}

		@Override
		public Integer next() {
			if(!getNextElement()) {
				throw new NoSuchElementException();
			}
			// 今回分のデータを取得.
			nowValue = element.getValue();
			nowLineNo = element.getLineNo(elementPos);
			elementPos = ascFlag ?
				elementPos + 1 :
				elementPos - 1;
			return nowLineNo;
		}
		
		@Override
		public Comparable getValue() {
			if(element == null) {
				throw new NoSuchElementException();
			}
			return nowValue;
		}
		
		@Override
		public int getLineNo() {
			if(element == null) {
				throw new NoSuchElementException();
			}
			return nowLineNo;
		}
	}
	
	// [NOT]インデックス検索結果.
	private static final class ResultSearchIndexNot
		implements ResultSearch<Integer> {
		// このインデックス管理情報.
		private RimIndex rimIndex;
		// 現在取得中のIndex位置.
		private int indexPos;
		
		// 現在取得中のIndex要素.
		private RimIndexElement element;
		// 取得中Indexの行取得ポジション.
		private int elementPos;
		
		// 昇順フラグ.
		private boolean ascFlag;
		
		// 今回取得情報.
		private Comparable nowValue;
		private int nowLineNo;

		// 除外開始ポジション.
		private int exclusionStart;
		// 除外終了ポジション.
		private int exclusionEnd;
		
		/**
		 * コンストラクタ.
		 * @param ascFlag 昇順で情報取得する場合は true.
		 * @param rimIndex RimIndexオブジェクトを設定します.
		 * @param indexPos インデックス項番の開始位置を設定します.
		 * @param exclusionStart 除外開始条件を設定します.
		 * @param exclusionEnd 除外終了位置を設定します.
		 * @param resultEnd 読み込み終了条件を設定します.
		 */
		public ResultSearchIndexNot(boolean ascFlag, RimIndex rimIndex, int indexPos,
			int exclusionStart, int exclusionEnd) {
			// 指定されたインデックス位置が除外番号範囲の場合.
			if(ascFlag) {
				// 昇順.
				if(indexPos >= exclusionStart &&
					indexPos <= exclusionEnd) {
					indexPos = exclusionEnd + 1;
				}
			} else {
				// 降順.
				if(indexPos <= exclusionStart &&
					indexPos >= exclusionEnd) {
					indexPos = exclusionEnd - 1;
				}
			}
			
			// posが範囲外の場合はnullが返却される.
			int elementPos = -1;
			final RimIndexElement element = rimIndex.getRimIndexElement(indexPos);
			// nullでない場合は、要素の開始位置をセット.
			if(element != null) {
				elementPos = ascFlag ? 0 : element.getLineLength() - 1;
			}
			
			// データーセット.
			this.rimIndex = rimIndex;
			this.indexPos = indexPos;
			this.element = element;
			this.elementPos = elementPos;
			this.ascFlag = ascFlag;
			this.exclusionStart = exclusionStart;
			this.exclusionEnd = exclusionEnd;
		}
		
		// 次の情報を読み込む.
		private final boolean getNextElement() {
			// 読み込み情報が存在しない場合.
			if(element == null) {
				return false;
			// 現在取得中の要素にある行番号の読み込みが完了した場合.
			// 次の情報を読み込む.
			} else if(
				(ascFlag && element.getLineLength() <= elementPos) ||
				(!ascFlag && elementPos < 0)) {
				
				// 昇順の場合.
				if(ascFlag) {
					
					// 次に取得する情報が除外番号範囲の場合.
					if(indexPos + 1 >= exclusionStart && indexPos + 1 <= exclusionEnd) {
						// 終了位置まで移動.
						indexPos = exclusionEnd;
					}
					
					// 次のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(++ indexPos)) == null) {
						// 情報が存在しない場合処理終了.
						element = null;
						return false;
					}
					
					// ポジション初期化.
					elementPos = 0;
					
				// 降順の場合.
				} else {
					
					// 次に取得する情報が除外番号範囲の場合.
					if(indexPos - 1 <= exclusionStart && indexPos - 1 >= exclusionEnd) {
						// 終了位置まで移動.
						indexPos = exclusionEnd;
					}
					
					// 前のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(-- indexPos)) == null) {
						// 情報が存在しない場合処理終了.
						element = null;
						return false;
					}
					
					// ポジション初期化.
					elementPos = element.getLineLength() - 1;
				}
			}
			return true;
		}

		@Override
		public boolean hasNext() {
			return getNextElement();
		}

		@Override
		public Integer next() {
			if(!getNextElement()) {
				throw new NoSuchElementException();
			}
			// 今回分のデータを取得.
			nowValue = element.getValue();
			nowLineNo = element.getLineNo(elementPos);
			elementPos = ascFlag ?
				elementPos + 1 :
				elementPos - 1;
			return nowLineNo;
		}
		
		@Override
		public Comparable getValue() {
			if(element == null) {
				throw new NoSuchElementException();
			}
			return nowValue;
		}
		
		@Override
		public int getLineNo() {
			if(element == null) {
				throw new NoSuchElementException();
			}
			return nowLineNo;
		}
	}
	
	// [IN]インデックス検索結果情報.
	private static final class ResultSearchIndexIn
		implements ResultSearch<Integer> {
		// in 条件.
		private ResultSearch<Integer>[] inList;
		// 現在のIn条件取得項番.
		private int targetIn;
		
		
		// このインデックス管理情報.
		private RimIndex rimIndex;
		// not in 条件.
		private RowsFlag notInPositions;
		// 現在の読み込み中行番号.
		private int indexPos;
		// 現在取得中のIndex要素.
		private RimIndexElement element;
		// 取得中Indexの行取得ポジション.
		private int elementPos;
		// 今回取得情報.
		private Comparable nowValue;
		private int nowLineNo;

		
		// 昇順フラグ.
		private boolean ascFlag;
		
		// not条件のフラグ.
		private boolean notFlag;
		
		// 終了フラグ.
		private boolean exitFlag;
		
		/**
		 * コンストラクタ.
		 * @param ascFlag 昇順で情報取得する場合は true.
		 * @param notEq Not条件で検索する場合は true.
		 * @param rimIndex RimIndexオブジェクトを設定します.
		 * @param values In条件群を設定します.
		 */
		public ResultSearchIndexIn(boolean ascFlag, boolean notEq, RimIndex rimIndex,
			Object... values) {
			
			int len = values.length;
			// in条件が指定されていない場合.
			if(len == 0) {
				throw new RimException("At least one condition must be set.");
			}
			int i, pos, targetIn;
			ColumnType columnType = rimIndex.columnType;
			
			// notEq = true の場合は、インデックスが使えず、
			// in で指定した行番号群以外を参照するような
			// 検索方法となる.
			if(notEq) {
				// indexを取得.
				final RimIndexElement[] fixIndex = rimIndex.fixIndex;
				
				// not検索の場合はin条件は行番号で処理する.
				final RowsFlag notInPositions = new RowsFlag(fixIndex.length);
				
				// valueを列型変換して行番号を取得.
				for(i = 0; i < len; i ++) {
					pos = SearchUtil.indexEq(
						fixIndex, (Comparable)columnType.convert(values[i]));
					if(pos != -1) {
						notInPositions.put(pos, true);
					}
				}
				
				// 生成できた場合は情報セット.
				this.rimIndex = rimIndex;
				this.notInPositions = notInPositions;
				this.indexPos = ascFlag ? 0 : fixIndex.length - 1;
				this.element = fixIndex[indexPos];
				this.elementPos = 0;
			
			// 通常検索.
			} else {
				// 通常の場合はin条件はResultSearchで処理する.
				ResultSearch<Integer>[] inList = new ResultSearch[len];
				
				// valueを列型変換してソート処理.
				for(i = 0; i < len; i ++) {
					values[i] = columnType.convert(values[i]);
					// valuesのどれかがnullの場合は例外.
					if(values[i] == null) {
						throw new RimException("Null cannot be set in the search condition.");
					}
				}
				Arrays.sort(values);
				
				// in条件毎にResultSearchIndexを作成する.
				if(ascFlag) {
					// 昇順.
					for(i = 0; i < len; i ++) {
						pos = SearchUtil.indexEq(rimIndex.fixIndex, (Comparable)values[i]);
						inList[i] = new ResultSearchIndex(false, ascFlag, rimIndex, pos, NOT_END);
					}
					targetIn = 0;
				} else {
					// 降順.
					for(i = len - 1; i >= 0; i --) {
						pos = SearchUtil.indexEq(rimIndex.fixIndex, (Comparable)values[i]);
						inList[i] = new ResultSearchIndex(false, ascFlag, rimIndex, pos, NOT_END);
					}
					targetIn = len - 1;
				}
				// 生成できた場合は情報セット.
				this.inList = inList;
				this.targetIn = targetIn;
			}
			
			// データーセット.
			this.ascFlag = ascFlag;
			this.notFlag = notEq;
			this.exitFlag = false;
		}
		
		// 次の情報を読み込む.
		private final boolean getNextElement() {
			// 終端の場合.
			if(exitFlag) {
				return false;
			}
			// 通常検索の場合.
			if(!notFlag) {
				// 現在の読み込み対象のResultSearchIndexが読み終わってる場合.
				if(inList[targetIn].hasNext()) {
					return true;
				}
				// 以降の条件が存在するかチェック.
				if(ascFlag) {
					// 昇順.
					final int len = inList.length;
					while(len > targetIn + 1) {
						if(inList[++ targetIn].hasNext()) {
							return true;
						}
					}
				} else {
					// 降順.
					while(targetIn > 0) {
						if(inList[-- targetIn].hasNext()) {
							return true;
						}
					}
				}
				// 存在しない場合は終端をセット.
				exitFlag = true;
				return false;
			}
			
			// not検索の場合.
			// 現在取得中の要素にある行番号の読み込みが完了した場合.
			// 次の情報を読み込む.
			if(
				(ascFlag && element.getLineLength() <= elementPos) ||
				(!ascFlag && elementPos < 0)) {
				
				// 昇順の場合.
				if(ascFlag) {
					
					// 次のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(++ indexPos)) != null) {
						// in 条件で指定されたvaluesの条件の場合.
						if(notInPositions.get(indexPos)) {
							// この情報を読み飛ばす.
							if((element = rimIndex.getRimIndexElement(++ indexPos)) == null) {
								// 次の情報が存在しない場合は終端をセット.
								exitFlag = true;
								return false;
							}
						}
						// ポジション初期化.
						elementPos = 0;
						return true;
					}
				// 降順の場合.
				} else {
					
					// 前のIndex要素を取得.
					if((element = rimIndex.getRimIndexElement(-- indexPos)) != null) {
						// in 条件で指定されたvaluesの条件の場合.
						if(notInPositions.get(indexPos)) {
							// この情報を読み飛ばす.
							if((element = rimIndex.getRimIndexElement(-- indexPos)) == null) {
								// 情報が存在しない場合は終端をセット.
								exitFlag = true;
								return false;
							}
						}
						// ポジション初期化.
						elementPos = element.getLineLength() - 1;
						return true;
					}
				}
			}
			return true;
		}
		
		@Override
		public boolean hasNext() {
			return getNextElement();
		}

		@Override
		public Integer next() {
			// 終端の場合.
			if(!getNextElement()) {
				throw new NoSuchElementException();
			// 通常取得の場合.
			} else if(!notFlag) {
				return inList[targetIn].next();
			}
			// not 取得の場合.
			nowValue = element.getValue();
			nowLineNo = element.getLineNo(elementPos);
			elementPos = ascFlag ?
				elementPos + 1 :
				elementPos - 1;
			return nowLineNo;
		}

		@Override
		public Comparable getValue() {
			if(exitFlag) {
				throw new NoSuchElementException();
			}
			return !notFlag ? inList[targetIn].getValue() : nowValue;
		}

		@Override
		public int getLineNo() {
			if(exitFlag) {
				throw new NoSuchElementException();
			}
			return !notFlag ? inList[targetIn].getLineNo() : nowLineNo;
		}
	}

}
