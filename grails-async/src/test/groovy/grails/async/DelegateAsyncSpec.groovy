package grails.async

import org.grails.async.decorator.PromiseDecorator
import org.grails.async.decorator.PromiseDecoratorProvider
import spock.lang.Specification

/**
 */
class DelegateAsyncSpec extends Specification{

    void "Test delegate async applied to class with a method taking arguments"() {

        when:"The DelegateAsync annotation is applied to a class."
            def mathService = new AsyncMathService()
            def p = mathService.sum(1,2)
        then:"Methods from the delegate return a promise"
            p instanceof Promise

        when:"The the value of the promise is obtained"
            def val = p.get()

        then:"It is correct"
            val == 3
    }

    void "Test delegate async applied to field with a method taking arguments"() {

        when:"The DelegateAsync annotation is applied to a class."
            def mathService = new AsyncMathService2()
            def p = mathService.sum(1,2)
        then:"Methods from the delegate return a promise"
            p instanceof Promise

        when:"The the value of the promise is obtained"
            def val = p.get()

        then:"It is correct"
            val == 3
    }

    void "Test delegate async passes decorators to created promises if target is a DecoratorProvider"() {

        when:"The DelegateAsync annotation is applied to a class."
            def mathService = new AsyncMathService3()
            def p = mathService.sum(1,2)
        then:"Methods from the delegate return a promise"
            p instanceof Promise

        when:"The the value of the promise is obtained"
            def val = p.get()

        then:"The decorator is applied to the value "
            val == 6
    }
}
class MathService {
    Integer sum(int n1, int n2) {
        n1 + n2
    }
    void calculate() {
        // no-op
    }
}
@DelegateAsync(MathService)
class AsyncMathService {}
class AsyncMathService2 {

    @DelegateAsync
    MathService ms = new MathService()
}
@DelegateAsync(MathService)
class AsyncMathService3 implements PromiseDecoratorProvider {
    List<PromiseDecorator> decorators = [ { Closure c ->
        return { c.call(*it) * 2  }
    } as PromiseDecorator ]
}

