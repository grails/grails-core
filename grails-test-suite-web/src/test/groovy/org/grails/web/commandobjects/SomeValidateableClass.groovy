package org.grails.web.commandobjects

import grails.artefact.Validateable

class SomeValidateableClass implements Validateable {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}
