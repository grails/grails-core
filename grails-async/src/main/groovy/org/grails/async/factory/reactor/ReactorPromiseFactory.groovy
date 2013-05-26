package org.grails.async.factory.reactor

import grails.async.Promise as P
import grails.async.PromiseList
import org.grails.async.factory.AbstractPromiseFactory
import org.grails.async.factory.gpars.GparsPromise
import reactor.core.Composable
import reactor.core.Promise
import reactor.core.R

/**
 * Reactor Implementation of {@link grails.async.PromiseFactory} interface
 *
 * @author Stephane Maldini
 * @since 2.3
 */
class ReactorPromiseFactory extends AbstractPromiseFactory {

    static final boolean REACTOR_PRESENT
    static {
        try {
            REACTOR_PRESENT = Thread.currentThread().contextClassLoader.loadClass("reactor.Fn") != null
        } catch (Throwable e) {
            REACTOR_PRESENT = false
        }
    }

    static boolean isReactorAvailable() {
        REACTOR_PRESENT
    }

    @Override
    def <T> grails.async.Promise<T> createBoundPromise(T value) {
        final variable = R.promise(value).build()
        return new ReactorPromise<T>(variable)
    }


    @Override
    def <T> P<T> createPromise(Closure<T>... closures) {
        if (closures.length == 1) {
            final callable = closures[0]
            return new ReactorPromise<T>(applyDecorators(callable, null))
        }
        def promiseList = new PromiseList()
        for (p in closures) {
            applyDecorators(p, null)
            promiseList << p
        }
        return promiseList
    }

    @Override
    def <T> List<T> waitAll(List<P<T>> promises) {
        final reactorPromises = promises.collect { (ReactorPromise) it }
        final Collection<Promise<T>> _promises = reactorPromises.collect { ReactorPromise it -> it.internalPromise }
        Promise.merge(_promises).await()
    }

    @Override
    def <T> P<List<T>> onComplete(List<P<T>> promises, @SuppressWarnings("rawtypes") Closure callable) {
        final reactorPromises = promises.collect { (ReactorPromise) it }
        def result = Promise.merge(
            reactorPromises.collect { ReactorPromise<T> it -> it.internalPromise }
        ).
            onSuccess(callable)
        new ReactorPromise<List<T>>(
            result
        )
    }

    @Override
    def <T> P<List<T>> onError(List<P<T>> promises, @SuppressWarnings("rawtypes") Closure callable) {
        final reactorPromises = promises.collect { (ReactorPromise) it }
        def result = Promise.merge(
            reactorPromises.collect { ReactorPromise<T> it -> it.internalPromise }
        ).
            onError(callable)
        new ReactorPromise<List<T>>(
            result
        )
    }
}
