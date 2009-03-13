package org.codehaus.groovy.grails.plugins.support.aware;

/**
 * <p>Convenience interface that can be implemented by classes that are
 * registered by plugins.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public interface ClassLoaderAware {
    /**
     * <p>This method is called by the {@link org.springframework.context.ApplicationContext} that
     * loads the Grails application. The {@link ClassLoader} that loads the Grails application code
     * is injected.</p>
     *
     * @param classLoader the {@link ClassLoader} that loads the Grails application code
     */
    void setClassLoader(ClassLoader classLoader);
}
