package rim.index;

import java.util.Arrays;
import java.util.NoSuchElementException;

import rim.RimBody;
import rim.RimResult;
import rim.RimRow;
import rim.core.ColumnType;
import rim.core.RowsFlag;
import rim.core.SearchUtil;
import rim.exception.RimException;
import rim.util.ObjectList;

/**
 * Rimインデックス情報.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RimIndex {
	// RimBody.
	private RimBody body;
	// 列番号.
	private int columnNo;
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
	 * @param body RimBodyを設定します.
	 * @param columnNo このインデックスの列番号が設定されます.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 */
	public RimIndex(RimBody body, int columnNo, int planIndexSize) {
		this.body = body;
		this.columnNo = columnNo;
		this.columnType = body.getColumnType(columnNo);
		this.indexByte = RimIndexUtil.getRowByteLength(
			body.getRowLength());
		this.planIndexSize = planIndexSize;
	}

	/**
	 * １つのインデックス要素を追加.
	 * @param value 要素を設定します.
	 * @param rowIds 行Id群を設定します.
	 * @param len 有効な長さを設定します.
	 * @return int 現在までの追加件数を取得.
	 */
	public int add(Comparable value, int[] rowIds, int len) {
		checkFixToError();
		final RimIndexElement em = RimIndexUtil.createRimIndexElement(
			indexByte, columnType, value, rowIds, len);
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
	 * 行追加が完了していない場合エラー出力.
	 */
	protected void checkNoFixToError() {
		if(!isFix()) {
			throw new RimException("Index addition is not complete.");
		}
	}
	
	/**
	 * 行追加が完了してる場合エラー出力.
	 */
	protected void checkFixToError() {
		if(isFix()) {
			throw new RimException("Index addition is complete.");
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
		return body.getColumnName(columnNo);
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
	 * 指定Valueに一致するRimIndexElementを取得.
	 * @param value RimIndexElementを取得したいValueを設定します.
	 * @return RimIndexElement 存在する場合はRimIndexElementが返却されます.
	 */
	public RimIndexElement getElement(Object value) {
		checkNoFixToError();
		return getElementByNo(
			SearchUtil.indexEq(fixIndex,
				(Comparable)columnType.convert(value)));
	}
	
	/**
	 * 番号を指定してRimIndexElementを取得.
	 * @param indexPos RimIndexElementを取得する番号を設定します.
	 * @return RimIndexElement RimIndexElementが返却されます.
	 */
	public final RimIndexElement getElementByNo(int indexPos) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult eq(boolean ascFlag, boolean notEq, Object value) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult gt(boolean ascFlag, boolean notEq, Object value) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult ge(boolean ascFlag, boolean notEq, Object value) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult lt(boolean ascFlag, boolean notEq, Object value) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult le(boolean ascFlag, boolean notEq, Object value) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult between(
		boolean ascFlag, boolean notEq, Object startObj, Object endObj) {
		checkNoFixToError();
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
	 * @return SearchResult 検索結果が返却されます.
	 */
	public RimResult in(boolean ascFlag, boolean notEq, Object... values) {
		checkNoFixToError();
		return new ResultSearchIndexIn(ascFlag, notEq, this, values);
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
		implements RimResult {
		
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
		public ResultSearchIndex(boolean nextFlag, boolean ascFlag,
			RimIndex rimIndex, int indexPos, ResultEnd resultEnd) {
			// posが範囲外の場合はnullが返却される.
			int elementPos = -1;
			final RimIndexElement element = rimIndex.getElementByNo(indexPos);
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
					if((element = rimIndex.getElementByNo(++ indexPos)) == null) {
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
					if((element = rimIndex.getElementByNo(-- indexPos)) == null) {
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
		public RimRow nextRow() {
			next();
			return rimIndex.body.getRow(nowLineNo);
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
		implements RimResult {
		
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
		public ResultSearchIndexNot(boolean ascFlag, RimIndex rimIndex,
			int indexPos, int exclusionStart, int exclusionEnd) {
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
			final RimIndexElement element = rimIndex.getElementByNo(indexPos);
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
					if((element = rimIndex.getElementByNo(++ indexPos)) == null) {
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
					if((element = rimIndex.getElementByNo(-- indexPos)) == null) {
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
		public RimRow nextRow() {
			next();
			return rimIndex.body.getRow(nowLineNo);
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
		implements RimResult {
		// in 条件.
		private RimResult[] inList;
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
		public ResultSearchIndexIn(boolean ascFlag, boolean notEq,
			RimIndex rimIndex, Object... values) {
			
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
				RimResult[] inList = new RimResult[len];
				
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
						inList[i] = new ResultSearchIndex(
							false, ascFlag, rimIndex, pos, NOT_END);
					}
					targetIn = 0;
				} else {
					// 降順.
					for(i = len - 1; i >= 0; i --) {
						pos = SearchUtil.indexEq(rimIndex.fixIndex, (Comparable)values[i]);
						inList[i] = new ResultSearchIndex(
							false, ascFlag, rimIndex, pos, NOT_END);
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
					if((element = rimIndex.getElementByNo(++ indexPos)) != null) {
						// in 条件で指定されたvaluesの条件の場合.
						if(notInPositions.get(indexPos)) {
							// この情報を読み飛ばす.
							if((element = rimIndex.getElementByNo(++ indexPos)) == null) {
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
					if((element = rimIndex.getElementByNo(-- indexPos)) != null) {
						// in 条件で指定されたvaluesの条件の場合.
						if(notInPositions.get(indexPos)) {
							// この情報を読み飛ばす.
							if((element = rimIndex.getElementByNo(-- indexPos)) == null) {
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
		public RimRow nextRow() {
			next();
			return rimIndex.body.getRow(nowLineNo);
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
