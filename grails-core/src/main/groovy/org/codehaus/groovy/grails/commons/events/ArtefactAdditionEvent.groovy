package org.codehaus.groovy.grails.commons.events

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.context.ApplicationEvent

/**
 * An event triggered for the addition of a new artefact
 */
@CompileStatic
class ArtefactAdditionEvent extends ApplicationEvent{
    /**
     * Create a new ApplicationEvent.
     * @param source the component that published the event (never {@code null})
     */
    ArtefactAdditionEvent(GrailsClass artefact) {
        super(artefact)
    }

    GrailsClass getArtefact() {
        (GrailsClass)source
    }
}
