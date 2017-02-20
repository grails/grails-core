package grails.async.web

import grails.async.Promise
import grails.async.Promises
import grails.async.decorator.PromiseDecorator
import groovy.transform.CompileStatic
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

    private static final WebRequestPromiseDecoratorLookupStrategy DECORATOR_LOOKUP = new WebRequestPromiseDecoratorLookupStrategy()
    /**
     * @see grails.async.PromiseFactory#waitAll(grails.async.Promise[])
     */
    static<T> List<T> waitAll(Promise<T>...promises) {
        return Promises.waitAll(promises)
    }

    /**
     * @see grails.async.PromiseFactory#waitAll(java.util.List)
     */
    static<T> List<T> waitAll(List<Promise<T>> promises) {
        return Promises.waitAll(promises)
    }

    /**
     * @see grails.async.PromiseFactory#waitAll(java.util.List)
     */
    static<T> List<T> waitAll(List<Promise<T>> promises, final long timeout, final TimeUnit units) {
        return Promises.waitAll(promises, timeout, units)
    }

    /**
     * @see grails.async.PromiseFactory#onComplete(java.util.List, groovy.lang.Closure)
     */
    static<T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure<?> callable ) {
        return Promises.onComplete(promises, callable)
    }
    /**
     * @see grails.async.PromiseFactory#onError(java.util.List, groovy.lang.Closure)
     */
    static<T> Promise<List<T>> onError(List<Promise<T>> promises, Closure<?> callable ) {
        return Promises.onError(promises, callable)
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(java.util.Map)
     */
    static<K,V> Promise<Map<K,V>> createPromise(Map<K, V> map) {
        return Promises.createPromise(map, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<List<T>> createPromise(Closure<T>... c) {
        return Promises.createPromise(Arrays.asList(c), DECORATOR_LOOKUP.findDecorators())
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
        return Promises.createPromise(c, DECORATOR_LOOKUP.findDecorators())
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
        return Promises.createPromise(closures, DECORATOR_LOOKUP.findDecorators())
    }

    /**
     * @see grails.async.PromiseFactory#createPromise()
     */
    static Promise<Object> createPromise() {
        return Promises.createPromise()
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(Class)
     */
    static<T> Promise<T> createPromise(Class<T> returnType) {
        return Promises.createPromise(returnType)
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(groovy.lang.Closure, java.util.List)
     */
    static<T> Promise<T> createPromise(Closure<T> c, List<PromiseDecorator> decorators) {
        return Promises.createPromise(c, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(java.util.List, java.util.List)
     */
    static<T> Promise<List<T>> createPromise(List<Closure<T>> closures, List<PromiseDecorator> decorators) {
        return Promises.createPromise(closures, DECORATOR_LOOKUP.findDecorators())
    }
    /**
     * @see grails.async.PromiseFactory#createPromise(grails.async.Promise[])
     */
    static <T> Promise<List<T>> createPromise(Promise<T>...promises) {
        return Promises.createPromise(promises)
    }

    /**
     * @see grails.async.PromiseFactory#createBoundPromise(java.lang.Object)
     */
    static<T> Promise<T> createBoundPromise(T value) {
        return Promises.createBoundPromise(value)
    }
}
