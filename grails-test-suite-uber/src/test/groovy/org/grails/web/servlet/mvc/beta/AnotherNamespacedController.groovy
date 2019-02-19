package org.grails.web.servlet.mvc.beta

class AnotherNamespacedController {

    static namespace = 'secondary'

    def demo() {
        render 'Rendered by the secondary AnotherNamespaced Controller'
    }
}
