package grails.test.runtime

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to be used in Junit tests and Spock specifications.
 * 
 * This annotation is for marking test classes and/or packages that should share the same runtime. 
 *
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.PACKAGE, ElementType.TYPE])
public @interface SharedRuntime {
    /**
     * All tests annotated with this annotation and same value() will use the same shared TestRuntime instance.
     * 
     * @return
     */
    Class<? extends SharedRuntimeConfigurer> value() default DefaultSharedRuntimeConfigurer
}
