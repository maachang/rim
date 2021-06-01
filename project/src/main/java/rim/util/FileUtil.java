package rim.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * ファイルユーティリティ.
 */
public final class FileUtil {
	protected FileUtil() {
	}

	// ファイル及びディレクトリの削除.
	private static final void _deleteFileOrDirectory(String name) throws Exception {
		Files.delete(Paths.get(name));
	}

	// ファイル時間を取得.
	private static final long _getFileTime(int type, String name) throws Exception {
		File fp = new File(name);
		if (fp.exists()) {
			BasicFileAttributes attrs = Files.readAttributes(
				Paths.get(name), BasicFileAttributes.class);
			switch (type) {
			// ファイル作成時間.
			case 0:
				return attrs.creationTime().toMillis();
			// ファイル最終アクセス時間.
			case 1:
				return attrs.lastAccessTime().toMillis();
			// ファイル最終更新時間.
			case 2:
				return attrs.lastModifiedTime().toMillis();
			}
		}
		return -1;
	}

	/**
	 * ファイル名の存在チェック.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return boolean [true]の場合、ファイルは存在します.
	 */
	public static final boolean isFile(String name) {
		File file = new File(name);
		return (file.exists() && !file.isDirectory());
	}

	/**
	 * ディレクトリ名の存在チェック.
	 *
	 * @param name
	 *            対象のディレクトリ名を設定します.
	 * @return boolean [true]の場合、ディレクトリは存在します.
	 */
	public static final boolean isDir(String name) {
		File file = new File(name);
		return (file.exists() && file.isDirectory());
	}

	/**
	 * 指定情報が読み込み可能かチェック.
	 *
	 * @param name
	 *            対象のファイル／ディレクトリ名を設定します.
	 * @return boolean [true]の場合、読み込み可能です.
	 */
	public static final boolean isRead(String name) {
		File file = new File(name);
		return (file.exists() && file.canRead());
	}

	/**
	 * 指定情報が書き込み可能かチェック.
	 *
	 * @param name
	 *            対象のファイル／ディレクトリ名を設定します.
	 * @return boolean [true]の場合、書き込み可能です.
	 */
	public static final boolean isWrite(String name) {
		File file = new File(name);
		return (file.exists() && file.canWrite());
	}

	/**
	 * 指定情報が読み書き込み可能かチェック.
	 *
	 * @param name
	 *            対象のファイル／ディレクトリ名を設定します.
	 * @return boolean [true]の場合、読み書き込み可能です.
	 */
	public static final boolean isIO(String name) {
		File file = new File(name);
		return (file.exists() && file.canRead() && file.canWrite());
	}

	/**
	 * 対象のディレクトリを生成.
	 *
	 * @param dirName
	 *            生成対象のディレクトリ名を設定します.
	 * @exception Exception
	 *                例外.
	 */
	public static final void mkdirs(String dir) throws Exception {
		File fp = new File(dir);
		if (!fp.mkdirs()) {
			throw new IOException("Failed to create directory (" + dir + ").");
		}
	}

	/**
	 * ファイルの長さを取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return long ファイルの長さが返却されます. [-1L]が返却された場合、ファイルは存在しません.
	 * @exception Exception
	 *                例外.
	 */
	public static final long getFileLength(String name) throws Exception {
		File fp = new File(name);
		return (fp.exists()) ? fp.length() : -1L;
	}

	/**
	 * ファイル生成時間を取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return long ファイルタイムが返却されます. [-1L]が返却された場合、ファイルは存在しません.
	 * @exception Exception
	 *                例外.
	 */
	public static final long birthtime(String name) throws Exception {
		return _getFileTime(0, name);
	}

	/**
	 * 最終アクセス時間を取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return long ファイルタイムが返却されます. [-1L]が返却された場合、ファイルは存在しません.
	 * @exception Exception
	 *                例外.
	 */
	public static final long atime(String name) throws Exception {
		return _getFileTime(1, name);
	}

	/**
	 * 最終更新時間を取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return long ファイルタイムが返却されます. [-1L]が返却された場合、ファイルは存在しません.
	 * @exception Exception
	 *                例外.
	 */
	public static final long mtime(String name) throws Exception {
		return _getFileTime(2, name);
	}

	/**
	 * ファイル名のフルパスを取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return String フルパス名が返却されます.
	 * @exception Exception
	 *                例外.
	 */
	public static final String getFullPath(String name) throws Exception {
		char c;
		name = new File(name).getCanonicalPath();
		final int len = name.length();
		StringBuilder buf = new StringBuilder(len + 2);
		if(!name.startsWith("/")) {
			buf.append("/");
		} else if(name.indexOf("\\") == -1) {
			return name;
		}
		for(int i = 0; i < len; i++) {
			c = name.charAt(i);
			if(c == '\\') {
				buf.append("/");
			} else {
				buf.append(c);
			}
		}
		return buf.toString();
	}

	/**
	 * 対象パスのファイル名のみ取得.
	 *
	 * @param path
	 *            対象のパスを設定します.
	 * @return String ファイル名が返却されます.
	 */
	public static final String getFileName(String path) {
		int p = path.lastIndexOf("/");
		if (p == -1) {
			p = path.lastIndexOf("\\");
		}
		if (p == -1) {
			return path;
		}
		return path.substring(p + 1);
	}

	/**
	 * 対象パスのディレクトリ名のみ取得.
	 *
	 * @param name
	 * @return
	 */
	public static final String getDirectoryName(String name) {
		String f = FileUtil.getFileName(name);
		return name.substring(0, name.length() - f.length());
	}

	/**
	 * ファイル内容を取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @return byte[] バイナリ情報が返却されます.
	 * @exception Exception
	 *                例外.
	 */
	public static final byte[] getFile(String name) throws Exception {
		int len;
		InputStream buf = null;
		ByteArrayOutputStream bo = null;
		byte[] b = new byte[1024];
		try {
			bo = new ByteArrayOutputStream();
			buf = new BufferedInputStream(new FileInputStream(name));
			while (true) {
				if ((len = buf.read(b)) <= 0) {
					if (len <= -1) {
						break;
					}
					continue;
				}
				bo.write(b, 0, len);
			}
			buf.close();
			buf = null;
			byte[] ret = bo.toByteArray();

			bo.close();
			bo = null;

			return ret;
		} finally {
			if (buf != null) {
				try {
					buf.close();
				} catch (Exception t) {
				}
			}
			buf = null;
			if (bo != null) {
				bo.close();
			}
		}
	}

	/**
	 * ファイル内容を取得.
	 *
	 * @param name
	 *            対象のファイル名を設定します.
	 * @param charset
	 *            対象のキャラクタセットを設定します.
	 * @return String 文字列情報が返却されます.
	 * @exception Exception
	 *                例外.
	 */
	public static final String getFileString(String name, String charset) throws Exception {
		int len;
		char[] tmp = new char[1024];
		CharArrayWriter ca = null;
		Reader buf = null;
		String ret = null;
		try {
			ca = new CharArrayWriter();
			buf = new BufferedReader(new InputStreamReader(new FileInputStream(name), charset));
			while ((len = buf.read(tmp, 0, 512)) > 0) {
				ca.write(tmp, 0, len);
			}
			ret = ca.toString();
			ca.close();
			ca = null;
			buf.close();
			buf = null;
		} finally {
			if (buf != null) {
				try {
					buf.close();
				} catch (Exception t) {
				}
			}
			if (ca != null) {
				try {
					ca.close();
				} catch (Exception t) {
				}
			}
			buf = null;
			ca = null;
			tmp = null;
		}
		return ret;
	}

	/**
	 * バイナリをファイル出力.
	 *
	 * @param newFile
	 *            [true]の場合、新規でファイル出力します.
	 * @param name
	 *            ファイル名を設定します.
	 * @param binary
	 *            出力対象のバイナリを設定します.
	 * @exception Exception
	 *                例外.
	 */
	public static final void setFile(boolean newFile, String name, byte[] binary) throws Exception {
		if (binary == null) {
			throw new IOException("There is no binary to output.");
		}
		BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(name, !newFile));
		try {
			buf.write(binary);
			buf.flush();
			buf.close();
			buf = null;
		} finally {
			if (buf != null) {
				try {
					buf.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 文字情報をファイル出力.
	 *
	 * @param newFile
	 *            [true]の場合、新規でファイル出力します.
	 * @param name
	 *            ファイル名を設定します.
	 * @param value
	 *            出力対象の文字列を設定します.
	 * @param charset
	 *            対象のキャラクタセットを設定します. nullの場合は、UTF8が設定されます.
	 * @exception Exception
	 *                例外.
	 */
	public static final void setFileString(boolean newFile, String name, String value, String charset)
			throws Exception {
		if (value == null) {
			throw new IOException("There is no target string information for output.");
		}
		BufferedWriter buf = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(name, !newFile), (charset == null) ? "UTF8" : charset));
		try {
			buf.write(value, 0, value.length());
			buf.flush();
			buf.close();
			buf = null;
		} finally {
			if (buf != null) {
				try {
					buf.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 指定ファイルorフォルダを削除.
	 *
	 * @param name
	 *            対象のファイルorフォルダ名を設定します.
	 * @return boolean 削除結果が返されます.
	 * @exception 例外.
	 */
	public static final void removeFile(String name) throws Exception {
		_deleteFileOrDirectory(name);
	}

	// 指定内のフォルダが空でない場合は中身も削除して削除処理.
	private static final void _delete(String name) throws Exception {
		if (isFile(name)) {
			_deleteFileOrDirectory(name);
		} else {
			String[] list = list(name);
			if (list != null && list.length > 0) {
				if (!name.endsWith("/")) {
					name = name + "/";
				}
				String path;
				int len = list.length;
				for (int i = 0; i < len; i++) {
					path = name + list[i];
					_delete(path);
					if (isDir(path)) {
						_deleteFileOrDirectory(path);
					}
				}
			}
		}
	}

	/**
	 * 指定フォルダ内のファイルとフォルダを全削除.
	 *
	 * @param name
	 *            削除対象のフォルダ名かファイル名を設定します.
	 * @throws Exception
	 *             例外.
	 */
	public static final void delete(String name) throws Exception {
		_delete(getFullPath(name));
	}

	/**
	 * ファイルリストを取得.
	 *
	 * @param name
	 *            対象のフォルダ名を設定します.
	 * @return String[] ファイルリストが返却されます.
	 * @exception Exception
	 *                例外.
	 */
	public static final String[] list(String name) throws Exception {
		File fp = new File(name);
		return (fp.exists()) ? fp.list() : new String[0];
	}

	/**
	 * ファイル、フォルダの移動.
	 *
	 * @param src
	 *            移動元のファイル名を設定します.
	 * @param dest
	 *            移動先のファイル名を設定します.
	 * @throws Exception
	 *             例外.
	 */
	public static final void move(String src, String dest) throws Exception {
		Files.move(Paths.get(src), Paths.get(dest));
	}

	// ファイルのコピー.
	private static final void _copyFile(String src, String dest) throws Exception {
		Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.COPY_ATTRIBUTES,
				StandardCopyOption.REPLACE_EXISTING);
	}

	// コピー処理.
	public static final void _copy(String src, String dest) throws Exception {
		if (isFile(src)) {
			_copyFile(src, dest);
		} else {
			String[] list = list(src);
			if (list != null && list.length > 0) {
				if (!src.endsWith("/")) {
					src = src + "/";
				}
				if (!dest.endsWith("/")) {
					dest = dest + "/";
				}
				int len = list.length;
				for (int i = 0; i < len; i++) {
					if (isDir(src)) {
						if (!isDir(dest + list[i])) {
							mkdirs(dest + list[i]);
						}
					}
					_copy(src + list[i], dest + list[i]);
				}
			}
		}
	}

	/**
	 * ファイル・フォルダのコピー処理.
	 *
	 * @param src
	 *            コピー元のファイル、フォルダを設定します.
	 * @param dest
	 *            コピー先のファイル、フォルダを設定します.
	 * @throws Exception
	 *             例外.
	 */
	public static final void copy(String src, String dest) throws Exception {
		_copy(getFullPath(src), getFullPath(dest));
	}

	/**
	 * リソースファイルを、対象ファイルにコピーする.
	 * @param src リソースファイルパスを設定します.
	 * @param dest コピー先のファイル名を設定します.
	 * @throws Exception
	 */
	public static final void rcpy(String src, String dest) throws Exception {
		int len;
		byte[] bin = new byte[4096];
		OutputStream out = null;
		InputStream in = null;
		try {
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream(src);
			out = new FileOutputStream(dest);
			while ((len = in.read(bin)) != -1) {
				out.write(bin, 0, len);
			}
			in.close();
			in = null;
			out.close();
			out = null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
				try {
					out.close();
				} catch (Exception e) {
				}
			}
		}
	}
}
