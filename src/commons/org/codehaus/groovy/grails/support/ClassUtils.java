package org.codehaus.groovy.grails.support;

/**
 * <p>Class with static method that operate on {@link Class} instances.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ClassUtils {

    /**
     * Detect if left and right types are matching types. In particular,
     * test if one is a primitive type and the other is the corresponding
     * Java wrapper type. Primitive and wrapper classes may be passed to
     * either arguments.
     *
     * @param leftType
     * @param rightType
     * @return
     */
    public static boolean isMatchBetweenPrimativeAndWrapperTypes(Class leftType, Class rightType) {
        if (leftType == null) {
            throw new NullPointerException("Left type is null!");
        } else if (rightType == null) {
            throw new NullPointerException("Right type is null!");
        } else if (leftType == Boolean.class && rightType == boolean.class) {
            return true;
        } else if (leftType == boolean.class && rightType == Boolean.class) {
            return true;
        } else if (leftType == Integer.class && rightType == int.class) {
            return true;
        } else if (leftType == int.class && rightType == Integer.class) {
            return true;
        } else if (leftType == Short.class && rightType == short.class) {
            return true;
        } else if (leftType == short.class && rightType == Short.class) {
            return true;
        } else if (leftType == Byte.class && rightType == byte.class) {
            return true;
        } else if (leftType == byte.class && rightType == Byte.class) {
            return true;
        } else if (leftType == Character.class && rightType == char.class) {
            return true;
        } else if (leftType == char.class && rightType == Character.class) {
            return true;
        } else if (leftType == Long.class && rightType == long.class) {
            return true;
        } else if (leftType == long.class && rightType == Long.class) {
            return true;
        } else if (leftType == Float.class && rightType == float.class) {
            return true;
        } else if (leftType == float.class && rightType == Float.class) {
            return true;
        } else if (leftType == Double.class && rightType == double.class) {
            return true;
        } else if (leftType == double.class && rightType == Double.class) {
            return true;
        }

        return false;
    }

}
