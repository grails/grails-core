package org.grails.web.servlet.mvc.beta

import grails.artefact.Artefact

@Artefact('Controller')
class NamespacedController {
    static namespace = 'secondary'

    def redirectToSelfWithImplicitNamespace() {
        // redirects to this controller
        redirect action: 'demo'
    }

    def redirectToSelfWithExplicitNamespace() {
        // redirects to this controller
        redirect action: 'demo', namespace: 'secondary'
    }

    def redirectToPrimary() {
        // redirects to controller with no namespace
        redirect controller: 'namespaced', action: 'demo', namespace: null
    }

    def redirectToAnotherPrimary() {
        // redirects to controller with no namespace
        redirect controller: 'anotherNamespaced', namespace: null, action: 'demo'
    }

    def redirectToAnotherSecondaryWithImplicitNamespace() {
        // redirects to controller with secondary namespace
        redirect controller: 'anotherNamespaced', action: 'demo'
    }

    def redirectToAnotherSecondaryWithExplicitNamespace() {
        // redirects to controller with secondary namespace
        redirect controller: 'anotherNamespaced', namespace: 'secondary', action: 'demo'
    }

    def demo() {
        render 'Rendered by the secondary Namespaced Controller'
    }
}
