package grails.validation

import org.codehaus.groovy.grails.exceptions.GrailsException

/**
 * An exception thrown when validation fails during a .save()
 *
 * @author Jeff Brown
 * @since 1.2
 */
class ValidationException extends GrailsException {

    public ValidationException(String msg) {
        super(msg)
    }

}