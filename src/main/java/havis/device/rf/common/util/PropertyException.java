package havis.device.rf.common.util;

public class PropertyException extends Exception {

	private static final long serialVersionUID = 1L;

	public PropertyException() {
		super();
	}

	public PropertyException(String message) {
		super(message);
	}

	PropertyException(String message, Throwable cause) {
		super(message, cause);
	}
}
