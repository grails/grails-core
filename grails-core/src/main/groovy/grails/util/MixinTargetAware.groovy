package grails.util

/**
 * An interface for mixins to implement that want to be made aware of the target object
 *
 * @author Graeme Rocher
 * @since 2.3.8
 */
public interface MixinTargetAware<T> {

    void setTarget(T targetObject)
}