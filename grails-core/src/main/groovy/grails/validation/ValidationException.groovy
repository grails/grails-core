/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.validation

import org.codehaus.groovy.grails.exceptions.GrailsException
import org.springframework.validation.Errors

/**
 * Thrown when validation fails during a .save().
 *
 * @author Jeff Brown
 * @since 1.2
 */
class ValidationException extends GrailsException {

    Errors errors
    String fullMessage

    ValidationException(String msg, Errors e) {
        super(msg)
        errors = e
        fullMessage = formatErrors(e, msg)
    }

    String getMessage() { fullMessage }

    static String formatErrors(Errors errors, String msg = null) {
        StringBuilder b = new StringBuilder(msg ? """$msg:
""" : '')

        for (error in errors.allErrors) {
            b.append("""\
- ${error}
""")
        }
        return b.toString()
    }
}
