package org.codehaus.groovy.grails.validation.exceptions;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class ConstraintVetoingException extends ConstraintException {
    public ConstraintVetoingException() {
    }

    public ConstraintVetoingException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ConstraintVetoingException(String arg0) {
        super(arg0);
    }

    public ConstraintVetoingException(Throwable arg0) {
        super(arg0);
    }
}
