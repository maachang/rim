package rim.util.seabass;

import rim.exception.CoreException;

/**
 * SeabassCipher例外.
 */
public class SeabassCipherException extends CoreException {
	private static final long serialVersionUID = -7995238015802434505L;

	public SeabassCipherException() {
		super(500);
	}

	public SeabassCipherException(String message) {
		super(500, message);
	}

	public SeabassCipherException(Throwable e) {
		super(500, e);
	}

	public SeabassCipherException(String message, Throwable e) {
		super(500, message, e);
	}
}
