package rim.util.seabass;

import rim.compress.CompressException;

/**
 * SeabassCompress例外.
 */
public class SeabassCompressException extends CompressException {
	private static final long serialVersionUID = -6021604387945191923L;

	public SeabassCompressException() {
		super(500);
	}

	public SeabassCompressException(String message) {
		super(500, message);
	}

	public SeabassCompressException(Throwable e) {
		super(500, e);
	}

	public SeabassCompressException(String message, Throwable e) {
		super(500, message, e);
	}
}
