package rim.compress;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rim.util.Flag;

/**
 * ZStandard-Compress.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class ZstdCompress {
	/**
	 * デフォルトの圧縮レベル.
	 */
	public static final int DEFAULT_LEVEL =3;
	
	// パッケージ名.
	private static final String ZSTD_PACKAGE = "com.github.luben.zstd";
	
	// 同期オブジェクト.
	private final Object sync = new Object();
	
	// 初期化フラグ.
	private final Flag initFlag = new Flag(false);
	
	// 初期化成功フラグ.
	private boolean initSuccessFlag;
	

	// boolean isError(long code)
	private Method zst_isError;
	
	// String getErrorName(long code)
	private Method zst_getErrorName;
	
	// long getErrorCode(long code)
	private Method zst_getErrorCode;
	
	// long compressBound(long srcSize)
	private Method zst_compressBound;
	
	// long decompressedSize(byte[] src, int srcPosition, int srcSize)
	private Method zstd_decompressedSize;
	
	// long compressByteArray(
	//     byte[] dst, int dstOffset, int dstSize, byte[] src, int srcOffset,
	//      int srcSize, int level, boolean checksumFlag)
	private Method zstd_compressByteArray;
	
	// long decompressByteArray(
	//     byte[] dst, int dstOffset, int dstSize, byte[] src, int srcOffset,
	//     int srcSize)
	private Method zstd_decompressByteArray;
	
	// コンストラクタ.
	private ZstdCompress() {}

	// シングルトン.
	private static final ZstdCompress INST = new ZstdCompress();
	
	/**
	 * オブジェクトを取得.
	 * @return ZstdCompress オブジェクトが返却されます.
	 */
	public static final ZstdCompress getInstance() {
		return INST.init();
	}
	
	// 初期化処理.
	private final ZstdCompress init() {
		// 既に初期化済みの場合.
		if(this.initFlag.get()) {
			return this;
		}
		synchronized(sync) {
			// 別のスレッドが処理して初期化済みの場合.
			if(this.initFlag.get()) {
				return this;
			}
			
			try {
				// LZ4のクラスを取得.
				final Class zstd = Class.forName(
					ZSTD_PACKAGE + ".Zstd");
				
				// Zstd.isError(static).
				this.zst_isError = zstd.getMethod("isError",
					Long.TYPE
				);
				// Zstd.getErrorName(static).
				this.zst_getErrorName = zstd.getMethod("getErrorName",
					Long.TYPE
				);
				// Zstd.getErrorCode(static).
				this.zst_getErrorCode = zstd.getMethod("getErrorCode",
					Long.TYPE
				);
				// Zstd.compressBound(static).
				this.zst_compressBound = zstd.getMethod("compressBound",
					Long.TYPE
				);
				// Zstd.decompressedSize(static).
				this.zstd_decompressedSize = zstd.getMethod("decompressedSize",
					byte[].class, Integer.TYPE, Integer.TYPE
				);
				
				// Zstd.compressByteArray(static).
				this.zstd_compressByteArray = zstd.getMethod("compressByteArray",
					byte[].class, Integer.TYPE, Integer.TYPE,
					byte[].class, Integer.TYPE, Integer.TYPE,
					Integer.TYPE, Boolean.TYPE
				);
				
				// Zstd.decompressByteArray(static).
				this.zstd_decompressByteArray = zstd.getMethod("decompressByteArray",
					byte[].class, Integer.TYPE, Integer.TYPE,
					byte[].class, Integer.TYPE, Integer.TYPE
				);
				
				// 正常読み込み完了.
				this.initSuccessFlag = true;
			} catch(Throwable t) {
				// errorの場合は初期化失敗.
				this.initSuccessFlag = false;
				// クリア.
				this.zst_isError = null;
				this.zst_getErrorName = null;
				this.zst_getErrorCode = null;
				this.zst_compressBound = null;
				this.zstd_decompressedSize = null;
				this.zstd_decompressedSize = null;
				this.zstd_compressByteArray = null;
				this.zstd_decompressByteArray = null;
			}
			this.initFlag.set(true);
		}
		return this;
	}

	/**
	 * LZ4の圧縮解凍が利用可能かチェック.
	 * @return boolean trueの場合は利用可能です.
	 */
	public final boolean isSuccessLibrary() {
		return initSuccessFlag;
	}
	
	// LZ4が利用可能かチェック.
	private final void checkNoSuccess() {
		if(!initSuccessFlag) {
			throw new CompressException(
				"Zstd compression / decompression is not available.");
		}
	}
	
	// エラーチェック.
	private final long checkError(long code) throws Exception {
		if((boolean)zst_isError.invoke(null, code)) {
			String errorName = (String)zst_getErrorName.invoke(null, code);
			long errorCode = (long)zst_getErrorCode.invoke(null, code);
			throw new CompressException("error: " + errorCode + " " + errorName);
		}
		return code;
	}
	
	/**
	 * 設定可能な圧縮レベルを取得します.
	 * @param level 対象の圧縮レベルを設定します.
	 *              1から22までが利用可能です.
	 * @return boolean trueの場合、設定可能です.
	 */
	public static final boolean isLevel(int level) {
		if(level <= 0 || level > 22) {
			return false;
		}
		return true;
	}
	
	/**
	 * 圧縮バイナリバッファの生成サイズを取得.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @return long 圧縮バイナリバッファの生成サイズが返却されます.
	 */
	public long maxCompressLength(long srcSize) {
		checkNoSuccess();
		try {
			return (long)zst_compressBound.invoke(null, srcSize);
		} catch(CompressException ce) {
			throw ce;
		} catch(Exception e) {
			if(e instanceof InvocationTargetException) {
				throw new CompressException(
					((InvocationTargetException)e).getTargetException());
			}
			throw new CompressException(e);
		}
	}
	
	/**
	 * 圧縮元のバイナリサイズを取得.
	 * @param src 元のバイナリを設定します.
	 * @return long 圧縮元のバイナリサイズが返却されます.
	 */
	public long decompressSize(byte[] src) {
		return decompressSize(src, 0, src.length);
	}
	
	/**
	 * 圧縮元のバイナリサイズを取得.
	 * @param src 元のバイナリを設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @return long 圧縮元のバイナリサイズが返却されます.
	 */
	public long decompressSize(byte[] src, int srcSize) {
		return decompressSize(src, 0, srcSize);
	}
	
	/**
	 * 圧縮元のバイナリサイズを取得.
	 * @param src 元のバイナリを設定します.
	 * @param srcOffset 元のバイナリのオフセット値を設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @return long 圧縮元のバイナリサイズが返却されます.
	 */
	public long decompressSize(byte[] src, int srcOffset, int srcSize) {
		checkNoSuccess();
		try {
			return (long)zstd_decompressedSize.invoke(null,
				src, srcOffset, srcSize);
		} catch(CompressException ce) {
			throw ce;
		} catch(Exception e) {
			if(e instanceof InvocationTargetException) {
				throw new CompressException(
					((InvocationTargetException)e).getTargetException());
			}
			throw new CompressException(e);
		}
	}
	
	/**
	 * 圧縮処理.
	 * @param buf 圧縮結果を受け取るバッファを設定します.
	 * @param src 元のバイナリを設定します.
	 * @param srcOffset 元のバイナリのオフセット値を設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @param level 圧縮レベルを設定します（デフォルト３）
	 * @return CompressBuffer 圧縮された内容が返却されます.
	 */
	public CompressBuffer compress(CompressBuffer buf,
		byte[] src, int srcOffset, int srcSize,
		int level) {
		return compress(buf, src, srcOffset, srcSize, level, false);
	}
	
	/**
	 * 圧縮処理.
	 * @param buf 圧縮結果を受け取るバッファを設定します.
	 * @param src 元のバイナリを設定します.
	 * @param srcOffset 元のバイナリのオフセット値を設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @param level 圧縮レベルを設定します（デフォルト３）
	 * @param checksumFlag チェックサムを入れる場合はtrue.
	 * @return CompressBuffer 圧縮された内容が返却されます.
	 */
	public CompressBuffer compress(CompressBuffer buf,
		byte[] src, int srcOffset, int srcSize,
		int level, boolean checksumFlag) {
		// バッファが設定されてない場合は生成.
		if(buf == null) {
			buf = new CompressBuffer();
		}
		// 圧縮後の最大バッファ長を取得.
		int size = (int)maxCompressLength(srcSize);
		// 取得したバッファ長を割り当て.
		buf.clear(size);
		buf.setLimit(size);
		
		// 圧縮処理.
		int compSize = (int)compress(buf.getRawBuffer(), 0, size,
			src, srcOffset, srcSize, level, checksumFlag);
		// 圧縮サイズをバッファにセット.
		buf.setLimit(compSize);
		return buf;
	}
	
	/**
	 * 圧縮処理.
	 * @param dst 圧縮結果を受け取るバイナリを設定します.
	 * @param dstOffset 圧縮結果を受け取るバイナリのオフセット値を設定します.
	 * @param dstSize 圧縮結果を受け取るバイナリの長さを設定します.
	 * @param src 元のバイナリを設定します.
	 * @param srcOffset 元のバイナリのオフセット値を設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @param level 圧縮レベルを設定します（デフォルト３）
	 * @return long 圧縮結果のバイナリ長が返却されます.
	 */
	public long compress(byte[] dst, int dstOffset, int dstSize,
		byte[] src, int srcOffset, int srcSize, int level) {
		return compress(dst, dstOffset, dstSize,
			src, srcOffset, srcSize, level, false);
	}
	
	/**
	 * 圧縮処理.
	 * @param dst 圧縮結果を受け取るバイナリを設定します.
	 * @param dstOffset 圧縮結果を受け取るバイナリのオフセット値を設定します.
	 * @param dstSize 圧縮結果を受け取るバイナリの長さを設定します.
	 * @param src 元のバイナリを設定します.
	 * @param srcOffset 元のバイナリのオフセット値を設定します.
	 * @param srcSize 元のバイナリの長さを設定します.
	 * @param level 圧縮レベルを設定します（デフォルト３）
	 * @param checksumFlag チェックサムを入れる場合はtrue.
	 * @return long 圧縮結果のバイナリ長が返却されます.
	 */
	public long compress(byte[] dst, int dstOffset, int dstSize,
		byte[] src, int srcOffset, int srcSize, int level, boolean checksumFlag) {
		checkNoSuccess();
		try {
			return checkError((long)zstd_compressByteArray.invoke(null,
				dst, dstOffset, dstSize, src, srcOffset, srcSize, level, checksumFlag));
		} catch(CompressException ce) {
			throw ce;
		} catch(Exception e) {
			if(e instanceof InvocationTargetException) {
				throw new CompressException(
					((InvocationTargetException)e).getTargetException());
			}
			throw new CompressException(e);
		}
	}
	
	/**
	 * 解凍処理.
	 * @param dstBuf 解凍を受け取るバッファを設定します.
	 * @return CompressBuffer 解凍結果が返却されます.
	 */
	public CompressBuffer decompress(CompressBuffer dstBuf, CompressBuffer srcBuf) {
		return decompress(dstBuf, srcBuf.getRawBuffer(), 0, srcBuf.getLimit());
	}
	
	/**
	 * 解凍処理.
	 * @param buf 解凍を受け取るバッファを設定します.
	 * @param src 圧縮バイナリを設定します.
	 * @param srcOffset 圧縮バイナリのオフセット値を設定します.
	 * @param srcSize 圧縮バイナリの長さを設定します.
	 * @return CompressBuffer 解凍結果が返却されます.
	 */
	public CompressBuffer decompress(CompressBuffer buf,
		byte[] src, int srcOffset, int srcSize) {
		// バッファが設定されてない場合は生成.
		if(buf == null) {
			buf = new CompressBuffer();
		}
		// 元のバイナリサイズを取得.
		int size = (int)decompressSize(src, srcOffset, srcSize);
		// 取得したバッファ長を割り当て.
		buf.clear(size);
		buf.setLimit(size);
		
		// 解凍処理.
		decompress(buf.getRawBuffer(), 0, size,
			src, srcOffset, srcSize);
		return buf;
	}
	
	/**
	 * 解凍処理.
	 * @param dst 解凍を受け取るバイナリを設定します.
	 * @param dstOffset 解凍結果を受け取るバイナリのオフセット値を設定します.
	 * @param dstSize 解凍結果を受け取るバイナリの長さを設定します.
	 * @param src 圧縮バイナリを設定します.
	 * @param srcOffset 圧縮バイナリのオフセット値を設定します.
	 * @param srcSize 圧縮バイナリの長さを設定します.
	 * @return long 解凍結果のバイナリ長が返却されます.
	 */
	public long decompress(byte[] dst, int dstOffset, int dstSize,
		byte[] src, int srcOffset, int srcSize) {
		checkNoSuccess();
		try {
			return checkError((long)zstd_decompressByteArray.invoke(null,
				dst, dstOffset, dstSize, src, srcOffset, srcSize));
		} catch(CompressException ce) {
			throw ce;
		} catch(Exception e) {
			if(e instanceof InvocationTargetException) {
				throw new CompressException(
					((InvocationTargetException)e).getTargetException());
			}
			throw new CompressException(e);
		}
	}
}
