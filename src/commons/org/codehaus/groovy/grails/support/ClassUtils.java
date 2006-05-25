package org.codehaus.groovy.grails.support;

/**
 * <p>Class with static method that operate on {@link Class} instances.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ClassUtils {
    public static boolean isPrimitiveType(Class type) {
        if (type == null) {
            throw new NullPointerException("Type is null!");
        } else if (type == boolean.class) {
            return true;
        } else if (type == byte.class) {
            return true;
        } else if (type == char.class) {
            return true;
        } else if (type == short.class) {
            return true;
        } else if (type == int.class) {
            return true;
        } else if (type == long.class) {
            return true;
        } else if (type == double.class) {
            return true;
        } else if (type == float.class) {
            return true;
        }

        return false;
    }
}
