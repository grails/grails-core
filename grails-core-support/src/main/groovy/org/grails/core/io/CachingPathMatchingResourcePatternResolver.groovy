package org.grails.core.io

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * @author Graeme Rocher
 * @since 3.0.12
 */
@CompileStatic
class CachingPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {
    public static final CachingPathMatchingResourcePatternResolver INSTANCE = new CachingPathMatchingResourcePatternResolver();

    private CachingPathMatchingResourcePatternResolver(){}

    CachingPathMatchingResourcePatternResolver(ResourceLoader parent) {
        super(parent)
    }

    @Memoized(maxCacheSize = 20)
    protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
        return super.doFindAllClassPathResources(path)
    }

    @Memoized(maxCacheSize = 20)
    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
        return super.findPathMatchingResources(locationPattern)
    }
}
