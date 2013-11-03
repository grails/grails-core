package grails.test.mixin.support

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.junit.rules.ExternalResource

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

    private boolean isPotentialMethod(Method method) {
        boolean isPotential =  method.getReturnType().equals(void.class) &&
        method.getParameterTypes().length == 0 && Modifier.isPublic(method.getModifiers()) && !Modifier.isAbstract(method.getModifiers())
        return isPotential;
    }

    private static class MethodEntry {
        Method method 
        int level
        int lineNumber
        int priority 
    }
    
    /**
     * Returns the annotated methods in priority order, lowest to highest. If two
     * have the same priority, their order is not guaranteed.
     *
     * @param annotationClass the annotation to look for
     * @return
     */
    private List<Method> methodsForAnnotation(Class annotationClass) {
        List<MethodEntry> methods = []
        addMethods(instance.getClass(), annotationClass, methods, [] as Set, 1)
        methods.sort { a, b -> a.priority <=> b.priority ?: b.level <=> a.level ?: a.lineNumber <=> b.lineNumber }.collect { it.method }
    }

    private addMethods(Class currentClass, Class annotationClass, List<MethodEntry> methods, Set<String> addedMethods, int level) {
        currentClass.getDeclaredMethods().each { Method method ->
            if(isPotentialMethod(method) && !addedMethods.contains(method.name)) {
                def annotation = method.getAnnotation(annotationClass)
                if( annotation ) {
                    methods << new MethodEntry(method: method, lineNumber: annotation.lineNumber(), priority: annotation.priority(), level: level)
                    addedMethods << method.name
                }
            }
        }
        if(currentClass.getSuperclass() != Object) {
            addMethods(currentClass.getSuperclass(), annotationClass, methods, addedMethods, level+1)
        }
    }

    @Override
    void before() throws Throwable {
        beforeMethods*.invoke(instance)
    }

    @Override
    void after() {
        afterMethods.reverse()*.invoke(instance)
    }
}
