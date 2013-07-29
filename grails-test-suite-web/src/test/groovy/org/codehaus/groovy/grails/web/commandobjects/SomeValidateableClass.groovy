package org.codehaus.groovy.grails.web.commandobjects

import grails.validation.Validateable

@Validateable
class SomeValidateableClass {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}
