package org.grails.web.servlet.mvc.alpha

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
