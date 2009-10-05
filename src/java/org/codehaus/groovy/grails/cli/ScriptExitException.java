package org.codehaus.groovy.grails.cli;

/**
 * Exception thrown when a script exits, but Grails has disabled the
 * system exit. Used for testing and Grails interactive mode.
 */
public class ScriptExitException extends RuntimeException {
    private int exitCode;

    public ScriptExitException(int exitCode) { 
        this.exitCode = exitCode;
    }

    public ScriptExitException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ScriptExitException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public ScriptExitException(int exitCode, Throwable cause) {
        super(cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() { return exitCode; }
}

