package org.codehaus.groovy.grails.web.servlet.view;

import java.lang.reflect.UndeclaredThrowableException;

import org.springframework.web.servlet.view.AbstractUrlBasedView;

public abstract class AbstractGrailsView extends AbstractUrlBasedView {
    public void rethrowRenderException(Throwable ex, String message) {
        if (ex instanceof Error) {
            throw (Error) ex;
        }        
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex, message);
    }
}
