package rim;

/**
 * rim定義.
 */
public class RimConstants {
	private RimConstants() {}

	/**
	 * バージョン.
	 */
	public static final String VERSION = "0.0.1";

	/**
	 * Rimファイルシンボル.
	 */
	public static final String SIMBOL = "@rim";
	
	/**
	 * Rimファイルシンボルバイナリ.
	 */
	public static final byte[] SIMBOL_BINARY = new byte[] {
		(byte)SIMBOL.charAt(0)
		,(byte)SIMBOL.charAt(1)
		,(byte)SIMBOL.charAt(2)
		,(byte)SIMBOL.charAt(3)
	};

	/**
	 * CSVファイル読み込みデフォルト文字コード.
	 */
	//public static final String DEFAULT_CSV_CHARSET = "Windows-31J";
	public static final String DEFAULT_CSV_CHARSET = "UTF8";

	/**
	 * CSVファイル区切り文字.
	 */
	public static final String DEFAULT_CSV_SEPARATION = ",";
	
	/**
	 * デフォルトのパースするNgram長.
	 */
	public static final int DEFAULT_NGRAM_LENGTH = 2;
	
	/**
	 * 最小のパースするNgram長.
	 */
	public static final int MIN_NGRAM_LENGTH = 1;
	
	/**
	 * 最大のパースするNgram長.
	 */
	public static final int MAX_NGRAM_LENGTH = 3;
	
}