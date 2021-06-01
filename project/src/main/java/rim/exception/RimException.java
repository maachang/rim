package rim.exception;

public class RimException extends CoreException {
	private static final long serialVersionUID = 2224608221542443412L;

	public RimException(int status) {
		super(status);
	}

	public RimException(int status, String message) {
		super(status, message);
	}

	public RimException(int status, Throwable e) {
		super(status, e);
	}

	public RimException(int status, String message, Throwable e) {
		super(status, message, e);
	}

	public RimException() {
		super();
	}

	public RimException(String m) {
		super(m);
	}

	public RimException(Throwable e) {
		super(e);
	}

	public RimException(String m, Throwable e) {
		super(m, e);
	}


}
