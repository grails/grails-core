package org.grails.plugins.web.async

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import grails.async.decorator.PromiseDecorator

/**
 * A promise decorated lookup strategy that binds a WebRequest to the promise thread
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class WebRequestPromiseDecorator implements PromiseDecorator {
    GrailsWebRequest webRequest

    WebRequestPromiseDecorator(GrailsWebRequest webRequest) {
        this.webRequest = webRequest
    }

    @Override
    def <D> Closure<D> decorate(Closure<D> c) {
        return (Closure<D>) {  args ->
            def newWebRequest = new GrailsWebRequest(webRequest.currentRequest, webRequest.currentResponse, webRequest.servletContext,webRequest.applicationContext)
            newWebRequest.addParametersFrom(webRequest.params)
            WebUtils.storeGrailsWebRequest(newWebRequest)
            try {
                return invokeClosure(c, args)
            }
            finally {
                newWebRequest.requestCompleted()
                WebUtils.storeGrailsWebRequest(webRequest)

            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def invokeClosure(Closure c, args) {
        if (args == null) {
            c.call(null)
        }
        else if(args && args.getClass().isArray()) {
            c.call(*args)
        }
        else if (args instanceof List) {
            c.call(*args)
        }
        else {
            c.call(args)
        }
    }
}
