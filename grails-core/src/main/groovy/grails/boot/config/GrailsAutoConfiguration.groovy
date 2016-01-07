package grails.boot.config

import grails.config.Settings
import grails.core.GrailsApplicationClass
import grails.core.GrailsApplicationLifeCycle
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer
import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.springframework.aop.config.AopConfigUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.UrlResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.util.ClassUtils
import org.springframework.util.ResourceUtils

import java.lang.reflect.Field

/**
 * A base class for configurations that bootstrap a Grails application
 *
 * @since 3.0
 * @author Graeme Rocher
 *
 */
@CompileStatic
class GrailsAutoConfiguration implements GrailsApplicationClass, ResourceLoaderAware, ApplicationContextAware {

    private static final String APC_PRIORITY_LIST_FIELD = "APC_PRIORITY_LIST";

    static {
        try {
            // patch AopConfigUtils if possible
            Field field = AopConfigUtils.class.getDeclaredField(APC_PRIORITY_LIST_FIELD);
            if(field != null) {
                field.setAccessible(true);
                Object obj = field.get(null);
                List<Class<?>> list = (List<Class<?>>) obj;
                list.add(GroovyAwareAspectJAwareAdvisorAutoProxyCreator.class);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    ResourcePatternResolver resourcePatternResolver = new GrailsClasspathIgnoringResourceResolver()

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
        def packages = packageNames().unique()
        Collection<Class> classes = new HashSet()
        for (pkg in packages) {
            if(pkg == null) continue
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(pkg) + Settings.CLASS_RESOURCE_PATTERN;

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
        def thisPackage = getClass().package
        thisPackage ? [ thisPackage ] : []
    }

    /**
     * @return The package names to scan. Delegates to {@link #packages()} by default
     */
    Collection<String> packageNames() {
        packages().collect { Package p -> p.name }
    }

    @Override
    void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = new GrailsClasspathIgnoringResourceResolver(resourceLoader)
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
    void onStartup(Map<String, Object> event) {
        // no-op
    }

    @Override
    void onShutdown(Map<String, Object> event) {
        // no-op
    }

    @CompileStatic
    @InheritConstructors
    @Slf4j
    private static class GrailsClasspathIgnoringResourceResolver extends PathMatchingResourcePatternResolver {
        @Override
        protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
            Set<Resource> result = new LinkedHashSet<Resource>(16)
            ClassLoader cl = getClassLoader()
            Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path))
            while (resourceUrls.hasMoreElements()) {

                URL url = resourceUrls.nextElement()
                // if the path is from a JAR file ignore, plugins inside JAR files will have their own mechanism for loading
                if(!url.path.contains('jar!/grails/')) {
                    result.add(convertClassLoaderURL(url))
                }

            }
            return result
        }


        @Memoized
        protected Resource[] findAllClassPathResources(String location) throws IOException {
            return super.findAllClassPathResources(location)
        }

        @Memoized
        protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
            return super.findPathMatchingResources(locationPattern)
        }
    }


}

