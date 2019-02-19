package org.grails.web.servlet.mvc.alpha

import grails.artefact.Artefact

@Artefact('Controller')
class NamespacedController {

    def redirectToSelf() {
        // redirects to this controller
        redirect action: 'demo'
    }

    def redirectToSecondary() {
        // redirects to controller in the secondary namespace
        redirect controller: 'namespaced', action: 'demo', namespace: 'secondary'
    }

    def demo() {
        render 'Rendered by the primary Namespaced Controller'
    }
}
