package grails.async

import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class PromiseListSpec extends Specification{


    void "Test promise chaining"() {
        when:"A promise is chained"
            def list = new PromiseList()
            list << { 1 }
            list << { 2 }
            list << { 3 }
            def res
            list.onComplete { List results ->
                res = results
            }
            sleep 500

        then:'the chain is executed'
            res == [1,2,3]
    }

    void "Test promise chaining with an exception"() {
        when:"A promise is chained"
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

        then:'the chain is executed'
            err != null
            err.message == "bad"
            res == null
    }
}
