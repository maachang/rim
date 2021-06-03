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
public class Lz4Compress {
	// LZ4-Package.
	private String lz4PackageName = "net.jpountz.lz4";
	
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
	
	// lz4Compressor.maxCompressedLength().
	private Method lz4Compressor_maxCompressedLength;
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
	 * @return Lz4Compress() オブジェクトが返却されます.
	 */
	public static final Lz4Compress getInstance() {
		return INST.init();
	}
	
	// 初期化処理.
	private final Lz4Compress init() {
		// 既に初期化済みの場合.
		if(initFlag.get()) {
			return this;
		}
		synchronized(sync) {
			// 別のスレッドが処理して初期化済みの場合.
			if(initFlag.get()) {
				return this;
			}
			
			try {
				// LZ4のクラスを取得.
				final Class lz4Fct = Class.forName(
					lz4PackageName + ".LZ4Factory");
				final Class lz4Cmp = Class.forName(
					lz4PackageName + ".LZ4Compressor");
				final Class lz4Fdc = Class.forName(
					lz4PackageName + ".LZ4FastDecompressor");
				
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
				
				// lz4Compressor.maxCompressedLength() メソッド.
				// int maxCompressedLength(int length)
				//  length: 圧縮対象のバイナリ長を設定.
				//  戻り値(int): 圧縮時に必要なバイナリ長を取得.
				this.lz4Compressor_maxCompressedLength =
					lz4Cmp.getMethod("maxCompressedLength", Integer.TYPE);
				
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
				initSuccessFlag = true;
			} catch(Throwable t) {
				// errorの場合は初期化失敗.
				initSuccessFlag = false;
			}
			initFlag.set(true);
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
	 * 解凍された時のバッファサイズの取得.
	 * @param binary 対象のバイナリを設定します.
	 * @param off 対象のオフセット値を設定します.
	 * @param len 対象のデータ長を設定します.
	 * @return int 解凍対象のバイナリサイズが返却されます.
	 */
	public final int decompressLength(byte[] binary, int off) {
		checkNoSuccess();
		return decompressLength(null, binary, off, binary.length);
	}
	
	// 解凍された時のバッファサイズの取得.
	private final int decompressLength(int[] out, byte[] binary, int off, int len) {
		int ret = 0,
			shift = 0,
			p= 0;
		do {
			if(p > len) {
				throw new IndexOutOfBoundsException(
					"The binary data length is not enough for the original data " +
					"length acquisition.");
			}
			ret += (binary[off+p] & 0x07f) << (shift++ * 7);
		} while((binary[off+p++] & 0x080) == 0x080);
		if(out != null) {
			out[0] = p;
		}
		return ret;
	}
	
	/**
	 * 元の長さを指定して、圧縮後で必要な最大バイト数を取得.
	 * @param len 元の長さを設定します.
	 * @return int 圧縮後で必要な最大バイト数が返却されます.
	 */
	public final int maxCompressedLength(int len) {
		checkNoSuccess();
		try {
			// 8byteは圧縮前のデータを確保する領域.
			return 8 + (int)lz4Compressor_maxCompressedLength
				.invoke(lz4Compressor, len);
		} catch(InvocationTargetException iv) {
			throw new CompressException(iv.getTargetException());
		} catch(Exception e) {
			throw new CompressException(e);
		}
	}
	
	/**
	 * 圧縮処理.
	 * @param out 圧縮内容を格納するバイナリを設定します.
	 * @param oOff 圧縮内容を格納するバイナリのオフセット値を設定します.
	 * @param oLen 圧縮内容を格納するバイナリの長さを設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @param iLen 元の長さを設定します.
	 * @return 実際に圧縮されたバイナリ長を設定します.
	 */
	public final int compress(byte[] out, int oOff, int oLen, byte[] in, int iOff, int iLen) {
		checkNoSuccess();
		try {
			
			// 圧縮前の内容を保存.
			int p = 0;
			int n = iLen;
			while (n > 0) {
				out[oOff + p++] = (n >= 128) ? (byte) (0x080 | (n & 0x07f)) : (byte) n;
				n >>= 7;
			}
			
			// 圧縮処理.
			return (int)lz4Compressor_compress.invoke(lz4Compressor,
				in, iOff, iLen, out, oOff + p, oLen - p);
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
	 * @param out 解凍内容を格納するバイナリを設定します.
	 * @param oOff 解凍内容を格納するバイナリのオフセット値を設定します.
	 * @param oLen 解凍内容を格納するバイナリの長さを設定します.
	 * @param in 元のバイナリを設定します.
	 * @param iOff 元のオフセット値を設定します.
	 * @return 実際に解凍されたバイナリ長を設定します.
	 */
	public final int decompress(byte[] out, int oOff, int oLen, byte[] in, int iOff) {
		checkNoSuccess();
		try {
			// 解凍後の長さを取得.
			int[] p = new int[1];
			int destLen = decompressLength(p, in, iOff, in.length);
			iOff += p[0]; p = null;
			
			// decompressLengthで取得した長さに対して設定した解凍後の格納先が少ない場合.
			if(destLen > oLen) {
				throw new CompressException(
					"The binary size of the decompression result is small (" +
					destLen + " : " + oLen + ")");
			}
			// 解凍処理.
			lz4FastDecompressor_decompress.invoke(lz4FastDecompressor,
				in, iOff, out, oOff, oLen);
			
			return destLen;
		} catch(Exception e) {
			if(e instanceof InvocationTargetException) {
				throw new CompressException(
					((InvocationTargetException)e).getTargetException());
			}
			throw new CompressException(e);
		}
	}
}
