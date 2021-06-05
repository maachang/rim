package rim.compress;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rim.util.Flag;

/**
 * LZ4圧縮・解凍処理.
 * 
 * ライブラリが正常に読まれない場合も考慮して
 * Reflectionですべて処理を行う.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class Lz4Compress {
	// LZ4-Package.
	private static final String LZ4_PACKAGE = "net.jpountz.lz4";
	
	// 同期オブジェクト.
	private final Object sync = new Object();
	
	// 初期化フラグ.
	private final Flag initFlag = new Flag(false);
	
	// 初期化成功フラグ.
	private boolean initSuccessFlag;
	
	// net.jpountz.lz4.LZ4Compressor.
	private Object lz4Compressor;
	// net.jpountz.lz4.LZ4FastDecompressor.
	private Object lz4FastDecompressor;
	
	// lz4Compressor.compress().
	private Method lz4Compressor_compress;
	// lz4FastDecompressor.decompress().
	private Method lz4FastDecompressor_decompress;
	
	// コンストラクタ.
	private Lz4Compress() {}

	// シングルトン.
	private static final Lz4Compress INST = new Lz4Compress();
	
	/**
	 * オブジェクトを取得.
	 * @return Lz4Compress オブジェクトが返却されます.
	 */
	public static final Lz4Compress getInstance() {
		return INST.init();
	}
	
	// 初期化処理.
	private final Lz4Compress init() {
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
				final Class lz4Fct = Class.forName(
					LZ4_PACKAGE + ".LZ4Factory");
				final Class lz4Cmp = Class.forName(
					LZ4_PACKAGE + ".LZ4Compressor");
				final Class lz4Fdc = Class.forName(
					LZ4_PACKAGE + ".LZ4FastDecompressor");
				
				// Lz4Factory Lz4Factory.fastestInstance() メソッド(static).
				final Method fctInst = lz4Fct.getMethod("fastestInstance");
				
				// LZ4Compressor fctInst.fastCompressor() メソッド.
				final Method fctCmp = lz4Fct.getMethod("fastCompressor");
				
				// LZ4FastDecompressor fctInst.fastDecompressor() メソッド.
				final Method fctFdCmp = lz4Fct.getMethod("fastDecompressor");
				
				// Lz4Factoryを取得.
				final Object lz4Factory = fctInst.invoke(null);
				
				// LZ4Compressorを取得.
				this.lz4Compressor = fctCmp.invoke(lz4Factory);
				
				// LZ4FastDecompressorを取得.
				this.lz4FastDecompressor = fctFdCmp.invoke(lz4Factory);
				
				// lz4Compressor.compress() メソッド.
				// int compress(byte[] src, int soff, int slen, byte[] dest, int dlen, int dlen)
				//  src: 元のバイナリ.
				//  soff: 元バイナリのオフセット値.
				//  slen: 元バイナリの長さ.
				//  dest: 圧縮先のバイナリ.
				//  doff: 圧縮先バイナリのオフセット値.
				//  dlen: 圧縮先バイナリの長さ.
				//  戻り値(int): 圧縮されたバイナリ長が返却されます.
				this.lz4Compressor_compress = lz4Cmp.getMethod("compress",
					byte[].class, Integer.TYPE, Integer.TYPE, byte[].class, Integer.TYPE, Integer.TYPE);
				
				// lz4FastDecompressor.decompress() メソッド.
				// int decompress(byte[] src, int soff, byte[] dest, int dlen, int dlen)
				//  src: 元のバイナリ.
				//  soff: 元バイナリのオフセット値.
				//  dest: 解凍先のバイナリ.
				//  doff: 解凍先バイナリのオフセット値.
				//  dlen: 解凍先バイナリの長さ.
				//  戻り値(int): 解凍されたバイナリ長が返却されます.
				this.lz4FastDecompressor_decompress = lz4Fdc.getMethod("decompress",
					byte[].class, Integer.TYPE, byte[].class, Integer.TYPE, Integer.TYPE);
				
				// 無事読み込めた場合は初期化成功.
				this.initSuccessFlag = true;
			} catch(Throwable t) {
				// errorの場合は初期化失敗.
				this.initSuccessFlag = false;
				// クリア.
				this.lz4Compressor = null;
				this.lz4FastDecompressor = null;
				this.lz4Compressor_compress = null;
				this.lz4FastDecompressor_decompress = null;
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
				"LZ4 compression / decompression is not available.");
		}
	}
	
	/**
	 * 圧縮前の元の長さをバイナリに保存した場合のバイト数を計算します.
	 * @param srcLength 圧縮前の元の長さを設定します.
	 * @return int 書き込まれたバイト数が返却されます.
	 */
	public final int writeSrcLengthToByteLength(int srcLength) {
		int p = 0;
		int n = srcLength;
		while (n > 0) {
			n >>= 7;
			p ++;
		}
		return p;
	}
	
	/**
	 * 圧縮前の元の長さをバイナリに保存.
	 * @param out 保存先のバイナリを設定します.
	 * @param off バイナリのオフセット値を設定します.
	 * @param srcLength 圧縮前の元の長さを設定します.
	 * @return int 書き込まれたバイト数が返却されます.
	 */
	public final int writeSrcLength(byte[] out, int off, int srcLength) {
		int p = 0;
		int n = srcLength;
		while (n > 0) {
			out[off + p++] = (n >= 128) ? (byte) (0x080 | (n & 0x07f)) : (byte) n;
			n >>= 7;
		}
		return p;
	}
	
	/**
	 * 解凍された時のバッファサイズの取得.
	 * @param outPos 読み込まれたバイト数が返却されます.
	 * @param binary 対象のバイナリを設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象のデータ長を設定します.
	 * @return int 解凍対象のバイナリサイズが返却されます.
	 */
	public final int decompressLength(int[] outReadByte, byte[] binary, int off, int len) {
		int ret = 0,
			shift = 0,
			p= 0;
		do {
			if(p > len) {
				throw new IndexOutOfBoundsException(
					"The binary data length is not enough for the original data " +
					"length acquisition.");
			}
			ret += (binary[off + p] & 0x07f) << (shift++ * 7);
		} while((binary[off + p ++] & 0x080) == 0x080);
		if(outReadByte != null) {
			outReadByte[0] = p;
		}
		return ret;
	}

	// 最大の入力データー数.
	private static final int MAX_INPUT_SIZE = 0x7E000000;

	/**
	 * 元の長さを指定して、圧縮後で必要な最大バイト数を取得.
	 * @param length 元の長さを設定します.
	 * @return int 圧縮後で必要な最大バイト数が返却されます.
	 */
	public final int maxCompressedLength(int length) {
		checkNoSuccess();
		if (length < 0) {
			throw new IllegalArgumentException("length must be >= 0, got " + length);
		} else if (length >= MAX_INPUT_SIZE) {
			throw new IllegalArgumentException("length must be < " + MAX_INPUT_SIZE);
		}
		return length + (length >> 8) + 16;
	}
	
	/**
	 * 圧縮処理.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @param iLen 元の長さを設定します.
	 * @return CompressBuffer 圧縮結果が返却されます.
	 */
	public final CompressBuffer compress(byte[] in, int iOff, int iLen) {
		return compress(null, in, iOff, iLen);
	}

	/**
	 * 圧縮処理.
	 * @param out 圧縮結果を格納するCompressBufferを設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @param iLen 元の長さを設定します.
	 * @return CompressBuffer 圧縮結果が返却されます.
	 */
	public final CompressBuffer compress(CompressBuffer out, byte[] in, int iOff, int iLen) {
		if(out == null) {
			out = new CompressBuffer();
			out.clear(maxCompressedLength(iLen));
		}
		final int len = compress(out.getRawBuffer(), 0, out.getRawBufferLength(),
			in, iOff, iLen);
		out.setLimit(len);
		return out;
	}
	
	/**
	 * 圧縮処理.
	 * @param out 圧縮内容を格納するバイナリを設定します.
	 * @param oOff 圧縮内容を格納するバイナリのオフセット値を設定します.
	 * @param oLen 圧縮内容を格納するバイナリの長さを設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @param iLen 元の長さを設定します.
	 * @return int 圧縮されたバイナリ長が返却されます.
	 */
	public final int compress(byte[] out, int oOff, int oLen,
		byte[] in, int iOff, int iLen) {
		checkNoSuccess();
		try {
			// 圧縮処理.
			return (int)lz4Compressor_compress.invoke(lz4Compressor,
				in, iOff, iLen, out, oOff, oLen);
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
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @return CompressBuffer 解凍されたCompressBufferが返却されます.
	 */
	public final CompressBuffer decompress(byte[] in, int iOff) {
		return decompress(null, in, iOff);
	}
	
	/**
	 * 解凍処理.
	 * @param out 解凍内容を格納するCompressBufferを設定します.
	 *            この値は「decompressLength()メソッド」で取得した値を
	 *            out.setLimit()に設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @return CompressBuffer 解凍されたCompressBufferが返却されます.
	 */
	public final CompressBuffer decompress(CompressBuffer out, byte[] in, int iOff) {
		decompress(out.getRawBuffer(), 0, out.getLimit(), in, iOff);
		return out;
	}
	
	/**
	 * 解凍処理.
	 * @param out 解凍内容を格納するバイナリを設定します.
	 *             この値は「decompressLength()メソッド」で取得した値より
	 *             同じ以上のバイナリサイズを設定します.
	 * @param oOff 解凍内容を格納するバイナリのオフセット値を設定します.
	 * @param oLen 解凍内容を格納するバイナリの長さを設定します.
	 *             この値は「decompressLength()メソッド」で取得した値を
	 *             設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 */
	public final void decompress(byte[] out, int oOff, int oLen, byte[] in, int iOff) {
		checkNoSuccess();
		try {
			// 解凍処理.
			lz4FastDecompressor_decompress.invoke(lz4FastDecompressor,
				in, iOff, out, oOff, oLen);
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
