package rim.compress;

import rim.exception.CoreException;

public class CompressException extends CoreException {
	private static final long serialVersionUID = -3685741810169492134L;

	public CompressException(int status) {
		super(status);
	}

	public CompressException(int status, String message) {
		super(status, message);
	}

	public CompressException(int status, Throwable e) {
		super(status, e);
	}

	public CompressException(int status, String message, Throwable e) {
		super(status, message, e);
	}

	public CompressException() {
		super();
	}

	public CompressException(String m) {
		super(m);
	}

	public CompressException(Throwable e) {
		super(e);
	}

	public CompressException(String m, Throwable e) {
		super(m, e);
	}


}
