package org.grails.core.support.internal.tools;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Set;

/**
 * Attempts to limit classpath searching to only locations relative to the given class
 *
 * @author Graeme Rocher
 * @since 3.1.13
 */
public class ClassRelativeResourcePatternResolver extends PathMatchingResourcePatternResolver {

    public ClassRelativeResourcePatternResolver(Class clazz) {
        super(new ClassRelativeClassLoader(clazz));
    }

    @Override
    protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
        // no-op - don't search jar roots
    }
}
