package grails.test.mixin;

import org.junit.rules.TestRule;

/**
 * Interface that is used for test mixin classes that want to return a TestRule instance that should be weaved in 
 * a field annotated with the @{@link org.junit.Rule} annotation in the concrete test class
 * 
 * @author Lari Hotari
 * @since 2.4
 */
public interface RuleFactory {
    public TestRule newRule(Object targetInstance);
}
