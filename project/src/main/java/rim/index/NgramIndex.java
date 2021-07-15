package rim.index;

import java.util.NoSuchElementException;

import rim.RimBody;
import rim.RimResultNgram;
import rim.RimRow;
import rim.core.ColumnType;
import rim.core.SearchUtil;
import rim.exception.RimException;
import rim.util.IndexKeyList;
import rim.util.ObjectList;

/**
 * NGramインデックス.
 */
public class NgramIndex {
	
	// インデックス型(String).
	private static final ColumnType VALUE_TYPE = ColumnType.String;
	
	// RimBody.
	private RimBody body;
	// Bodyに対する列番号.
	private int columnNo;
	
	// Ngram長.
	private int ngramLength;
	
	// Ngramインデックス情報.
	private ObjectList<Ngram> index = new ObjectList<Ngram>();
	// 登録予定のインデックス総行数.
	private int planIndexSize;
	
	// fixしたインデックス情報.
	private Ngram[] fixIndex;
	// インデックス総数.
	private int indexSize;
	
	/**
	 * コンストラクタ.
	 * @param body RimBodyを設定します.
	 * @param columnNo このインデックスの列番号が設定されます.
	 * @param ngramLength Ngram長を設定します.
	 * @param planIndexSize このインデックスの予定登録行数を設定します.
	 */
	public NgramIndex(RimBody body, int columnNo, int ngramLength,
		int planIndexSize) {
		this.body = body;
		this.columnNo = columnNo;
		this.planIndexSize = planIndexSize;
		this.ngramLength = ngramLength;
	}
	
	/**
	 * １つのインデックス要素を追加.
	 * @param value 要素を設定します.
	 * @param rowIds 行Id群を設定します.
	 * @param positions 対象要素の文字開始位置を設定します.
	 * @param len 有効な長さを設定します.
	 * @return int 現在までの追加件数を取得.
	 */
	public int add(long value, int[] rowIds, int[] positions, int len) {
		int i;
		// １つのNgram要素に対する詳細情報を生成.
		final DetailNgram[] details = new DetailNgram[len];
		for(i = 0; i < len; i ++) {
			details[i] = createDetailNgram(rowIds[i], positions[i]);
		}
		// Ngram情報を生成.
		Ngram ngram = null;
		switch(ngramLength) {
		case 1:
			ngram = new Unigram(value, details);
			break;
		case 2:
			ngram = new Bigram(value, details);
			break;
		case 3:
			ngram = new Trigram(value, details);
			break;
		}
		index.add(ngram);
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
				"It does not match the expected number of ngram index rows(" +
				planIndexSize + "/" + indexSize + ")");
		}
		fixIndex = index.toArray(Ngram.class);
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
			throw new RimException("Ngram index addition is not complete.");
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
		return VALUE_TYPE;
	}
	
	/**
	 * Ngram長を取得.
	 * @return int Ngram長が返却されます.
	 */
	public int getNgramLength() {
		return ngramIndex;
	}

	/**
	 * 設定されているインデックス行数を取得.
	 * @return int インデックス行数が返却されます.
	 */
	public int getLength() {
		return indexSize;
	}
	
	/**
	 * 指定文字を含む情報を検索.
	 * @param ascFlag 昇順で取得する場合は true.
	 * @param lineExclusion 同一行番号を取得しない場合は true.
	 * @param value 検索ワードを設定します.
	 * @return RimResultNgram 検索結果が返却されます.
	 */
	public RimResultNgram search(boolean ascFlag, boolean lineExclusion,
		String value) {
		return search(ascFlag, lineExclusion, value, 0, value.length());
	}
	
	/**
	 * 指定文字を含む情報を検索.
	 * @param ascFlag 昇順で取得する場合は true.
	 * @param lineExclusion 同一行番号を取得しない場合は true.
	 * @param value 検索ワードを設定します.
	 * @param off 検索ワード文字のオフセット値を設定します.
	 * @param len 検索ワードの長さを設定します.
	 * @return RimResultNgram 検索結果が返却されます.
	 */
	public RimResultNgram search(boolean ascFlag, boolean lineExclusion,
		String value, int off, int len) {
		return new NgramResult(this, ascFlag, lineExclusion, value, off, len);
	}
	
	/**
	 * NgramResult.
	 */
	@SuppressWarnings("rawtypes")
	private static final class NgramResult implements RimResultNgram {
		// RimBody.
		private RimBody body;
		// Bodyに対する列番号.
		private int columnNo;
		
		// インデックス情報.
		private Ngram[] index;
		// 昇順フラグ.
		private boolean ascFlag;
		
		// ワードリスト.
		private long[] wordList;
		// ワードリスト長.
		private int wordLength;
		// 検索TopのNgram.
		private Ngram topNgram;
		// TopのNgramに対する詳細位置数.
		private int detailLength;
		// 取得中Indexの詳細位置.
		private int detailPos;
		// Ngram詳細検索用.
		private DetailNgram4_4 detailNgram;
		
		// nextGetで取得済み条件.
		private boolean nextGetFlag;
		// 今回取得情報.
		private int[] result;
		
		// 取得同一行情報管理.
		private IndexKeyList acquiredRowIdList;
		
		/**
		 * コンストラクタ.
		 * @param index RimNgramIndexオブジェクトを設定します.
		 * @param ascFlag 昇順の場合は true.
		 * @param lineExclusion 同一行番号を取得しない場合は true.
		 * @param value 検索ワードを設定します.
		 * @param off 検索ワードのオフセット値を設定します.
		 * @param len 検索ワードの長さを設定します.
		 */
		public NgramResult(NgramIndex index, boolean ascFlag, boolean lineExclusion,
			String value, int off, int len) {
			
			// 検索対象のNgramワード群を取得.
			final int ngramLen = index.ngramLength;
			final int wordLength = len - (ngramLen - 1);
			final long[] wordList = new long[wordLength];
			for(int i = 0, j = off; i < wordLength; i ++) {
				wordList[i] = SearchUtil.getNgramString(value, j ++, ngramLen);
			}
			int detailLength = 0;
			int detailPos = -2;
			Ngram topNgram = null;
			// 検索TopのNgramを取得.
			if(wordLength > 0) {
				// TopNgramを取得.
				topNgram = getNgram(index.fixIndex, wordList[0]);
				if(topNgram != null) {
					// Detail長を取得.
					detailLength = topNgram.getDetailLength();
					// 開始位置を設定.
					detailPos = (ascFlag) ? -1 : detailLength;
				}
			}
			
			this.ascFlag = ascFlag;
			this.body = index.body;
			this.columnNo = index.columnNo;
			this.index = index.fixIndex;
			
			this.wordLength = wordLength;
			this.wordList = wordList;
			this.topNgram = topNgram;
			this.detailLength = detailLength;
			this.detailPos = detailPos;
			this.detailNgram = new DetailNgram4_4();
			this.result = new int[] {-1, -1};
			
			this.acquiredRowIdList = lineExclusion ?
				new IndexKeyList() : null;
		}
		
		// ワードを設定して１つのNgramを取得.
		private static final Ngram getNgram(final Ngram[] index, final long value) {
			final int p = SearchUtil.indexEq(index, value);
			if(p == -1) {
				return null;
			}
			return index[p];
		}
		
		// TopNgramの詳細番号を設定して、その条件が指定ワード一致する
		// 内容かチェックし、一致する場合は行番号を返却.
		private final boolean oneSearch(final int[] out, final int no) {
			Ngram ngram;
			int p, rowId, wordPos;
			
			// 指定された詳細番号に対する行番号とワード位置を取得.
			DetailNgram topDetail = topNgram.getDetail(no);
			if(topDetail == null) {
				return false;
			}
			rowId = topDetail.getRowId();
			wordPos = topDetail.getPosition();
			topDetail = null;
			
			// ２番目以降のワードで行番号とワード位置に一致する
			// 条件をチェックして、全ワードが一致する場合は
			// 行番号を返却する.
			for(int i = 1; i < wordLength; i ++) {
				// 指定ワードのNgramが存在するかチェック.
				if((ngram = getNgram(index, wordList[i])) == null) {
					// 見つからない場合は検索不一致.
					return false;
				}
				// 存在する場合、詳細条件を設定して検索.
				detailNgram.set(rowId, wordPos + i);
				// 見つからない場合は検索不一致.
				p = SearchUtil.indexEq(ngram.getDetails(), detailNgram);
				if(p == -1) {
					return false;
				}
			}
			// 見つかった場合.
			out[0] = rowId;		// 行情報.
			out[1] = wordPos;	// 文字開始位置.
			return true;
		}
		
		// 次の情報を取得.
		private final boolean nextGet() {
			if(topNgram == null) {
				return false;
			} else if(nextGetFlag) {
				return true;
			}
			// 昇順の場合.
			if(ascFlag) {
				while(true) {
					if(oneSearch(result, ++ detailPos)) {
						// 同一行情報の場合.
						if(acquiredRowIdList != null &&
							acquiredRowIdList.add(result[0])) {
							continue;
						}
						nextGetFlag = true;
						return true;
					} else if(detailPos >= detailLength) {
						acquiredRowIdList.clear();
						topNgram = null;
						return false;
					}
				}
			} else {
				while(true) {
					if(oneSearch(result, -- detailPos)) {
						// 同一行情報の場合.
						if(acquiredRowIdList != null &&
							acquiredRowIdList.add(result[0])) {
							continue;
						}
						nextGetFlag = true;
						return true;
					} else if(detailPos < 0) {
						acquiredRowIdList.clear();
						topNgram = null;
						return false;
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return nextGet();
		}

		@Override
		public Integer next() {
			if(!nextGet()) {
				throw new NoSuchElementException();
			}
			nextGetFlag = false;
			return result[0];
		}

		@Override
		public RimRow nextRow() {
			next();
			return body.getRow(result[0]);
		}

		@Override
		public Comparable getValue() {
			if(topNgram == null) {
				throw new NoSuchElementException();
			} else if(result[0] == -1) {
				return null;
			}
			RimRow row = body.getRow(result[0]);
			return row.getString(columnNo);
		}

		@Override
		public int getLineNo() {
			if(topNgram == null) {
				throw new NoSuchElementException();
			}
			return result[0];
		}

		@Override
		public int getPosition() {
			if(topNgram == null) {
				throw new NoSuchElementException();
			}
			return result[1];
		}
		
		@Override
		public boolean isAcquiredLine() {
			return acquiredRowIdList == null;
		}
	}
	
	
	// Ngram要素.
	private static interface Ngram extends Comparable<Object> {
		/**
		 * Ngramを表す文字の１区切りの長さを取得.
		 * @return int Ngramの長さが返却されます.
		 *             1の場合はユニグラム.
		 *             2の場合はバイグラム.
		 *             3の場合はトライグラム.
		 */
		public int getNgramLength();
		
		/**
		 * Ngramの文字情報が返却されます.
		 * @return long Ngramの文字情報が返却されます.
		 *              内容としては getNgramLength() == 1 ならユニグラム.
		 *              0x0000,0000,0000,ffff(1) が文字範囲.
		 *              内容としては getNgramLength() == 2 ならバイグラム.
		 *              0x0000,0000,ffff(2),ffff(1) が文字範囲.
		 *              内容としては getNgramLength() == 3 ならトライグラム.
		 *              0x0000,ffff(3),ffff(2),ffff(1) が文字範囲.
		 */
		public long getValue();
		
		/**
		 * このNgramの文字情報と一致する詳細条件を指定して取得.
		 * @param no 項番を設定します.
		 * @return DetailNgram 詳細条件が返却されます.
		 */
		default DetailNgram getDetail(int no) {
			final DetailNgram[] details = getDetails();
			if(no < 0 || no >= details.length) {
				return null;
			}
			return details[no];
		}
		
		/**
		 * このNgramの文字情報と一致する詳細条件件数を取得.
		 * @return int 詳細条件件数が返却されます.
		 */
		default int getDetailLength() {
			return getDetails().length;
		}
		
		/**
		 * このNgramの文字情報と一致する詳細条件群を取得.
		 * @return DetailNgram[] 詳細条件群が返却されます.
		 */
		public DetailNgram[] getDetails();
		
		@Override
		default int compareTo(Object value) {
			long r;
			if(value instanceof Ngram) {
				r = getValue() - ((Ngram)value).getValue();
			} else if(value instanceof Long) {
				r = getValue() - (Long)value;
			} else if(value instanceof String) {
				r = SearchUtil.getNgramString(
					(String)value, 0, getNgramLength());
			} else {
				throw new RimException(
					"An object type that is not available is set.");
			}
			if(r < 0L) {
				return -1;
			} else if(r > 0L) {
				return 1;
			}
			return 0;
		}
	}
	
	// ユニグラム.
	private static final class Unigram implements Ngram {
		private char value;
		private DetailNgram[] details;
		
		/**
		 * コンストラクタ.
		 * @param value 文字情報を設定します.
		 * @param details Ngram詳細を設定します.
		 */
		public Unigram(long value, DetailNgram[] details) {
			this.value = (char)(value & 0x000000000000ffffL);
			this.details = details;
		}

		@Override
		public int getNgramLength() {
			return 1;
		}

		@Override
		public long getValue() {
			return (long)(value & 0x000000000000ffffL);
		}

		@Override
		public DetailNgram[] getDetails() {
			return details;
		}
	}
	
	// バイグラム.
	private static final class Bigram implements Ngram {
		private int value;
		private DetailNgram[] details;
		
		/**
		 * コンストラクタ.
		 * @param value 文字情報を設定します.
		 * @param details Ngram詳細を設定します.
		 */
		public Bigram(long value, DetailNgram[] details) {
			this.value = (int)(value & 0x00000000ffffffffL);
			this.details = details;
		}

		@Override
		public int getNgramLength() {
			return 2;
		}

		@Override
		public long getValue() {
			return (long)(value & 0x00000000ffffffffL);
		}

		@Override
		public DetailNgram[] getDetails() {
			return details;
		}
	}
	
	// トライグラム.
	private static final class Trigram implements Ngram {
		private long value;
		private DetailNgram[] details;
		
		/**
		 * コンストラクタ.
		 * @param value 文字情報を設定します.
		 * @param details Ngram詳細を設定します.
		 */
		public Trigram(long value, DetailNgram[] details) {
			this.value = (value & 0x0000ffffffffffffL);
			this.details = details;
		}

		@Override
		public int getNgramLength() {
			return 3;
		}

		@Override
		public long getValue() {
			return (long)(value & 0x0000ffffffffffffL);
		}

		@Override
		public DetailNgram[] getDetails() {
			return details;
		}
	}
	
	/**
	 * DetailNgramを生成.
	 * @param rowId 行番号を設定します.
	 * @param position 文字ポジションを設定します.
	 * @return DetailNgram DetailNgramが返却されます.
	 */
	private static final DetailNgram createDetailNgram(
		final int rowId, final int position) {
		final int posByte = RimIndexUtil.getRowByteLength(position);
		switch(RimIndexUtil.getRowByteLength(rowId)) {
		case 1:
			switch(posByte) {
			case 1: return new DetailNgram1_1().set(rowId, position);
			case 2: return new DetailNgram1_2().set(rowId, position);
			case 4: return new DetailNgram1_4().set(rowId, position);
			}
		case 2:
			switch(posByte) {
			case 1: return new DetailNgram2_1().set(rowId, position);
			case 2: return new DetailNgram2_2().set(rowId, position);
			case 4: return new DetailNgram2_4().set(rowId, position);
			}
		case 4:
			switch(posByte) {
			case 1: return new DetailNgram4_1().set(rowId, position);
			case 2: return new DetailNgram4_2().set(rowId, position);
			case 4: return new DetailNgram4_4().set(rowId, position);
			}
		}
		return new DetailNgram4_4().set(rowId, position);
	}
	
	// 1つのNgramの行番号と文字開始位置管理.
	private static interface DetailNgram extends Comparable<DetailNgram> {
		/**
		 * 行番号を取得.
		 * @return int 行番号が返却されます.
		 */
		public int getRowId();
		
		/**
		 * 文字開始位置を取得.
		 * @return int 文字開始位置が返却されます.
		 */
		public int getPosition();
		
		@Override
		default int compareTo(DetailNgram oneNgram) {
			// 行番号で大なり・小なり判別.
			final int r = getRowId() - oneNgram.getRowId();
			if(r != 0) {
				return r;
			}
			// 文字開始位置で大なり・小なり判別.
			final int p = getPosition() - oneNgram.getPosition();
			if(p != 0) {
				return p;
			}
			return 0;
		}
	}
	
	// 行番号byte, ポジションbyteのNgram詳細.
	private static final class DetailNgram1_1 implements DetailNgram {
		private byte rowId;
		private byte position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (byte)(rowId & 0x000000ff);
			this.position = (byte)(position & 0x000000ff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x000000ff;
		}
		@Override
		public final int getPosition() {
			return position & 0x000000ff;
		}
	}
	
	// 行番号byte, ポジションshortのNgram詳細.
	private static final class DetailNgram1_2 implements DetailNgram {
		private byte rowId;
		private short position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (byte)(rowId & 0x000000ff);
			this.position = (short)(position & 0x0000ffff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x000000ff;
		}
		@Override
		public final int getPosition() {
			return position & 0x0000ffff;
		}
	}
	
	// 行番号byte, ポジションintのNgram詳細.
	private static final class DetailNgram1_4 implements DetailNgram {
		private byte rowId;
		private int position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (byte)(rowId & 0x000000ff);
			this.position = position;
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x000000ff;
		}
		@Override
		public final int getPosition() {
			return position;
		}
	}
	
	// 行番号short, ポジションbyteのNgram詳細.
	private static final class DetailNgram2_1 implements DetailNgram {
		private short rowId;
		private byte position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (short)(rowId & 0x0000ffff);
			this.position = (byte)(position & 0x000000ff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x0000ffff;
		}
		@Override
		public final int getPosition() {
			return position & 0x000000ff;
		}
	}
	
	// 行番号short, ポジションshortのNgram詳細.
	private static final class DetailNgram2_2 implements DetailNgram {
		private short rowId;
		private short position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (short)(rowId & 0x0000ffff);
			this.position = (short)(position & 0x0000ffff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x0000ffff;
		}
		@Override
		public final int getPosition() {
			return position & 0x0000ffff;
		}
	}
	
	// 行番号short, ポジションintのNgram詳細.
	private static final class DetailNgram2_4 implements DetailNgram {
		private short rowId;
		private int position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = (short)(rowId & 0x0000ffff);
			this.position = position;
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId & 0x0000ffff;
		}
		@Override
		public final int getPosition() {
			return position;
		}
	}
	
	// 行番号short, ポジションbyteのNgram詳細.
	private static final class DetailNgram4_1 implements DetailNgram {
		private int rowId;
		private byte position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = rowId;
			this.position = (byte)(position & 0x000000ff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId;
		}
		@Override
		public final int getPosition() {
			return position & 0x000000ff;
		}
	}
	
	// 行番号short, ポジションshortのNgram詳細.
	private static final class DetailNgram4_2 implements DetailNgram {
		private int rowId;
		private short position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = rowId;
			this.position = (short)(position & 0x0000ffff);
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId;
		}
		@Override
		public final int getPosition() {
			return position & 0x0000ffff;
		}
	}
	
	// 行番号short, ポジションintのNgram詳細.
	private static final class DetailNgram4_4 implements DetailNgram {
		private int rowId;
		private int position;
		public final DetailNgram set(int rowId, int position) {
			this.rowId = rowId;
			this.position = position;
			return this;
		}
		@Override
		public final int getRowId() {
			return rowId;
		}
		@Override
		public final int getPosition() {
			return position;
		}
	}
}
