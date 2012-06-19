package org.codehaus.groovy.grails.plugins.exceptions;

public class PluginException extends RuntimeException {

    private static final long serialVersionUID = -3041972956196552302L;

    public PluginException() {
        super();
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginException(String message) {
        super(message);
    }

    public PluginException(Throwable cause) {
        super(cause);
    }
}
