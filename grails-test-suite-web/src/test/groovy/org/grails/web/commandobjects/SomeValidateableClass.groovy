package org.grails.web.commandobjects

import grails.validation.Validateable

class SomeValidateableClass implements Validateable {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}
