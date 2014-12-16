package grails.async.web

import groovy.transform.CompileStatic
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.util.Assert
import org.springframework.web.context.request.async.AsyncWebRequest

import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of Spring 4.0 {@link AsyncWebRequest} interface for Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class AsyncGrailsWebRequest extends GrailsWebRequest implements AsyncWebRequest, AsyncListener{

    Long timeout
    AsyncContext asyncContext

    private AtomicBoolean asyncCompleted = new AtomicBoolean(false)

    List<Runnable> timeoutHandlers = []
    List<Runnable> completionHandlers = []


    AsyncGrailsWebRequest(HttpServletRequest request, HttpServletResponse response, GrailsApplicationAttributes attributes) {
        super(request, response, attributes)
    }

    AsyncGrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        super(request, response, servletContext)
    }

    AsyncGrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ApplicationContext applicationContext) {
        super(request, response, servletContext, applicationContext)
    }

    @Override
    void addTimeoutHandler(Runnable runnable) {
        timeoutHandlers << runnable
    }

    @Override
    void addCompletionHandler(Runnable runnable) {
        completionHandlers << runnable
    }

    @Override
    void startAsync() {
        Assert.state(request.asyncSupported, "The current request does not support Async processing")
        if(!isAsyncStarted()) {
            asyncContext = request.startAsync(request, response)
            asyncContext.addListener(this)
        }
    }

    @Override
    boolean isAsyncStarted() {
        asyncContext && request.asyncStarted
    }

    @Override
    void dispatch() {
       Assert.notNull this.asyncContext, "Cannot dispatch without an AsyncContext"
       asyncContext.dispatch()
    }

    @Override
    boolean isAsyncComplete() {
        return this.asyncCompleted.get()
    }

    @Override
    void onComplete(AsyncEvent event) throws IOException {
        for(handler in completionHandlers) {
            handler.run()
        }
        asyncContext = null
        asyncCompleted.set true
    }

    @Override
    void onTimeout(AsyncEvent event) throws IOException {
        for(handler in timeoutHandlers) {
            handler.run()
        }
    }

    @Override
    void onError(AsyncEvent event) throws IOException {}

    @Override
    void onStartAsync(AsyncEvent event) throws IOException {}
}
