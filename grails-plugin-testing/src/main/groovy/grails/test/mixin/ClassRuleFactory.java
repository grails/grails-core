package grails.test.mixin;

import org.junit.rules.TestRule;

/**
 * Interface that is used for test mixin classes that want to return a TestRule instance that should be weaved in 
 * a field annotated with the @{@link org.junit.ClassRule} annotation in the concrete test class
 * 
 * The mixin instance will be held in a static field in the owner class when the mixin class implements this interface.
 * 
 * @author Lari Hotari
 * @since 2.4
 */
public interface ClassRuleFactory {
    public TestRule newClassRule(Class<?> targetClass);
}
