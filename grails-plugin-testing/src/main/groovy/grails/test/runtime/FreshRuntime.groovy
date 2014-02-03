package grails.test.runtime

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to be used in Junit tests and Spock specifications.
 * Tests annotated with this annotation will get a fresh TestRuntime instance.
 * If the annotation is added to class level, it will get used for all tests in the class. 
 *
 * The usage of this annotation is required for enabling doWithSpring/doWithConfig callbacks 
 * that work with the test instance.
 *  
 * @author Lari Hotari
 * @since 2.4
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
public @interface FreshRuntime {

}
