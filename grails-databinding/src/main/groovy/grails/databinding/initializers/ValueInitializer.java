package grails.databinding.initializers;


public interface ValueInitializer {
    Object initialize();
    Class<?> getTargetType();
}
