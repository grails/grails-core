package org.codehaus.groovy.grails.plugins.exceptions;

public class PluginException extends RuntimeException {

	public PluginException() {
		super();
	}

	public PluginException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public PluginException(String arg0) {
		super(arg0);
	}

	public PluginException(Throwable arg0) {
		super(arg0);
	}

}
