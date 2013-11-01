package grails.test.mixin.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ordered analogue for @Before. Lower priority methods run first and all @MixinBefore methods
 * are guaranteed to run before any @Before.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MixinBefore {
    int priority();
}
