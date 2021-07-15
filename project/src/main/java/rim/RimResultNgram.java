package rim;

/**
 * Ngram検索結果の情報.
 */
public interface RimResultNgram extends RimResult {
	
	/**
	 * 検索文字位置を取得.
	 * @return int 検索文字位置が返却されます.
	 */
	public int getPosition();
	
	/**
	 * 同一行情報の再取得が行われる場合 true.
	 * たとえば "abcabcd" と言う文字に対して "abc"で検索した
	 * 場合、１つの文字に２つの検索条件が含まれているので、
	 * この場合において、同一行が返却させたい設定の場合は
	 * true が返却されます.
	 * @return boolean trueの場合、同一行の再取得が行われます.
	 */
	public boolean isAcquiredLine();
}
