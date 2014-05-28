package grails.boot.config

import groovy.transform.CompileStatic
import org.grails.boot.support.GrailsApplicationPostProcessor
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.util.ClassUtils

/**
 * A Grails configuration that scans for classes using the packages defined by the packages() method and creates the necessary
 * {@link org.codehaus.groovy.grails.commons.GrailsApplication} and {@link org.codehaus.groovy.grails.plugins.GrailsPluginManager} beans
 * that constitute a Grails application.
 *
 * @see GrailsApplicationPostProcessor
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsConfiguration implements ResourceLoaderAware {

    public static final String CLASS_RESOURCE_PATTERN = "/**/*.class"

    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver()

    /**
     * @return A post processor that uses the {@link org.codehaus.groovy.grails.plugins.GrailsPluginManager} to configure the {@link org.springframework.context.ApplicationContext}
     */
    @Bean
    GrailsApplicationPostProcessor grailsApplicationPostProcessor() {
        return new GrailsApplicationPostProcessor(classes() as Class[])
    }

    /**
     * @return The classes that constitute the Grails application
     */
    Collection<Class> classes() {
        def readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver)
        def packages = packages()
        Collection<Class> classes = []
        for (pkg in packages) {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(pkg.name) + CLASS_RESOURCE_PATTERN;

            classes.addAll scanUsingPattern(pattern, readerFactory)
        }

        // try the default package in case of a script without recursing into subpackages
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +  "*.class"
        classes.addAll scanUsingPattern(pattern, readerFactory)


        return classes
    }

    /**
     * @return The packages to scan
     */
    Collection<Package> packages() {
        [ getClass().package ]
    }

    @Override
    void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader)
    }

    private Collection<Class> scanUsingPattern(String pattern, CachingMetadataReaderFactory readerFactory) {
        def resources = this.resourcePatternResolver.getResources(pattern)
        def classLoader = Thread.currentThread().contextClassLoader
        Collection<Class> classes = []
        for (Resource res in resources) {
            // ignore closures / inner classes
            if(!res.filename.contains('$')) {
                def reader = readerFactory.getMetadataReader(res)
                def metadata = reader.annotationMetadata
                if (metadata.annotationTypes.any() { String annotation -> annotation.startsWith('grails.') }) {
                    classes << classLoader.loadClass(reader.classMetadata.className)
                }
            }
        }
        return classes
    }

}
