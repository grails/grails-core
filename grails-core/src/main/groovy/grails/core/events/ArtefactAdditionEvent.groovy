package grails.core.events

import groovy.transform.CompileStatic
import grails.core.GrailsClass
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
