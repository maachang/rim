package rim.exception;

/**
 * 基底例外.
 */
public class CoreException extends RuntimeException {
	private static final long serialVersionUID = 3500964215730865406L;
	protected int status;
	protected String msg;

	public CoreException(int status) {
		super();
		this.status = status;
	}

	public CoreException(int status, String message) {
		super(message);
		this.status = status;
	}

	public CoreException(int status, Throwable e) {
		super(_getMessage(null, e), e);
		this.status = status;
	}

	public CoreException(int status, String message, Throwable e) {
		super(_getMessage(message, e), e);
		this.status = status;
	}

	public CoreException() {
		this(500);
	}

	public CoreException(String m) {
		this(500, m);
	}

	public CoreException(Throwable e) {
		this(_getStatus(e), _getMessage(null, e), e);
	}

	public CoreException(String m, Throwable e) {
		this(_getStatus(e), _getMessage(m, e), e);
	}

	public int getStatus() {
		return status;
	}

	public void setMessage(String msg) {
		this.msg = msg;
	}

	public String getMessage() {
		return msg == null ? super.getMessage() : msg;
	}

	public String getLocalizedMessage() {
		return msg == null ? super.getLocalizedMessage() : msg;
	}

	private static final int _getStatus(Throwable e) {
		if(e instanceof CoreException) {
			return ((CoreException)e).getStatus();
		}
		return 500;
	}

	private static final String _getMessage(String msg, Throwable e) {
		if(msg != null) {
			return msg;
		}
		return e.getMessage();
	}
}

