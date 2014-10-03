package grails.boot

import grails.util.Environment
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.util.logging.Commons
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.grails.io.watch.DirectoryWatcher
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

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
@Commons
class GrailsApp extends SpringApplication {


    @Override
    ConfigurableApplicationContext run(String... args) {
        def applicationContext = super.run(args)

        grails.util.Environment environment = grails.util.Environment.getCurrent()
        if(environment.isReloadEnabled()) {
            enableDevelopmentModeWatch(environment, applicationContext)
        }
        printRunStatus(applicationContext)

        return applicationContext
    }

    protected void enableDevelopmentModeWatch(Environment environment, ConfigurableApplicationContext applicationContext, String... args) {
        def location = environment.getReloadLocation()

        DirectoryWatcher directoryWatcher = new DirectoryWatcher()
        configureDirectoryWatcher(directoryWatcher, location)
        Queue<File> changedFiles = new ConcurrentLinkedQueue<>()

        directoryWatcher.addListener(new DirectoryWatcher.FileChangeListener() {
            @Override
            void onChange(File file) {
               changedFiles << file
            }

            @Override
            void onNew(File file) {
               changedFiles << file
            }
        })

        Thread.start {
            CompilerConfiguration compilerConfig = new CompilerConfiguration()
            compilerConfig.setTargetDirectory(new File(location, "build/classes/main"))

            while(true) {
                def i = changedFiles.size()
                try {
                    if(i > 1) {

                        // more than one change, recompile and restart
                        applicationContext.close()
                        def unit = new CompilationUnit(compilerConfig)
                        unit.addSources(changedFiles as File[])
                        unit.compile()
                        changedFiles.clear()
                        super.run(args)
                    }
                    else if(i == 1) {
                        def changedFile = changedFiles.poll()
                        println "File $changedFile changed, recompiling..."
                        // only one change, just to a simple recompile and propagate the change
                        def unit = new CompilationUnit(compilerConfig)
                        unit.addSource(changedFile)
                        unit.compile()
                    }
                } catch (CompilationFailedException cfe) {
                    log.error("Compilation Error: $cfe.message", cfe)
                }

                sleep 1000
            }

        }
        directoryWatcher.start()

    }

    protected void configureDirectoryWatcher(DirectoryWatcher directoryWatcher, String location) {
        directoryWatcher.addWatchDirectory(new File(location, "grails-app"), ['groovy', 'java'])
        directoryWatcher.addWatchDirectory(new File(location, "src/main/groovy"), ['groovy', 'java'])
        directoryWatcher.addWatchDirectory(new File(location, "src/main/java"), ['groovy', 'java'])
    }

    @CompileDynamic
    protected printRunStatus(ConfigurableApplicationContext applicationContext) {
        try {
            log.info("Grails application running at http://localhost:${applicationContext.embeddedServletContainer.port}")
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
