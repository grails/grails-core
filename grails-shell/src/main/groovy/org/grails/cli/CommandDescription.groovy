package org.grails.cli

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CommandDescription {
    String name
    String description
    String usage
}
