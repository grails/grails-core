package grails.test.mixin.support

import org.junit.rules.ExternalResource

import java.lang.reflect.Method

/**
 * Test rule which looks at each @MixinBefore and @MixinAfter method and builds a
 * before/after rule using proper priority. Since JUnit rules run before @Before and @After
 * this ensures that Grails specific setup/teardown occurs in the proper order.
 */
class MixinTestRule extends ExternalResource {
    final instance

    private List<Method> beforeMethods = []
    private List<Method> afterMethods = []

    MixinTestRule(instance) {
        this.instance = instance

        beforeMethods = methodsForAnnotation(MixinBefore.class)
        afterMethods = methodsForAnnotation(MixinAfter.class)
    }

    /**
     * Returns the annotated methods in priority order, lowest to highest. If two
     * have the same priority, their order is not guaranteed.
     *
     * @param annotationClass the annotation to look for
     * @return
     */
    private List<Method> methodsForAnnotation(Class annotationClass) {
        List<Method> methods = [].withDefault { [] }
        instance.getClass().getDeclaredMethods().each { method ->
            def annotation = method.getAnnotation(annotationClass)
            if( annotation ) {
                methods[annotation.priority()] << method
            }
        }

        // Flatten out the list and remove any nulls
        // (if a priority is missing, like 0 and a 2, 1 will be null)
        List<Method> flattened = methods.flatten() as List<Method>
        flattened.retainAll { it != null }
        flattened
    }

    @Override
    protected void before() throws Throwable {
        beforeMethods*.invoke(instance)
    }

    @Override
    protected void after() {
        afterMethods.reverse()*.invoke(instance)
    }
}
