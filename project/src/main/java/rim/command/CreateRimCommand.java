package rim.command;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import rim.RimConstants;
import rim.SaveRim;
import rim.compress.CompressType;
import rim.compress.Lz4Compress;
import rim.compress.ZstdCompress;
import rim.util.Args;
import rim.util.CsvReader;
import rim.util.FileUtil;
import rim.util.ObjectList;

/**
 * 指定したCSVからRimファイルを作成します.
 */
public class CreateRimCommand {
	
	public static final void main(String[] args) throws Exception {
		CreateRimCommand cmd = new CreateRimCommand();
		cmd.execute(args);
	}

	// コマンド引数管理.
	private Args args;

	// ヘルプ情報.
	private final void help() {
		System.out.println("");
		System.out.println("CSVファイルをRim変換します.");
		System.out.println("");
		System.out.println("Usage: crim [options]");
		System.out.println(" options 説明:");

		System.out.println("  -v [--version]");
		System.out.println("     このコマンドのバージョンを表示.");

		System.out.println("  -h [--help]");
		System.out.println("     ヘルプ情報を表示.");

		System.out.println("  -f [--csv] {ファイル名}");
		System.out.println("      ※ この条件は必須です.");
		System.out.println("     変換元のCSVファイル名を設定します.");
		
		System.out.println("  -s [--separation] {区切り文字}");
		System.out.println("     変換元のCSVの区切り文字を設定します.");

		System.out.println("  -o [--output] {ファイル名}");
		System.out.println("      ※ この条件は必須です.");
		System.out.println("     出力先のRim変換されるファイル名を設定します");
		
		System.out.println("  -p [--comp] [--compress] {圧縮タイプ}");
		System.out.println("     圧縮タイプを設定します。設定しない場合は圧縮しません。");
		System.out.println("       default  標準圧縮");
		System.out.println("       gzip     GZIP圧縮");
		
		// LZ4が利用可能な場合はヘルプ表示.
		if(Lz4Compress.getInstance().isSuccessLibrary()) {
			System.out.println(
						   "       lz4      LZ4圧縮");
		}
		// Zstdが利用可能な場合はヘルプ表示.
		if(ZstdCompress.getInstance().isSuccessLibrary()) {
			System.out.println(
						   "       zstd{no} Zstandard圧縮");
			System.out.println(
						   "                noは圧縮レベルで(1-22)まで設定可能です。");
			System.out.println(
						   "                設定しない場合は 3 が指定されます.");
		}

		System.out.println("  -i [--index] {列名} ....");
		System.out.println("     列名を設定して、インデックス対象を設定します.");
		System.out.println("     この内容は以下のように複数指定可能です.");
		System.out.println("      > -i id -i name -i age");
		System.out.println("     この場合 id と name と age がインデックスになります.");
		
		System.out.println("  -g [--geo] {緯度列名,経度列名} ....");
		System.out.println("     緯度列名と経度列名を設定して、Geoインデックス対象を設定します.");
		System.out.println("     この内容は以下のように複数指定可能です.");
		System.out.println("      > -g lat,lon -g abc,def");
		System.out.println("     この場合 lat,lon と abc,def がGeoインデックスになります.");
		System.out.println("     緯度列名と経度列名の区切り文字は[,]で区切ります.");
		
		System.out.println("  -n{level} [--ngram] [--unigram] [--bigram] [--trigram] {列名} ....");
		System.out.println("     列名を設定して、ngramのインデックスを設定します.");
		System.out.println("     この内容は以下のように複数指定可能です.");
		System.out.println("      > -n name -n description");
		System.out.println("     この場合 name と description がNgramインデックスになります.");
		System.out.println("     また level を設定することでNgramの精度と速度が変わります.");
		System.out.println("      [-n1] [--unigram] でユニグラムでインデックスを作成します.");
		System.out.println("      [-n] [-n2] [--ngram] [--bigram] でバイグラムでインデックスを作成します.");
		System.out.println("      [-n3] [--trigram] でトリグラムでインデックスを作成します.");

		System.out.println("  -c [--charset] {文字コード}");
		System.out.println("     CSVファイルの文字コードを設定します");
		System.out.println("     設定しない場合は \"" +
			RimConstants.DEFAULT_CSV_CHARSET + "\" が設定されます");

		System.out.println("");
	}

	// バージョン.
	private final void version() {
		System.out.println(RimConstants.VERSION);
	}

	// エラーメッセージ出力.
	private final void errorOut(String message) {
		System.err.println(message);
		System.err.println("");
		System.exit(1);
	}

	// 処理結果のレポート表示.
	private static final void viewReport(
		long time, int rowAll, String csvFile, String outFileName,
		ObjectList<String> indexList, ObjectList<String[]> geoIndexList,
		ObjectList<Object[]> ngramIndexList, CompressType compressType,
		String charset, String separation, Object option)
		throws Exception {
		time = System.currentTimeMillis() - time;

		System.out.println();
		System.out.println("変換時間: " + time + " msec");
		System.out.println();
		
		long inSize = FileUtil.getFileLength(csvFile);
		long outSize = FileUtil.getFileLength(outFileName);

		System.out.println("入力元csv: " + csvFile);
		System.out.println("fileSize: " + inSize + " byte");

		System.out.println("出力先rim: " + outFileName);
		System.out.println("fileSize: " + outSize + " byte");
		System.out.println("圧縮率: " + 
			(int)((double)outSize / (double)inSize * 100d) + " %");

		System.out.println();

		System.out.println("変換行数: " + rowAll + " row");
		
		System.out.println("登録Index数: " + indexList.size());
		int len = indexList.size();
		for(int i = 0; i < len; i ++) {
			System.out.println("  index(" + (i + 1) + "): " + indexList.get(i));
		}
		
		System.out.println("登録GeoIndex数: " + geoIndexList.size());
		len = geoIndexList.size();
		for(int i = 0; i < len; i ++) {
			System.out.println("  geoIndex(" + (i + 1) + "): " +
				geoIndexList.get(i)[0] + ", " + geoIndexList.get(i)[1]);
		}
		
		System.out.println("登録NgramIndex数: " + ngramIndexList.size());
		len = ngramIndexList.size();
		for(int i = 0; i < len; i ++) {
			System.out.println("  ngramIndex(" + (i + 1) + "): " +
				ngramIndexList.get(i)[0] +
				"(" + ngramLengthByString(ngramIndexList.get(i)[1]) + ")");
		}

		System.out.println();
		
		System.out.println("圧縮タイプ: " + compressType);
		if(CompressType.Zstd == compressType) {
			System.out.println("  圧縮レベル: " + option);
		}
		System.out.println("CSV文字コード: " + charset);
		System.out.println("CSV区切り文字: [" + separation + "]");

		System.out.println();
	}
	
	// ngram長を文字列に変換.
	private static final String ngramLengthByString(final Object length) {
		switch((Integer)length) {
		case 1: return "unigram";
		case 2: return "bigram";
		}
		return "trigram";
	}

	// コマンド実行.
	private final void execute(String[] params)
		throws Exception {
		long time = System.currentTimeMillis();

		args = new Args(params);

		// バージョン表示.
		if(args.isValue("-v", "--version")) {
			version();
			System.exit(0);
			return;
		}

		// ヘルプ表示.
		if(args.isValue("-h", "--help")) {
			help();
			System.exit(0);
			return;
		}
		
		// 圧縮タイプを取得.
		CompressType compressType;
		String compress = args.get("-p", "--comp", "--compress");
		if(compress != null) {
			// 文字列から圧縮タイプを取得.
			compressType = CompressType.get(compress);
		} else {
			compressType = CompressType.None;
		}
		
		// オプション.
		Object option = null;
		
		// LZ4の場合、ライブラリが読み込まれ利用可能かチェック.
		if(CompressType.LZ4 == compressType) {
			if(!Lz4Compress.getInstance().isSuccessLibrary()) {
				errorOut("LZ4のjarが読み込まれてないため圧縮は利用出来ません.");
				return;
			}
		// Zstdの場合、ライブラリが読み込まれ利用可能かチェック.
		} else if(CompressType.Zstd ==compressType) {
			if(!ZstdCompress.getInstance().isSuccessLibrary()) {
				errorOut("Zstdのjarが読み込まれてないため圧縮は利用出来ません.");
				return;
			}
			// zstdの文字指定から圧縮レベルを取得.
			int level = CompressType.getZstdLevel(compress);
			if(level > 0) {
				option = level;
			}
		}

		// CSV文字コードを取得.
		String charset = args.get("-c", "--charset");
		if(charset == null) {
			charset = RimConstants.DEFAULT_CSV_CHARSET;
		}
		
		// CSV区切り文字を取得.
		String separation = args.get("-s", "--separation");
		if(separation == null) {
			separation = RimConstants.DEFAULT_CSV_SEPARATION;
		}

		// 入出力先を取得.
		String csvFile = args.get("-f", "--csv");
		String outFileName = args.get("-o", "--output");
		if(csvFile == null) {
			help();
			errorOut("CSVファイルが設定されていません.");
			return;
		}
		if(outFileName == null) {
			help();
			errorOut("Rim変換出力先のファイルが設定されていません.");
			return;
		}

		// CSVファイルが存在しない場合.
		if(!FileUtil.isFile(csvFile)) {
			errorOut("指定されたCSVファイルは存在しません: " + csvFile);
			return;
		}

		// 出力先のディレクトリ名を取得.
		String outDir = FileUtil.getDirectoryName(outFileName);

		// 出力先ディレクトリが存在するかチェック.
		if(!FileUtil.isDir(outDir) && !outDir.isEmpty()) {
			errorOut("指定された出力先のディレクトリは存在しません: " + outFileName);
			return;
		}

		// インデックス情報を取得.
		ObjectList<String> indexList = new ObjectList<String>();
		for(int i = 0;; i ++) {
			String index = args.next(i, "-i", "--index");
			if(index == null) {
				break;
			}
			indexList.add(index);
		}
		
		// Geoインデックス情報を取得.
		ObjectList<String[]> geoIndexList = new ObjectList<String[]>();
		for(int i = 0;; i ++) {
			String index = args.next(i, "-g", "--geo");
			if(index == null) {
				break;
			}
			int p = index.indexOf(",");
			if(p == -1) {
				errorOut("指定されたGeoインデックスの列名は "+
					"\"緯度,経度\" で設定する必要があります: " +
					index);
				return;
			}
			geoIndexList.add(
				new String[] {
					index.substring(0, p),
					index.substring(p + 1)
				});
		}
		
		// ngramインデックス情報を取得.
		ObjectList<Object[]> ngramIndexList = new ObjectList<Object[]>();
		// unigram.
		for(int i = 0;; i ++) {
			String index = args.next(i, "-n1", "--unigram");
			if(index == null) {
				break;
			}
			ngramIndexList.add(new Object[] {index, 1});
		}
		// bigram.
		for(int i = 0;; i ++) {
			String index = args.next(i, "-n", "-n2", "--ngram", "--bigram");
			if(index == null) {
				break;
			}
			ngramIndexList.add(new Object[] {index, 2});
		}
		// trigram.
		for(int i = 0;; i ++) {
			String index = args.next(i, "-n3", "--trigram");
			if(index == null) {
				break;
			}
			ngramIndexList.add(new Object[] {index, 3});
		}

		// 変換処理.
		CsvReader csv = null;
		SaveRim convRim = null;
		OutputStream out = null;
		try {
			// CSVオープン.
			csv = new CsvReader(csvFile, charset, separation);
			
			// 書き込み先ファイルをオープン.
			out = new BufferedOutputStream(new FileOutputStream(outFileName));

			// 変換オブジェクトを作成.
			convRim = new SaveRim(
				csv, out, compressType, option);

			// インデックスの設定.
			int len = indexList.size();
			for(int i = 0; i < len; i ++) {
				convRim.addIndex(indexList.get(i));
			}
			
			// geoインデックスの設定.
			len = geoIndexList.size();
			for(int i = 0; i < len; i ++) {
				convRim.addGeoIndex(
					geoIndexList.get(i)[0],
					geoIndexList.get(i)[1]);
			}
			
			// ngramインデックスの設定.
			len = ngramIndexList.size();
			for(int i = 0; i < len; i ++) {
				convRim.addNgramIndex(
					(String)ngramIndexList.get(i)[0],
					(Integer)ngramIndexList.get(i)[1]);
			}

			// ファイル出力.
			int rowAll = convRim.write();

			// 後処理.
			csv.close(); csv = null;
			out.close(); out = null;
			convRim.close(); convRim = null;

			// 処理結果のレポートを表示.
			viewReport(time, rowAll, csvFile, outFileName, indexList,
				geoIndexList, ngramIndexList, compressType, charset,
				separation, option);

			System.exit(0);
		} finally {
			if(csv != null) {
				try {
					csv.close();
				} catch(Exception e) {}
			}
			if(out != null) {
				try {
					out.close();
				} catch(Exception e) {}
			}
			if(convRim != null) {
				try {
					convRim.close();
				} catch(Exception e) {}
			}
		}

	}
}