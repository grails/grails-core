package org.grails.domain.compiler

import grails.artefact.DomainClass

import org.grails.compiler.injection.TraitInjector
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.io.support.GrailsResourceUtils

class DomainClassTraitInjector implements TraitInjector {

    Class getTrait() {
        DomainClass
    }

    @Override
    boolean shouldInject(URL url) {
        GrailsResourceUtils.isDomainClass(url)
    }

    @Override
    String[] getArtefactTypes() {
        [DomainClassArtefactHandler.TYPE]
    }

}
