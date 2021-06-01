package rim.util;

import rim.exception.CoreException;

/**
 * DateException.
 */
public class DateException extends CoreException {
	private static final long serialVersionUID = 6781105083531790385L;
	public DateException(int status) {
		super(status);
	}

	public DateException(int status, String message) {
		super(status, message);
	}

	public DateException(int status, Throwable e) {
		super(status, e);
	}

	public DateException(int status, String message, Throwable e) {
		super(status, message, e);
	}

	public DateException() {
		super();
	}

	public DateException(String m) {
		super(m);
	}

	public DateException(Throwable e) {
		super(e);
	}

	public DateException(String m, Throwable e) {
		super(m, e);
	}
}
