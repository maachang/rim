package rim;

/**
 * Rim-Util.
 */
public class RimUtil {
	private RimUtil() {}

	/**
	 * 長さからその内容に収まるバイト数を取得.
	 * @param len 長さを設定します.
	 * @return 長さの範囲に収まるバイト数が返却されます.
	 */
	public static final int intLengthByByte1_4Length(int len) {
		if(len > 0x00ffffff) {
			return 4;
		} else if(len > 0x0000ffff) {
			return 3;
		} else if(len > 0x000000ff) {
			return 2;
		}
		return 1;
	}

}
