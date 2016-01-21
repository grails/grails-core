package grails.boot

import grails.boot.config.tools.SettingsFile
import grails.compiler.ast.ClassInjector
import grails.core.GrailsApplication
import grails.io.IOUtils
import grails.plugins.GrailsPlugin
import grails.plugins.GrailsPluginManager
import grails.util.BuildSettings
import grails.util.Environment
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.grails.boot.internal.JavaCompiler
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer
import org.grails.compiler.injection.GrailsAwareInjectionOperation
import org.grails.core.util.BeanCreationProfilingPostProcessor
import org.grails.io.watch.DirectoryWatcher
import org.grails.io.watch.FileExtensionFileChangeListener
import org.grails.plugins.support.WatchPattern
import org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Extends the {@link SpringApplication} with reloading behavior and other Grails features
 *
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@InheritConstructors
@Slf4j
class GrailsApp extends SpringApplication {

    private static boolean developmentModeActive = false
    private static DirectoryWatcher directoryWatcher

    boolean enableBeanCreationProfiler = false

    @Override
    ConfigurableApplicationContext run(String... args) {
        def applicationContext = super.run(args)

        Environment environment = Environment.getCurrent()

        log.info("Application starting in environment: {}", environment.getName())
        log.debug("Application directory discovered as: {}", IOUtils.findApplicationDirectory())
        log.debug("Current base directory is [{}]. Reloading base directory is [{}]", new File("."), BuildSettings.BASE_DIR)

        if(environment.isReloadEnabled()) {
            log.debug("Reloading status: ", environment.isReloadEnabled())
            enableDevelopmentModeWatch(environment, applicationContext)
        }
        printRunStatus(applicationContext)

        return applicationContext
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        ConfigurableApplicationContext applicationContext = super.createApplicationContext()

        applyAutowireByNamePerformanceOptimization(applicationContext)
        if(enableBeanCreationProfiler) {
            def processor = new BeanCreationProfilingPostProcessor()
            applicationContext.getBeanFactory().addBeanPostProcessor(processor)
            applicationContext.addApplicationListener(processor)
        }
        return applicationContext
    }

    // SPR-11864 workaround
    protected void applyAutowireByNamePerformanceOptimization(ConfigurableApplicationContext configurableApplicationContext) {
        if(configurableApplicationContext instanceof GenericApplicationContext) {
            Field beanFactoryField = ReflectionUtils.findField(GenericApplicationContext, "beanFactory", DefaultListableBeanFactory)
            ReflectionUtils.makeAccessible(beanFactoryField)

            def beanFactory = new OptimizedAutowireCapableBeanFactory()
            ReflectionUtils.setField(beanFactoryField, configurableApplicationContext, beanFactory)
        }
    }

    @Override
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        configurePropertySources(environment, args)

        def env = Environment.current
        environment.addActiveProfile(env.name)
    }

    @CompileDynamic // TODO: Report Groovy VerifierError
    protected void enableDevelopmentModeWatch(Environment environment, ConfigurableApplicationContext applicationContext, String... args) {
        def location = environment.getReloadLocation()

        if(location) {
            directoryWatcher = new DirectoryWatcher()
            configureDirectoryWatcher(directoryWatcher, location)
            Queue<File> changedFiles = new ConcurrentLinkedQueue<>()
            Queue<File> newFiles = new ConcurrentLinkedQueue<>()

            directoryWatcher.addListener(new FileExtensionFileChangeListener(['groovy', 'java']) {
                @Override
                void onChange(File file, List<String> extensions) {
                    changedFiles << file.canonicalFile
                }

                @Override
                void onNew(File file, List<String> extensions) {
                    changedFiles << file.canonicalFile
                    newFiles << file.canonicalFile
                }
            })

            def pluginManagerListener = createPluginManagerListener(applicationContext)
            directoryWatcher.addListener(pluginManagerListener)

            File baseDir = new File(location).canonicalFile

            List<File> watchBaseDirectories = [baseDir]
            def parentDir = baseDir.parentFile
            File settingsFile = new File(parentDir, "settings.gradle")

            if(settingsFile.exists()) {
                def cc = new CompilerConfiguration()
                cc.scriptBaseClass = SettingsFile.name
                def binding = new Binding()
                def shell = new GroovyShell(Thread.currentThread().contextClassLoader, binding, cc)
                try {
                    shell.evaluate(settingsFile)
                } catch (Throwable e) {
                    // ignore
                }
                def projectPaths = binding.getVariables().get('projectPaths')
                if(projectPaths) {
                    for(path in projectPaths) {
                        if(path) {

                            def pathStr = path.toString()
                            if(pathStr.startsWith(':')) {
                                pathStr = pathStr.substring(1)
                            }
                            watchBaseDirectories << new File(parentDir, pathStr)
                        }
                    }
                }
            }

            def locationLength = location.length() + 1
            def pluginManager = applicationContext.getBean(GrailsPluginManager)
            for(GrailsPlugin plugin in pluginManager.allPlugins) {
                for(WatchPattern wp in plugin.watchedResourcePatterns) {
                    for(watchBase in watchBaseDirectories) {
                        if(wp.file) {
                            def resolvedPath = new File(watchBase, wp.file.path.substring(locationLength))
                            directoryWatcher.addWatchFile(resolvedPath)
                        }
                        else if(wp.directory && wp.extension) {

                            def resolvedPath = new File(watchBase, wp.directory.path.substring(locationLength))
                            directoryWatcher.addWatchDirectory(resolvedPath, wp.extension)
                        }
                    }
                }
            }


            developmentModeActive = true
            Thread.start {
                CompilerConfiguration compilerConfig = new CompilerConfiguration()
                compilerConfig.setTargetDirectory(new File(location, BuildSettings.BUILD_CLASSES_PATH))

                while(isDevelopmentModeActive()) {
                    // Workaround for some IDE / OS combos - 2 events (new + update) for the same file
                    def uniqueChangedFiles = changedFiles as Set


                    def i = uniqueChangedFiles.size()
                    try {
                        if(i > 1) {
                            changedFiles.clear()
                            for(f in uniqueChangedFiles) {
                                recompile(f, compilerConfig, location)
                                if(newFiles.contains(f)) {
                                    newFiles.remove(f)
                                    pluginManager.informOfFileChange(f)
                                }
                                sleep 1000
                            }
                        }
                        else if(i == 1) {
                            changedFiles.clear()
                            def changedFile = uniqueChangedFiles[0]
                            changedFile = changedFile.canonicalFile
                            // Groovy files within the 'conf' directory are not compiled
                            if(changedFile.path.contains('/grails-app/conf/')) {
                                pluginManager.informOfFileChange(changedFile)
                            }
                            else {
                                recompile(changedFile, compilerConfig, location)
                                if(newFiles.contains(changedFile)) {
                                    newFiles.remove(changedFile)
                                    pluginManager.informOfFileChange(changedFile)
                                }
                            }
                        }

                        newFiles.clear()
                    } catch (CompilationFailedException cfe) {
                        log.error("Compilation Error: $cfe.message", cfe)
                    }

                    sleep 1000
                }

            }
            directoryWatcher.start()
        }


    }

    static boolean isDevelopmentModeActive() {
        return developmentModeActive
    }

    static void setDevelopmentModeActive(boolean active) {
        developmentModeActive = active
        if(directoryWatcher != null) {
            directoryWatcher.active = active
        }
    }

    protected void recompile(File changedFile, CompilerConfiguration compilerConfig, String location) {
        File appDir = null
        def changedPath = changedFile.path
        if (changedPath.contains('/grails-app')) {
            appDir = new File(changedPath.substring(0, changedPath.indexOf('/grails-app')))
        }
        def baseFileLocation = appDir?.absolutePath ?: location
        compilerConfig.setTargetDirectory(new File(baseFileLocation, BuildSettings.BUILD_CLASSES_PATH))
        println "File $changedFile changed, recompiling..."
        if(changedFile.name.endsWith('.java')) {
            if(JavaCompiler.isAvailable()) {
                JavaCompiler.recompile(compilerConfig, changedFile)
            }
            else {
                log.error("Cannot recompile [$changedFile.name], the current JVM is not a JDK (recompilation will not work on a JRE missing the compiler APIs).")
            }
        }
        else {
            compileGroovyFile(compilerConfig, changedFile)
        }
    }

    protected void compileGroovyFile(CompilerConfiguration compilerConfig, File changedFile) {
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()
        for (ClassInjector classInjector in classInjectors) {
            if (classInjector instanceof AbstractGrailsArtefactTransformer) {
                ((AbstractGrailsArtefactTransformer) classInjector).clearCachedState()
            }
        }
        // only one change, just to a simple recompile and propagate the change
        def unit = new CompilationUnit(compilerConfig)
        unit.addSource(changedFile)
        unit.compile()
    }

    /**
     * Creates and returns a file change listener for notifying the plugin manager of changes.
     * @param applicationContext - The running {@link org.springframework.context.ApplicationContext}
     * @return {@link DirectoryWatcher.FileChangeListener}
     */
    protected static DirectoryWatcher.FileChangeListener createPluginManagerListener(ConfigurableApplicationContext applicationContext) {
        def pluginManager = applicationContext.getBean(GrailsPluginManager)
        return new DirectoryWatcher.FileChangeListener() {
            @Override
            void onChange(File file) {
                if(!file.name.endsWith('.groovy') && !file.name.endsWith('.java')) {
                    pluginManager.informOfFileChange(file)
                }
            }

            @Override
            void onNew(File file) {
                if(!file.name.endsWith('.groovy') && !file.name.endsWith('.java')) {
                    pluginManager.informOfFileChange(file)
                }
            }
        }
    }

    protected void configureDirectoryWatcher(DirectoryWatcher directoryWatcher, String location) {
        directoryWatcher.addWatchDirectory(new File(location, "grails-app"), ['groovy', 'java'])
        directoryWatcher.addWatchDirectory(new File(location, "src/main/groovy"), ['groovy', 'java'])
        directoryWatcher.addWatchDirectory(new File(location, "src/main/java"), ['groovy', 'java'])
    }

    @CompileDynamic
    protected printRunStatus(ConfigurableApplicationContext applicationContext) {
        try {
            def protocol = System.getProperty('server.ssl.key-store') ? 'https' : 'http'
            GrailsApplication app = applicationContext.getBean(GrailsApplication)
            String context_path = app.config.getProperty('server.context-path', '')
            if(context_path){
                println("WARNING: 'server.context-path: ${context_path}' is deprecated. Please use 'server.contextPath: ${context_path}'")
            } else {
                context_path=''
            }
            // in spring-boot context-path is chosen before contextPath ...
            String contextPath = context_path?context_path:app.config.getProperty('server.contextPath', '')
            println("Grails application running at ${protocol}://localhost:${applicationContext.embeddedServletContainer.port}${contextPath} in environment: ${Environment.current.name}")
        } catch (e) {
            // ignore
        }
    }

    @Override
    protected void printBanner() {
        // noop
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object source, String... args) {
        return run([ source ] as Object[], args);
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
        return new GrailsApp(sources).run(args);
    }
}
