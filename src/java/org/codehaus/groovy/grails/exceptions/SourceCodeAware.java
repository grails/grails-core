package org.codehaus.groovy.grails.exceptions;

import java.io.Serializable;

/**
 * An interface that represents an exception that is capable of providing more information about the source code
 * 
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Nov 15, 2007
 */
public interface SourceCodeAware extends Serializable {
    String getFileName();

    int getLineNumber();
}
