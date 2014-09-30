package org.grails.web.commandobjects

import grails.validation.Validateable

@Validateable
class SomeValidateableClass {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}

@Validateable
class OtherValidateableClass {
    String myProperty

    long getMyPropertyAsLong() {
        this.myProperty as Long
    }

    static constraints = {
        myProperty validate: { val ->
            // check if val is an actual long, otherwise return error
            try {
                val as Long
                true // no error here
            } catch (NumberFormatException e) {
                'error'
            }
        }
    }
}
