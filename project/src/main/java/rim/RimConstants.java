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
	 * デフォルトの文字列の長さを表すバイト数.
	 */
	public static final int DEFAULT_STRING_HEADER_LENGTH = 2;

	/**
	 * CSVファイル読み込みデフォルト文字コード.
	 */
	public static final String DEFAULT_CSV_CHARSET = "Windows-31J";

	/**
	 * CSVファイル区切り文字.
	 */
	public static final String DEFAULT_CSV_SEPARATION = ",";
}