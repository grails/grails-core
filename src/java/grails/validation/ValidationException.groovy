package grails.validation

import org.codehaus.groovy.grails.exceptions.GrailsException
import org.springframework.validation.Errors

/**
 * An exception thrown when validation fails during a .save()
 *
 * @author Jeff Brown
 * @since 1.2
 */
class ValidationException extends GrailsException {

    Errors errors

    public ValidationException(String msg, Errors e) {
        super(formatErrors(e, msg))
        this.errors = e
    }

    public ValidationException(Errors e) {
        super(formatErrors(e))
        this.errors = e
    }

    static String formatErrors(Errors errors, String msg = null) {
       StringBuilder b = new StringBuilder(msg ? """$msg:
""" : '')
        
       for(error in errors.allErrors) {
          b.append("""\
- ${error}
""")
       }
       return b.toString()
    }
}