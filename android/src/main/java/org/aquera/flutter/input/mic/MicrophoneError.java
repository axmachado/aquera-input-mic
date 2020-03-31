package org.aquera.flutter.input.mic;

public class MicrophoneError extends Exception {
    public MicrophoneError() {
    }

    public MicrophoneError(String message) {
        super(message);
    }

    public MicrophoneError(String message, Throwable cause) {
        super(message, cause);
    }

    public MicrophoneError(Throwable cause) {
        super(cause);
    }

    public MicrophoneError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
