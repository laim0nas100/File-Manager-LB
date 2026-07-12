package lt.lb.filemanagerlb.utility;

/**
 *
 * @author laim0nas100
 */
public class FileNameException extends Exception {

    public FileNameException() {
    }

    public FileNameException(String message) {
        super(message);
    }

    public FileNameException(Throwable cause) {
        super(cause);
    }

    public FileNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNameException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
