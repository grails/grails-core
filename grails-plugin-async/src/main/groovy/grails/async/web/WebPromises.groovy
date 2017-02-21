package grails.async.web

import grails.async.Promise
import grails.async.PromiseFactory
import grails.async.Promises
import grails.async.decorator.PromiseDecorator
import groovy.transform.CompileStatic
import org.grails.async.factory.SynchronousPromiseFactory
import org.grails.async.factory.gpars.GparsPromiseFactory
import org.grails.plugins.web.async.AsyncWebRequestPromiseDecoratorLookupStrategy
import org.grails.plugins.web.async.WebRequestPromiseDecoratorLookupStrategy

import java.util.concurrent.TimeUnit

/**
 * A specific promises factory class designed for use in controllers and other web contexts
 *
 * @since 3.2.7
 * @author  Graeme Rocher
 */
@CompileStatic
class WebPromises {

    private static final AsyncWebRequestPromiseDecoratorLookupStrategy DECORATOR_LOOKUP = new AsyncWebRequestPromiseDecoratorLookupStrategy()

    static PromiseFactory promiseFactory

    static {
        if (GparsPromiseFactory.isGparsAvailable()) {
            promiseFactory = new GparsPromiseFactory()
        }
        else {
            promiseFactory = new SynchronousPromiseFactory()
        }
    }

    private WebPromises() {
    }
    /**
     * @see grails.async.PromiseFactory#waitAll(grails.async.Promise[])
     */
    static<T> List<T> waitAll(Promise<T>...promises) {
        return promiseFactory.waitAll(promises)
    }

    /**
     * @see grails.async.PromiseFactory#waitAll(java.util.List)
     */
    static<T> List<T> waitAll(List<Promise<T>> promises) {
        return promiseFactory.waitAll(promises)
    }

    /**
     * @see grails.async.PromiseFactory#waitAll(java.util.List)
     */
    static<T> List<T> waitAll(List<Promise<T>> promises, final long timeout, final TimeUnit units) {
        return promiseFactory.waitAll(promises, timeout, units)
    }

    /**
     * @see grails.async.PromiseFactory#onComplete(java.util.List, groovy.lang.Closure)
     */
    static<T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure<?> callable ) {
        return promiseFactory.onComplete(promises, callable)
    }
    /**
     * @see grails.async.PromiseFactory#onError(java.util.List, groovy.lang.Closure)
     */
    static<T> Promise<List<T>> onError(List<Promise<T>> promises, Closure<?> callable ) {
        return promiseFactory.onError(promises, callable)
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(java.util.Map)
     */
    static<K,V> Promise<Map<K,V>> createPromise(Map<K, V> map) {
        return promiseFactory.createPromise(map, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<List<T>> createPromise(Closure<T>... c) {
        return promiseFactory.createPromise(Arrays.asList(c), DECORATOR_LOOKUP.findDecorators())
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(java.util.Map)
     */
    static<K,V> Promise<Map<K,V>> tasks(Map<K, V> map) {
        return createPromise(map)
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<T> task(Closure<T> c) {
        return promiseFactory.createPromise(c, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<List<T>> tasks(Closure<T>... c) {
        return createPromise(c)
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<List<T>> tasks(List<Closure<T>> closures) {
        return promiseFactory.createPromise(closures, DECORATOR_LOOKUP.findDecorators())
    }

    /**
     * @see grails.async.PromiseFactory#createPromise()
     */
    static Promise<Object> createPromise() {
        return promiseFactory.createPromise()
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(Class)
     */
    static<T> Promise<T> createPromise(Class<T> returnType) {
        return promiseFactory.createPromise(returnType)
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure, java.util.List)
     */
    static<T> Promise<T> createPromise(Closure<T> c, List<PromiseDecorator> decorators) {
        return promiseFactory.createPromise(c, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(java.util.List, java.util.List)
     */
    static<T> Promise<List<T>> createPromise(List<Closure<T>> closures, List<PromiseDecorator> decorators) {
        return promiseFactory.createPromise(closures, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(grails.async.Promise[])
     */
    static <T> Promise<List<T>> createPromise(Promise<T>...promises) {
        return promiseFactory.createPromise(promises)
    }

    /**
     * @see grails.async.PromiseFactory#createBoundPromise(java.lang.Object)
     */
    static<T> Promise<T> createBoundPromise(T value) {
        return promiseFactory.createBoundPromise(value)
    }
}
