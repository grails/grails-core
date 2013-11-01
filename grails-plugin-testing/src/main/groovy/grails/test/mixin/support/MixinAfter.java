package grails.test.mixin.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ordered analogue for @After. The priority number works in reverse for @MixinAfter
 * in that priority = 0 will run last. All @MixinAfter methods are guaranteed to run after
 * any @After methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MixinAfter {
    int priority();
}
