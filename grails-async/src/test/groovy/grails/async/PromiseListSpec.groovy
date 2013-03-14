package grails.async

import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class PromiseListSpec extends Specification{


    void "Test promise list handling"() {
        when:"A list of promises is created"
            def list = new PromiseList()
            list << { 1 }
            list << { 2 }
            list << { 3 }
            def res
            list.onComplete { List results ->
                res = results
            }
            sleep 500

        then:'then the result from onComplete is correct'
            res == [1,2,3]
    }

    void "Test promise list handling with some async operations and some values"() {
        when:"A list of promises is created"
            def list = new PromiseList()
            list << { 1 }
            list <<  2
            list << { 3 }
            def res
            list.onComplete { List results ->
                res = results
            }
            sleep 500

        then:'then the result from onComplete is correct'
            res == [1,2,3]
    }

    void "Test promise list with then chaining"() {
        when:"A promise list is used with then chaining"
            def list = new PromiseList<Integer>()
            list << { 1 }
            def promise = list.then { it << 2; it }.then { it << 3; it}
            def result = promise.get()
        then:"An appropriately populated list is produced"
            result == [1,2,3]

    }

    void "Test promise list with an exception"() {
        when:"A promise list with a promise that throws an exception"
            def list = new PromiseList()
            list << { 1 }
            list << { throw new RuntimeException("bad") }
            list << { 3 }
            def res
            list.onComplete { List results ->
                res = results
            }
            def err
            list.onError { Throwable t ->
                err = t
            }
            sleep 500

        then:'the onError handler is invoked with the exception'
            err != null
            err.message == "bad"
            res == null
    }
}
