package org.grails.plugins.web.async

import grails.async.web.AsyncGrailsWebRequest
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import grails.async.decorator.PromiseDecorator
import org.springframework.web.context.request.async.WebAsyncManager
import org.springframework.web.context.request.async.WebAsyncUtils

import javax.servlet.http.HttpServletRequest
import java.util.concurrent.TimeoutException

/**
 * A promise decorated lookup strategy that binds a WebRequest to the promise thread
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AsyncWebRequestPromiseDecorator implements PromiseDecorator {
    GrailsWebRequest webRequest
    final AsyncGrailsWebRequest asyncRequest
    volatile boolean timeoutReached = false

    AsyncWebRequestPromiseDecorator(GrailsWebRequest webRequest) {
        this.webRequest = webRequest
        HttpServletRequest currentServletRequest = webRequest.currentRequest
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(currentServletRequest)
        AsyncGrailsWebRequest newWebRequest
        if(asyncManager.isConcurrentHandlingStarted()) {
            newWebRequest = AsyncGrailsWebRequest.lookup(currentServletRequest)
            if( newWebRequest == null || newWebRequest.isAsyncComplete() ) {
                throw new IllegalStateException("Cannot start a task once asynchronous request processing has completed")
            }
        }
        else {
            newWebRequest = new AsyncGrailsWebRequest(currentServletRequest, webRequest.currentResponse, webRequest.servletContext,webRequest.applicationContext)
            newWebRequest.addParametersFrom(webRequest.params)
            asyncManager.setAsyncWebRequest(newWebRequest)
            newWebRequest.startAsync()
        }
        newWebRequest.addTimeoutHandler({
            timeoutReached = true
        })
        asyncRequest = newWebRequest
    }

    @Override
    def <D> Closure<D> decorate(Closure<D> c) {
        return (Closure<D>) {  args ->
            if(timeoutReached) {
                throw new TimeoutException("Asynchronous request processing timeout reached")
            }
            WebUtils.storeGrailsWebRequest(asyncRequest)
            try {
                return invokeClosure(c, args)
            }
            finally {
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
