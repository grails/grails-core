package org.grails.gradle.plugin.core
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.EscapeUnicode
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions
import org.grails.gradle.plugin.agent.AgentTasksEnhancer
import org.grails.gradle.plugin.commands.ApplicationContextCommandTask
import org.grails.gradle.plugin.run.FindMainClassTask

class GrailsGradlePlugin extends GroovyPlugin {
    List<Plugin<Project>> pluginInstancesToApply = [new IntegrationTestGradlePlugin()]

    void apply(Project project) {
        super.apply(project)
        pluginInstancesToApply.each { it.apply(project) }

        project.extensions.create("grails", GrailsExtension)
        registerFindMainClassTask(project)

        def projectDir = project.projectDir

        def grailsSourceDirs = []
        def excludedDirs = ['views', 'migrations', 'assets', 'i18n', 'conf']
        new File("$projectDir/grails-app").eachDir { File subdir ->
            def dirName = subdir.name
            if(!subdir.hidden && !dirName.startsWith(".") && !excludedDirs.contains(dirName)) {
                grailsSourceDirs << subdir.absolutePath
            }
        }

        grailsSourceDirs << "$projectDir/src/main/groovy"

        System.setProperty( BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
        def environment = Environment.current


        enableFileWatch(environment, project)

        def grailsVersion = project.getProperties().get('grailsVersion')

        if(!grailsVersion) {
            def grailsCoreDep = project.configurations.getByName('compile').dependencies.find { Dependency d -> d.name == 'grails-core' }
            grailsVersion = grailsCoreDep.version
        }

        enableNative2Ascii(project, grailsVersion)


        if(project.extensions.findByName('assets')) {
            project.assets {
                assetsPath = 'grails-app/assets'
                compileDir = 'build/assetCompile/assets'
            }
        }


        def tasks = project.tasks
        project.afterEvaluate {
            if( tasks.findByName("war") ) {
                project.war {
                   from "${project.buildDir}/assetCompile"
                }
            }
            else {
                project.processResources {
                    from("${project.buildDir}/assetCompile") {
                        into "META-INF"
                    }
                }
            }
        }


        configureConsoleTask(tasks, project)

        boolean isJava8Compatible = JavaVersion.current().isJava8Compatible()

        def systemPropertyConfigurer = { JavaForkOptions task ->
            def map = System.properties.findAll { entry ->
                entry.key.startsWith("grails.")
            }
            for(key in map.keySet()) {
                def value = map.get(key)
                if(value) {
                    def sysPropName = key.toString().substring(7)
                    task.systemProperty(sysPropName, value.toString())
                }
            }
            task.systemProperty Metadata.APPLICATION_NAME, project.name
            task.systemProperty Metadata.APPLICATION_VERSION, project.version
            task.systemProperty Metadata.APPLICATION_GRAILS_VERSION, grailsVersion
            task.systemProperty Environment.KEY, Environment.current.name ?: Environment.DEVELOPMENT.name
            task.systemProperty Environment.FULL_STACKTRACE, System.getProperty(Environment.FULL_STACKTRACE) ?: ""
            task.minHeapSize = "768m"
            task.maxHeapSize = "768m"
            if(!isJava8Compatible) {
                task.jvmArgs "-XX:PermSize=96m", "-XX:MaxPermSize=256m"
            }
            task.jvmArgs "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=3"
        }

        tasks.withType(Test).each systemPropertyConfigurer
        tasks.withType(JavaExec).each systemPropertyConfigurer

        project.sourceSets {
            main {
                groovy {
                    srcDirs = grailsSourceDirs
                    resources {
                        srcDirs = [
                                "$projectDir/src/main/resources",
                                "$projectDir/grails-app/conf",
                                "$projectDir/grails-app/views",
                                "$projectDir/grails-app/i18n"
                        ]
                    }
                }
            }
        }
    }


    @CompileStatic
    protected void configureConsoleTask(TaskContainer tasks, Project project) {
        def consoleConfiguration = project.configurations.create("console")
        def findMainClass = tasks.findByName('findMainClass')
        def consoleTask = createConsoleTask(project, tasks, consoleConfiguration)
        def shellTask = createShellTask(project, tasks, consoleConfiguration)

        findMainClass.doLast {
            def mainClassName = project.properties.get("mainClassName")
            consoleTask.args mainClassName
            shellTask.args mainClassName
            project.tasks.withType(ApplicationContextCommandTask) { ApplicationContextCommandTask task ->
                task.args mainClassName
            }
        }

        consoleTask.dependsOn(tasks.findByName('classes'), findMainClass)
        shellTask.dependsOn(tasks.findByName('classes'), findMainClass)
    }

    protected JavaExec createConsoleTask(Project project, TaskContainer tasks, Configuration configuration) {
        tasks.create("console", JavaExec) {
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            main = "grails.ui.console.GrailsSwingConsole"
        }
    }

    protected JavaExec createShellTask(Project project, TaskContainer tasks, Configuration configuration) {
        tasks.create("shell", JavaExec) {
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            main = "grails.ui.shell.GrailsShell"
            standardInput = System.in
        }
    }

    protected void enableFileWatch(Environment environment, Project project) {
        if (environment.isReloadEnabled()) {
//            configureWatchPlugin(project)

            project.configurations {
                agent
            }
            project.dependencies {
                agent "org.springframework:springloaded:1.2.1.RELEASE"
            }
            project.afterEvaluate(new AgentTasksEnhancer())
        }
    }

    @CompileStatic
    protected void registerFindMainClassTask(Project project) {
        def findMainClassTask = project.tasks.create(name: "findMainClass", type: FindMainClassTask, overwrite: true)
        findMainClassTask.mustRunAfter project.tasks.withType(GroovyCompile)
        def bootRepackageTask = project.tasks.findByName("bootRepackage")
        if(bootRepackageTask) {
            bootRepackageTask.dependsOn findMainClassTask
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    protected void enableNative2Ascii(Project project, grailsVersion) {
        for (SourceSet sourceSet in project.sourceSets) {
            project.tasks.getByName(sourceSet.processResourcesTaskName) { CopySpec task ->
                def grailsExt = project.extensions.getByType(GrailsExtension)
                task.filter( ReplaceTokens, tokens: [
                        'info.app.name': project.name,
                        'info.app.version': project.version,
                        'info.app.grailsVersion': grailsVersion
                    ]
                )
                task.from(sourceSet.resources) {
                    include '**/*.properties'
                    if(grailsExt.native2ascii) {
                        filter(EscapeUnicode)
                    }
                }
                task.from(sourceSet.resources) {
                    exclude '**/*.properties'
                }
                task.from(sourceSet.resources) {
                    include '**/*.groovy'
                }
            }
        }

    }

}
