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
    String fullMessage

    public ValidationException(String msg, Errors e) {
        super(msg)
        this.errors = e
        this.fullMessage = formatErrors(e, msg)
    }

    public String getMessage() {
        return fullMessage
    }

    public static String formatErrors(Errors errors, String msg = null) {
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