package org.grails.web.commandobjects

import grails.validation.trait.Validateable

class SomeValidateableClass implements Validateable {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}
