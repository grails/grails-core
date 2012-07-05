package org.grails.plugins.tomcat

    import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory
import org.grails.plugins.tomcat.fork.ForkedTomcatServer
import org.grails.plugins.tomcat.fork.TomcatExecutionContext
import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import grails.util.Environment
import org.apache.commons.logging.Log

class TomcatServerFactory implements EmbeddableServerFactory,BuildSettingsAware {

    BuildSettings buildSettings

    @CompileStatic
    EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        TomcatExecutionContext ec = new TomcatExecutionContext()
        List<File> buildDependencies = buildMinimalIsolatedClasspath()

        ec.buildDependencies = buildDependencies



        ec.runtimeDependencies = buildSettings.runtimeDependencies
        ec.providedDependencies = buildSettings.providedDependencies
        ec.contextPath = contextPath
        ec.baseDir = buildSettings.baseDir
        ec.env = Environment.current.name
        ec.grailsHome = buildSettings.grailsHome
        ec.classesDir = buildSettings.classesDir
        ec.grailsWorkDir = buildSettings.grailsWorkDir
        ec.projectWorkDir = buildSettings.projectWorkDir
        ec.projectPluginsDir = buildSettings.projectPluginsDir
        ec.testClassesDir = buildSettings.testClassesDir
        ec.resourcesDir = buildSettings.resourcesDir

        return new ForkedTomcatServer(ec)
    }

    @CompileStatic
    private List<File> buildMinimalIsolatedClasspath() {
        List<File> buildDependencies = []

        buildDependencies.addAll(IsolatedWarTomcatServer.findTomcatJars(buildSettings))
        buildDependencies.add findJarFile(GroovySystem)
        buildDependencies.add findJarFile(Log)

        buildDependencies.addAll buildSettings.buildDependencies.findAll { File f ->
            final fileName = f.name
            fileName.contains('grails-bootstrap') ||
                    fileName.contains('jcl-over-slf4j') ||
                    fileName.contains('slf4j-api') ||
                    fileName.contains('ivy') ||
                    fileName.contains('ant') ||
                    fileName.contains('jline') ||
                    fileName.contains('jansi')
        }
        buildDependencies
    }

    @CompileStatic
    private File findJarFile(Class targetClass) {
        def absolutePath = targetClass.getResource('/' + targetClass.name.replace(".", "/") + ".class").getPath()
        final jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
        final jarFile = new File(jarPath)
        jarFile
    }

    EmbeddableServer createForWAR(String warPath, String contextPath) {
        return new IsolatedWarTomcatServer(warPath, contextPath)
    }
}
