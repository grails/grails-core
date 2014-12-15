package grails.boot.config

import grails.config.Settings
import grails.core.GrailsApplicationLifeCycle
import groovy.transform.CompileStatic
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.util.ClassUtils

/**
 * A base class for configurations that bootstrap a Grails application
 *
 * @since 3.0
 * @author Graeme Rocher
 *
 */
@CompileStatic
class GrailsAutoConfiguration implements GrailsApplicationLifeCycle, ResourceLoaderAware, ApplicationContextAware {

    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver()
    ApplicationContext applicationContext

    /**
     * @return A post processor that uses the {@link grails.plugins.GrailsPluginManager} to configure the {@link org.springframework.context.ApplicationContext}
     */
    @Bean
    GrailsApplicationPostProcessor grailsApplicationPostProcessor() {
        return new GrailsApplicationPostProcessor( this, applicationContext, classes() as Class[])
    }

    /**
     * @return The classes that constitute the Grails application
     */
    Collection<Class> classes() {
        def readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver)
        def packages = packages()
        Collection<Class> classes = [] as Set
        for (pkg in packages) {
            if(pkg == null) continue
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(pkg.name) + Settings.CLASS_RESOURCE_PATTERN;

            classes.addAll scanUsingPattern(pattern, readerFactory)
        }

        // try the default package in case of a script without recursing into subpackages
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +  "*.class"
        classes.addAll scanUsingPattern(pattern, readerFactory)

        def classLoader = Thread.currentThread().contextClassLoader
        for(cls in AbstractGrailsArtefactTransformer.transformedClassNames) {
            try {
                classes << classLoader.loadClass(cls)
            } catch (ClassNotFoundException cnfe) {
                // ignore
            }
        }

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

    @Override
    Closure doWithSpring() { null }

    @Override
    void doWithDynamicMethods() {
        // no-op
    }

    @Override
    void doWithApplicationContext() {
        // no-op
    }

    @Override
    void onConfigChange(Map<String, Object> event) {
        // no-op
    }

    @Override
    void onShutdown(Map<String, Object> event) {
        // no-op
    }
}

