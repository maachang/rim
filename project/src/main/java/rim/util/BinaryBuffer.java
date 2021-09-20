package rim.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import rim.exception.RimException;

/**
 * バイナリバッファ.
 */
public class BinaryBuffer {
	/**
	 * 最小バッファ長.
	 */
	protected static final int MIN_LENGTH = 256;
	
	/**
	 * デフォルトバッファ長.
	 */
	protected static final int DEF_LENGTH = 512;

	// 1つのI/Oバッファの塊.
	protected static class Entry {
		// 次のEntry要素.
		Entry next;
		// バッファ情報.
		byte[] value;
		// 有効書き込み位置.
		int writePosition;
		// 読み込み位置.
		int readPosition;
		
		/**
		 * コンストラクタ.
		 * @param len 生成サイズを設定します.
		 */
		public Entry(int len) {
			next = null;
			value = new byte[len];
			writePosition = 0;
			readPosition = 0;
		}
		
		/**
		 * 書き込み可能な長さを取得.
		 * @return int 書き込み可能な長さが返却されます.
		 */
		public int writeRemaining() {
			return value.length - writePosition;
		}
		
		/**
		 * 書き込み可能な領域が存在するかチェック.
		 * @return boolean true の場合、書き込み可能です.
		 */
		public boolean hasWriteRemaining() {
			// 書き込み可能な長さが存在する場合.
			return value.length > writePosition;
		}
		
		/**
		 * 指定長の内容が書き込み可能かチェック.
		 * @param len 今回書き込む予定の長さを設定します.
		 * @return boolean true の場合、書き込み可能です.
		 */
		public boolean hasWriteRemaining(int len) {
			// 書き込み可能な長さが指定された書き込む予定の
			// 長さの範囲内の場合.
			return value.length >= writePosition + len;
		}
		
		/**
		 * 指定長の内容が書き込み可能かチェック.
		 * @param buf 今回書き込む予定のByteBufferを設定します.
		 * @return boolean true の場合、書き込み可能です.
		 */
		public boolean hasWriteRemaining(ByteBuffer buf) {
			return hasWriteRemaining(buf.remaining());
		}
		
		/**
		 * Entry内の読み込み可能な長さを取得.
		 * @return int 読み込み可能な長さが返却されます.
		 */
		public int readRemaining() {
			return writePosition - readPosition;
		}
		
		/**
		 * Entry内は読み込み可能かチェック.
		 * @return boolean true の場合、読み込み可能です.
		 */
		public boolean hasReadRemaining() {
			return writePosition > readPosition;
		}
		
		/**
		 * Entry内にバイト情報を書き込む.
		 * @param b バイト情報を設定します.
		 * @return boolean true の場合、正しく追加されました.
		 */
		public boolean write(byte b) {
			// Entryの書き込みが完了してる場合.
			if(!hasWriteRemaining()) {
				return false;
			}
			// １バイト書き込む.
			value[writePosition ++] = b;
			return true;
		}
		
		/**
		 * Entry内にバイナリ情報を書き込む.
		 * @param b バイナリを設定します.
		 * @param off オフセット値を設定します.
		 * @param len 長さを設定します.
		 * @return int 今回書き込まれた長さが返却されます.
		 */
		public int write(byte[] b, int off, int len) {
			int writeLen;
			// 書き込むバイナリが存在しない場合.
			if(len <= 0) {
				return 0;
			// 今回の書き込み内容で処理可能な場合.
			} else if(hasWriteRemaining(len)) {
				// 指定された長さ分書き込む.
				writeLen = len;
			// 指定されたバイナリの全てが書き込み可能でない場合.
			} else {
				// Entryに対して書き込み可能な長さを取得.
				writeLen = writeRemaining();
			}
			// 書き込み.
			System.arraycopy(b, off, value, writePosition, writeLen);
			// 書き込み長をEntryの有効データ長に反映.
			writePosition += writeLen;
			return writeLen;
		}
		
		/**
		 * Entry内にバイナリ情報を書き込む.
		 * @param buf ByteBufferを設定します.
		 * @return int 今回書き込まれた長さが返却されます.
		 */
		public int write(ByteBuffer buf) {
			int writeLen;
			// 書き込むバイナリ長を取得.
			final int len = buf.remaining();
			// 書き込みバッファが存在しない場合.
			if(len <= 0) {
				return 0;
			// 今回のByteBufferで処理可能な場合.
			} else if(hasWriteRemaining(len)) {
				// 今回のByteBuffer分書き込む.
				writeLen = len;
			// 指定されたバイナリの全てが書き込み可能でない場合.
			} else {
				// Entryに対して書き込み可能な長さを取得.
				writeLen = writeRemaining();
			}
			// 書き込み.
			buf.get(value, writePosition, writeLen);
			// 書き込み長をEntryの有効データ長に反映.
			writePosition += writeLen;
			return writeLen;
		}
		
		/**
		 * Entry内の読み込み処理.
		 * @param update 読み込み結果を更新する場合は true.
		 * @param out 読み込み内容を格納するバイナリ.
		 * @param off 読み込みバイナリのオフセット値を設定します.
		 * @param len 読み込みバイナリの長さを設定します.
		 * @return int 読み込まれた長さが返却されます.
		 */
		public int read(boolean update, byte[] out, int off, int len) {
			int readLen;
			// このEntryで読み込み可能な長さを取得.
			final int entryLen = readRemaining();
			// 読み込みバイナリ長が存在しない場合.
			// Entryで読み込み可能な内容が存在しない場合.
			if(len == 0 || entryLen == 0) {
				return 0;
			// Entryで読み込み可能な長さよりバイナリ長の方が長い場合.
			} else if(entryLen < len) {
				// 読み込み長をEntryで読み込み可能な長さを設定.
				readLen = entryLen;
			// 今回の読み込みバイナリで処理可能な場合.
			} else {
				readLen = len;
			}
			// 読み込み処理.
			System.arraycopy(value, readPosition, out, off, readLen);
			// 読み込み結果を更新する場合.
			if(update) {
				// 読み込み位置を更新.
				readPosition += readLen;
			}
			return readLen;
		}
		
		/**
		 * Entry内をスキップ.
		 * @param len スキップ対象の長さを設定します.
		 * @return int 読み込まれた長さが返却されます.
		 */
		public int skip(int len) {
			int readLen;
			// このEntryで読み込み可能な長さを取得.
			final int entryLen = readRemaining();
			// 読み込みバイナリ長が存在しない場合.
			// Entryで読み込み可能な内容が存在しない場合.
			if(len == 0 || entryLen == 0) {
				return 0;
			// Entryで読み込み可能な長さよりバイナリ長の方が長い場合.
			} else if(entryLen < len) {
				// 読み込み長をEntryで読み込み可能な長さを設定.
				readLen = entryLen;
			// 今回の読み込みバイナリで処理可能な場合.
			} else {
				readLen = len;
			}
			// 読み込み位置を更新.
			readPosition += readLen;
			return readLen;
		}
		
		/**
		 * バイト一致チェック.
		 * @param pos 参照する位置を設定します.
		 * @param b チェック対象のバイナリを設定します.
		 * @return boolean true の場合一致してます.
		 */
		public boolean equals(int pos, byte b) {
			return value[readPosition + pos] == b;
		}
	};

	// Entry.valueのバッファ長.
	private final int entryValueLength;

	// ラストEntry.
	private Entry lastEntry;
	
	// ファーストEntry.
	private Entry firstEntry;

	// 全体のデータ長.
	private int allLength;
	
	// クローズフラグ.
	private boolean closeFlag;

	/**
	 * コンストラクタ.
	 */
	public BinaryBuffer() {
		this(DEF_LENGTH);
	}
	
	/**
	 * コンストラクタ.
	 * @param size 対象の１データのバッファ長を設定します.
	 */
	public BinaryBuffer(int size) {
		if (size <= MIN_LENGTH) {
			size = MIN_LENGTH;
		}
		entryValueLength = size;
		lastEntry = null;
		firstEntry = null;
		allLength = 0;
		closeFlag = false;
	}
	
	
	// クローズチェック.
	protected void closeCheck() {
		if(closeFlag) {
			throw new RimException("It's already closed.");
		}
	}
	
	/**
	 * 情報クリア.
	 */
	public void clear() {
		closeCheck();
		lastEntry = null;
		firstEntry = null;
		allLength = 0;
	}

	/**
	 * クローズ.
	 * ※クローズした場合は、このオブジェクトは利用できなくなります.
	 */
	public void close() {
		if(!closeFlag) {
			clear();
			closeFlag = true;
		}
	}
	
	// 新しいEntryを追加.
	private final void addEntry() {
		// lastEntryに新しい領域を作成.
		lastEntry.next = new Entry(entryValueLength);
		// 今回作成したEntryをlastEntryに設定.
		lastEntry = lastEntry.next;
	}
	
	// Entryが生成されていない場合、新たに作成する.
	private final void createEntry() {
		// firstEntryが存在しない場合作成.
		if(firstEntry == null) {
			// firstEntryを新規作成.
			firstEntry = new Entry(entryValueLength);
			// lastEntryにセット.
			lastEntry = firstEntry;
		}
		// 現在のlastEntryの書き込み領域が存在しない場合.
		else if(!lastEntry.hasWriteRemaining()) {
			// 新しいEntryをlastEntryに追加.
			addEntry();
		}
	}
	
	/**
	 * 指定位置までの情報を取得.
	 * @param pos 指定位置を取得します.
	 * @retuen Object[] null の場合は指定位置の条件の取得に失敗しました.
	 *                  正しく取得できた場合は、以下の条件で取得されます.
	 *                  [0]: 指定位置Entry
	 *                  [1]: 指定位置Entryの読み込み開始ポジション.
	 */
	private final Object[] specifiedPosition(int pos) {
		int readLen;
		// 最初のEntryから読み取る.
		Entry entry = firstEntry;
		// 読み取り中のEntryが存在しなくなるまで読み取る.
		while(entry != null) {
			// 対象Entryの読み込み可能な長さを取得.
			readLen = entry.readRemaining();
			// 対象のEntryが指定位置の条件にマッチする.
			if(pos < readLen) {
				return new Object[] {entry, pos};
			}
			// 今回読み取った内容を読み込みデータ長から削除.
			pos -= readLen;
			// 次のEntryを取得.
			entry = entry.next;
		}
		return null;
	}

	/**
	 * 書き込み処理.
	 * @param b 対象のバイナリ情報を設定します.
	 */
	public void write(int b) {
		// クローズチェック.
		closeCheck();
		// Entryが生成されてない場合生成する.
		createEntry();
		// 書き込み処理.
		lastEntry.write((byte)b);
		// 全体長に書き込んだサイズをセット.
		allLength ++;
	}

	/**
	 * 書き込み処理.
	 * @param bin 対象のバイナリを設定します.
	 */
	public void write(byte[] bin) {
		write(bin, 0, bin.length);
	}

	/**
	 * 書き込み処理.
	 * @param bin 対象のバイナリを設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象のデータ長を設定します.
	 */
	public void write(byte[] bin, int off, int len) {
		// クローズチェック.
		closeCheck();
		// 書き込み内容が存在しない場合.
		if (len <= 0) {
			return;
		}
		// Entryが生成されてない場合生成する.
		createEntry();
		
		// 書き込み処理.
		int writeLen;
		while(true) {
			// １つのEntryに書き込む.
			writeLen = lastEntry.write(bin, off, len);
			// 今回書き込んだサイズをセット.
			off += writeLen;
			len -= writeLen;
			// 全体長に書き込んだサイズをセット.
			allLength += writeLen;
			// 書き込みが完了した場合.
			if(len <= 0) {
				return;
			}
			// 対象Entryの書き込みが完了した場合.
			else if(!lastEntry.hasWriteRemaining()) {
				// 新しいEntryを追加.
				addEntry();
			}
		}
	}

	/**
	 * データセット.
	 * @param 対象のByteBufferを設定します
	 * @exception IOException 例外.
	 */
	public void write(ByteBuffer buf) throws IOException {
		// クローズチェック.
		closeCheck();
		// 書き込み可能な情報が存在しない場合.
		if(!buf.hasRemaining()) {
			return;
		}
		// Entryが生成されてない場合生成する.
		createEntry();
		
		// 書き込み開始.
		int writeLen;
		while(true) {
			// １つのEntryに書き込む.
			writeLen = lastEntry.write(buf);
			// 全体長に書き込んだサイズをセット.
			allLength += writeLen;
			// 書き込みが完了した場合.
			if(!buf.hasRemaining()) {
				return;
			}
			// 対象Entryの書き込みが完了した場合.
			else if(!lastEntry.hasWriteRemaining()) {
				// 新しいEntryを追加.
				addEntry();
			}
		}
	}

	/**
	 * 現在の書き込みバッファ長を取得.
	 * @return int 書き込みバッファ長が返却されます.
	 */
	public int size() {
		// クローズチェック.
		closeCheck();
		return allLength;
	}

	/**
	 * 情報の参照取得.
	 * ※この処理では、参照モードで情報を取得します.
	 * @param buf 対象のバッファ情報を設定します.
	 * @return int 取得された情報長が返却されます.
	 * @exception IOException IO例外.
	 */
	public int peek(byte[] buf) throws IOException {
		return peek(buf, 0, buf.length);
	}

	/**
	 * 情報の参照取得.
	 * ※この処理では、参照モードで情報を取得します.
	 * @param buf 対象のバッファ情報を設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象の長さを設定します.
	 * @return int 取得された情報長が返却されます.
	 */
	public int peek(byte[] buf, int off, int len) {
		// クローズチェック.
		closeCheck();
		int readLen;
		int ret = 0;
		// 最初のEntryから読み取る.
		Entry entry = firstEntry;
		// 読み取り中のEntryが存在しなくなるまで読み取る.
		while(entry != null) {
			// 対象のEntryを読み取る(読み込み更新なし).
			readLen = entry.read(false, buf, off, len);
			// 今回読み取ったデータ長をセット.
			off += readLen;
			len -= readLen;
			ret += readLen;
			// 読み込みが完了した場合.
			if(len <= 0) {
				// 読み取り完了.
				break;
			}
			// 次のEntryを取得.
			entry = entry.next;
		}
		// 読み取り完了.
		return ret;
	}

	/**
	 * 情報の取得.
	 * @param buf 対象のバッファ情報を設定します.
	 * @return int 取得された情報長が返却されます.
	 */
	public int read(byte[] buf) {
		return read(buf, 0, buf.length);
	}

	/**
	 * 情報の取得.
	 * @param buf 対象のバッファ情報を設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象の長さを設定します.
	 * @return int 取得された情報長が返却されます.
	 */
	public int read(byte[] buf, int off, int len) {
		// クローズチェック.
		closeCheck();
		int readLen;
		int ret = 0;
		// 最初のEntryから読み取る.
		Entry entry = firstEntry;
		// 読み取り中のEntryが存在しなくなるまで読み取る.
		while(entry != null) {
			// 対象のEntryを読み取る(読み込み更新あり).
			readLen = entry.read(true, buf, off, len);
			// 今回読み取ったデータ長をセット.
			off += readLen;
			len -= readLen;
			ret += readLen;
			// 読み込みが完了した場合.
			if(len <= 0) {
				// 読み取り完了.
				break;
			}
			// 次のEntryを取得.
			entry = entry.next;
		}
		// 次の読み込み開始位置のEntryをFirstEntryにセット.
		firstEntry = entry;
		// 全体長から今回の取得長を反映する.
		allLength -= ret;
		return ret;
	}

	/**
	 * スキップ.
	 * @parma len スキップするデータ長を設定します.
	 * @return int 実際にスキップされた数が返却されます.
	 *             [-1]が返却された場合、オブジェクトはクローズしています.
	 */
	public int skip(int len) {
		// クローズチェック.
		closeCheck();
		int readLen;
		int ret = 0;
		// 最初のEntryから読み取る.
		Entry entry = firstEntry;
		// 読み取り中のEntryが存在しなくなるまで読み取る.
		while(entry != null) {
			// 対象のEntryをスキップする.
			readLen = entry.skip(len);
			// 今回読み取ったデータ長をセット.
			len -= readLen;
			ret += readLen;
			// 読み込みが完了した場合.
			if(len <= 0) {
				// 読み取り完了.
				break;
			}
			// 次のEntryを取得.
			entry = entry.next;
		}
		// 次の読み込み開始位置のEntryをFirstEntryにセット.
		firstEntry = entry;
		// 全体長から今回の取得長を反映する.
		allLength -= ret;
		return ret;
	}
	
	/**
	 * データ取得.
	 * @return byte[] 設定されているデータを全て取得します.
	 */
	public byte[] toByteArray() {
		return toByteArray(false);
	}

	/**
	 * データ取得.
	 * @param readByClear true の場合取得したデータは削除されます.
	 * @return byte[] 設定されているデータを全て取得します.
	 */
	public byte[] toByteArray(boolean readByClear) {
		int len;
		// 現在の読み込み可能なバイナリ長のデータを生成.
		byte[] ret = new byte[allLength];
		// peekで読み取る.
		len = peek(ret, 0, allLength);
		if(readByClear) {
			clear();
		}
		// 読み込み可能な長さが取得した長さと一致しない場合は例外.
		if(len != allLength) {
			throw new RimException("The planned length (" + allLength
				+ ") does not match the obtained length (" + len + ").");
		}
		return ret;
	}
	
	/**
	 * 指定条件の位置を取得.
	 * @param chk チェック対象のバイナリ情報を設定します.
	 * @return int 取得データ長が返却されます.
	 *             [-1]の場合は情報は存在しません.
	 */
	public int indexOf(final byte[] chk) {
		return indexOf(chk, 0);
	}

	/**
	 * 指定条件の位置を取得.
	 * @param chk チェック対象のバイナリ情報を設定します.
	 * @param off 検索開始位置を設定します.
	 * @return int 取得データ長が返却されます.
	 *             [-1]の場合は情報は存在しません.
	 */
	public int indexOf(final byte[] chk, int off) {
		closeCheck();
		// 開始位置を取得.
		Object[] o = specifiedPosition(off);
		// 取得に失敗した場合.
		if(o == null) {
			// 検索結果なし.
			return -1;
		}
		
		// 開始位置の情報を取得.
		Entry entry = (Entry)o[0];
		int pos = (Integer)o[1];
		o = null;
		
		// 対象の最初の１文字.
		final byte firstChk = chk[0];
		// 対象の長さ.
		final int chkLength = chk.length;
		// 累積読み込みカウント.
		int readCount = 0;
		
		int i, j, len;
		int chkPos, secondPos, lenJ;
		Entry secondEntry;
		
		// 検索開始.
		while(entry != null) {
			// Entryで読み込み可能な長さを取得.
			len = entry.readRemaining();
			for(i = pos; i < len; i ++) {
				// 対象の１文字目の内容が一致した場合.
				if(entry.equals(i, firstChk)) {
					// 対象の長さが１文字の場合.
					if(chkLength == 1) {
						// 検索結果を返却.
						return i + readCount;
					}
					
					// 2文字目からチェック.
					chkPos = 1;
					// 2文字目の開始番号.
					secondPos = i + 1;
					// Entryの長さ.
					lenJ = len;
					// 対象Entry.
					secondEntry = entry;
					
					// ２文字以降を検索.
					while(secondEntry != null) {
						// 検索開始.
						for(j = secondPos; j < lenJ; j ++) {
							// 2文字目以降の内容が一致した場合.
							if(secondEntry.equals(j, chk[chkPos ++])) {
								// 対象が見つかった場合.
								if(chkPos == chkLength) {
									// 検索結果を返却.
									return i + readCount;
								}
							// 2文字目以降の内容の不一致.
							} else {
								// whileループを終わらせる.
								secondEntry = null;
								break;
							}
						}
						// 次のEntryに跨いだ検索の場合.
						if(secondEntry != null) {
							// 次のEntryをセット.
							secondEntry = secondEntry.next;
							// 次の情報が存在する場合.
							if(secondEntry != null) {
								// 次の読み込み可能な長さを取得する.
								lenJ = secondEntry.readRemaining();
								// positionを0にセット.
								secondPos = 0;
							}
						}
					}
				}
			}
			// 次のEntryをセット.
			entry = entry.next;
			// positionを0にセット.
			pos = 0;
			// 累積読み込みカウントに今回分の
			// 読み込み長をセット.
			readCount += len;
		}
		return -1;
	}

	/**
	 * データが存在するかチェック.
	 * @return boolean [true]の場合、空です.
	 */
	public boolean isEmpty() {
		closeCheck();
		return allLength == 0;
	}
}
