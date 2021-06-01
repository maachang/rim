package rim.util.seabass;

import rim.exception.CoreException;

/**
 * SeabassComp例外.
 */
public class SeabassCompException extends CoreException {
	private static final long serialVersionUID = -6021604387945191923L;

	public SeabassCompException() {
		super(500);
	}

	public SeabassCompException(String message) {
		super(500, message);
	}

	public SeabassCompException(Throwable e) {
		super(500, e);
	}

	public SeabassCompException(String message, Throwable e) {
		super(500, message, e);
	}
}
