package org.codehaus.groovy.grails.commons.metaclass;

/**
 * <p>Interface for code that returns a value based on an input object.</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public interface FunctionCallback {

    Object execute(Object object);
}
