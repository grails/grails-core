/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.gradle.plugin.core

import grails.plugins.GrailsVersionUtils
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.util.Metadata
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import nebula.plugin.extraconfigurations.ProvidedBasePlugin
import org.apache.tools.ant.filters.EscapeUnicode
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.DefaultCompositeFileTree
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.JavaForkOptions
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.grails.build.parsing.CommandLineParser
import org.grails.gradle.plugin.agent.AgentTasksEnhancer
import org.grails.gradle.plugin.commands.ApplicationContextCommandTask
import org.grails.gradle.plugin.commands.ApplicationContextScriptTask
import org.grails.gradle.plugin.model.GrailsClasspathToolingModelBuilder
import org.grails.gradle.plugin.run.FindMainClassTask
import org.grails.gradle.plugin.util.SourceSets
import org.grails.io.support.FactoriesLoaderSupport
import org.springframework.boot.gradle.SpringBootPluginExtension
import org.apache.tools.ant.taskdefs.condition.Os
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.repackage.RepackageTask

import javax.inject.Inject

/**
 * The main Grails gradle plugin implementation
 *
 * @since 3.0
 * @author Graeme Rocher
 */
class GrailsGradlePlugin extends GroovyPlugin {
    public static final String APPLICATION_CONTEXT_COMMAND_CLASS = "grails.dev.commands.ApplicationCommand"
    public static final String PROFILE_CONFIGURATION = "profile"

    protected static final List<String> CORE_GORM_LIBRARIES = ['async','core', 'simple', 'web', 'rest-client', 'gorm', 'gorm-validation', 'gorm-plugin-support','gorm-support', 'test-support', 'hibernate-core', 'gorm-test', 'rx', 'rx-plugin-support']
    // NOTE: mongodb, neo4j etc. should NOT be included here so they can be independently versioned
    protected static final List<String> CORE_GORM_PLUGINS = ['hibernate4','hibernate5']

    List<Class<Plugin>> basePluginClasses = [ProvidedBasePlugin, IntegrationTestGradlePlugin]
    List<String> excludedGrailsAppSourceDirs = ['migrations', 'assets']
    List<String> grailsAppResourceDirs = ['views', 'i18n', 'conf']
    private final ToolingModelBuilderRegistry registry

    @Inject
    GrailsGradlePlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry
    }

    @CompileStatic
    void apply(Project project) {
        // reset the environment to ensure it is resolved again for each invocation
        Environment.reset()

        if( project.tasks.findByName('compileGroovy') == null ) {
            super.apply(project)
        }

        configureProfile(project)

        applyDefaultPlugins(project)

        registerToolingModelBuilder(project, registry)

        registerGrailsExtension(project)

        applyBasePlugins(project)

        registerFindMainClassTask(project)

        configureGrailsBuildSettings(project)

        configureFileWatch(project)

        def grailsVersion = resolveGrailsVersion(project)

        enableNative2Ascii(project, grailsVersion)

        configureSpringBootExtension(project)

        configureAssetCompilation(project)

        configureConsoleTask(project)

        configureForkSettings(project, grailsVersion)

        configureGrailsSourceDirs(project)

        configureApplicationCommands(project)

        createBuildPropertiesTask(project)

        configureRunScript(project)

        configureRunCommand(project)

        configurePathingJar(project)
    }

    @CompileStatic
    protected void configureProfile(Project project) {
        def profileConfiguration = project.configurations.create(PROFILE_CONFIGURATION)

        profileConfiguration.incoming.beforeResolve() {
            if (!profileConfiguration.allDependencies) {
                addDefaultProfile(project, profileConfiguration)
            }
        }
    }


    @CompileStatic
    protected void applyDefaultPlugins(Project project) {
        def springBoot = project.extensions.findByType(SpringBootPluginExtension)
        if (!springBoot) {
            project.plugins.apply(SpringBootPlugin)
        }

        DependencyManagementPlugin dependencyManagementPlugin = project.plugins.findPlugin(DependencyManagementPlugin)
        if (dependencyManagementPlugin == null) {
            project.plugins.apply(DependencyManagementPlugin)
        }

        DependencyManagementExtension dme = project.extensions.findByType(DependencyManagementExtension)

        applyBomImport(dme, project)

        boolean isGorm61 = false
        boolean hasGormVersion = project.hasProperty('gormVersion')
        String gormVersion

        if(hasGormVersion) {
            gormVersion = project.properties['gormVersion']
            isGorm61 = GrailsVersionUtils.supportsAtLeastVersion(gormVersion, "6.1.0")
        }

        if (isGorm61) {
            project.afterEvaluate {
                DependencySet dependencies = project.configurations.getByName('testCompile').allDependencies
                boolean hasPluginTesting = false
                boolean hasGormTest = false
                dependencies.each {
                    if (it.name == "grails-plugin-testing") {
                        hasPluginTesting = true
                    }
                    if (it.name == "grails-datastore-gorm-test") {
                        hasGormTest = true
                    }
                }
                if (hasPluginTesting && !hasGormTest) {
                    project.dependencies.add "testCompile", "org.grails:grails-datastore-gorm-test:$gormVersion"
                }
            }
        }

        project.configurations.all( { Configuration configuration ->
            for(oldPluginExcludes in ['async', 'events', 'converters', 'gsp', 'testing']) {
                configuration.exclude(group:"org.grails", module:"grails-plugin-$oldPluginExcludes".toString())
            }

            if(hasGormVersion) {
                if(isGorm61) {
                    configuration.exclude(module:'grails-datastore-simple')
                }

                configuration.resolutionStrategy.eachDependency( { DependencyResolveDetails details ->
                    String dependencyName = details.requested.name
                    String group = details.requested.group
                    if(group == 'org.grails' &&
                            dependencyName.startsWith('grails-datastore')) {
                        for(suffix in GrailsGradlePlugin.CORE_GORM_LIBRARIES) {
                            if(dependencyName == "grails-datastore-$suffix") {
                                details.useVersion(gormVersion)
                                return
                            }
                        }
                    }
                    else if(group == 'org.grails.plugins' && GrailsGradlePlugin.CORE_GORM_PLUGINS.contains(dependencyName)) {
                        details.useVersion(gormVersion - '.RELEASE')
                    }
                } as Action<DependencyResolveDetails>)
            }
        } as Action<Configuration>)
    }

    private void applyBomImport(DependencyManagementExtension dme, project) {
        dme.imports({
            mavenBom("org.grails:grails-bom:${project.properties['grailsVersion']}")
        })
        dme.setApplyMavenExclusions(false)
    }

    protected String getDefaultProfile() {
        'web'
    }

    @CompileStatic
    void addDefaultProfile(Project project, Configuration profileConfig) {
        project.dependencies.add('profile', "org.grails.profiles:${System.getProperty("grails.profile") ?: defaultProfile}:")
    }

    protected Task createBuildPropertiesTask(Project project) {

        def resourcesDir = SourceSets.findMainSourceSet(project).output.resourcesDir
        def buildInfoFile = new File(resourcesDir, "META-INF/grails.build.info")


        def buildPropertiesTask = project.tasks.create("buildProperties")
        def buildPropertiesContents = ['grails.env': Environment.isSystemSet() ? Environment.current.name : Environment.PRODUCTION.name,
                                        'info.app.name': project.name,
                                        'info.app.version':  project.version instanceof Serializable ? project.version : project.version.toString(),
                                        'info.app.grailsVersion': project.properties.get('grailsVersion')]

        buildPropertiesTask.inputs.properties(buildPropertiesContents)
        buildPropertiesTask.outputs.file(buildInfoFile)
        buildPropertiesTask.doLast {
            project.buildDir.mkdirs()
            ant.mkdir(dir:buildInfoFile.parentFile)
            ant.propertyfile(file: buildInfoFile) {
                for(me in buildPropertiesTask.inputs.properties) {
                    entry key: me.key, value: me.value
                }
            }
        }

        project.afterEvaluate {
            TaskContainer tasks = project.tasks
            tasks.findByName("processResources")?.dependsOn(buildPropertiesTask)
        }
    }

    @CompileStatic
    protected void configureSpringBootExtension(Project project) {
        def springBoot = project.extensions.findByType(SpringBootPluginExtension)

        if(springBoot) {
            springBoot.providedConfiguration = ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME
        }
    }

    @CompileStatic
    protected void registerToolingModelBuilder(Project project, ToolingModelBuilderRegistry registry) {
        registry.register(new GrailsClasspathToolingModelBuilder())
    }

    @CompileStatic
    protected void applyBasePlugins(Project project) {
        for(Class<Plugin> cls in basePluginClasses) {
            project.plugins.apply(cls)
        }
    }

    protected GrailsExtension registerGrailsExtension(Project project) {
        project.extensions.add("grails", new GrailsExtension(project))
    }

    @CompileStatic
    protected void configureFileWatch(Project project) {
        def environment = Environment.current
        enableFileWatch(environment, project)
    }

    @CompileStatic
    protected String configureGrailsBuildSettings(Project project) {
        System.setProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
    }

    protected void configureApplicationCommands(Project project) {
        def applicationContextCommands = FactoriesLoaderSupport.loadFactoryNames(APPLICATION_CONTEXT_COMMAND_CLASS)
        for (ctxCommand in applicationContextCommands) {
            def taskName = GrailsNameUtils.getLogicalPropertyName(ctxCommand, "Command")
            def commandName = GrailsNameUtils.getScriptName(GrailsNameUtils.getLogicalName(ctxCommand, "Command"))
            project.tasks.create(taskName, ApplicationContextCommandTask) {
                classpath = project.sourceSets.main.runtimeClasspath + project.configurations.console
                command = commandName
                systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.name)
                if (project.hasProperty('args')) {
                    args(CommandLineParser.translateCommandline(project.args))
                }
            }
        }
    }

    protected void configureGrailsSourceDirs(Project project) {
        project.sourceSets {
            main {
                groovy {
                    srcDirs = resolveGrailsSourceDirs(project)
                }
                resources {
                    srcDirs = resolveGrailsResourceDirs(project)
                }
            }
        }
    }

    @CompileStatic
    protected List<File> resolveGrailsResourceDirs(Project project) {
        List<File> grailsResourceDirs = [project.file("src/main/resources")]
        for(f in grailsAppResourceDirs) {
            grailsResourceDirs.add(project.file("grails-app/${f}"))
        }
        grailsResourceDirs
    }

    @CompileStatic
    protected List<File> resolveGrailsSourceDirs(Project project) {
        List<File> grailsSourceDirs = []
        project.file("grails-app").eachDir { File subdir ->
            if (isGrailsSourceDirectory(subdir)) {
                grailsSourceDirs.add(subdir)
            }
        }
        grailsSourceDirs.add(project.file("src/main/groovy"))
        grailsSourceDirs
    }

    @CompileStatic
    protected boolean isGrailsSourceDirectory(File subdir) {
        def dirName = subdir.name
        !subdir.hidden && !dirName.startsWith(".") && !excludedGrailsAppSourceDirs.contains(dirName) && !grailsAppResourceDirs.contains(dirName)
    }

    protected String resolveGrailsVersion(Project project) {
        def grailsVersion = project.property('grailsVersion')

        if (!grailsVersion) {
            def grailsCoreDep = project.configurations.getByName('compile').dependencies.find { Dependency d -> d.name == 'grails-core' }
            grailsVersion = grailsCoreDep.version
        }
        grailsVersion
    }

    protected void configureAssetCompilation(Project project) {
        if (project.extensions.findByName('assets')) {
            project.assets {
                assetsPath = 'grails-app/assets'
                compileDir = 'build/assetCompile/assets'
            }
        }
    }

    @CompileStatic
    protected void configureForkSettings(Project project, grailsVersion) {
        boolean isJava8Compatible = JavaVersion.current().isJava8Compatible()

        def systemPropertyConfigurer = { String defaultGrailsEnv, JavaForkOptions task ->
            def map = System.properties.findAll { entry ->
                entry.key?.toString()?.startsWith("grails.")
            }
            for (key in map.keySet()) {
                def value = map.get(key)
                if (value) {
                    def sysPropName = key.toString().substring(7)
                    task.systemProperty(sysPropName, value.toString())
                }
            }
            task.systemProperty Metadata.APPLICATION_NAME, project.name
            task.systemProperty Metadata.APPLICATION_VERSION, project.version
            task.systemProperty Metadata.APPLICATION_GRAILS_VERSION, grailsVersion
            task.systemProperty Environment.KEY, defaultGrailsEnv
            task.systemProperty Environment.FULL_STACKTRACE, System.getProperty(Environment.FULL_STACKTRACE) ?: ""
            if(task.minHeapSize == null) {
                task.minHeapSize = "768m"
            }
            if(task.maxHeapSize == null) {
                task.maxHeapSize = "768m"
            }
            List<String> jvmArgs = task.jvmArgs
            if (!isJava8Compatible) {
                if(!jvmArgs.any { !it.startsWith('-XX:MaxPermSize')}) {
                    task.jvmArgs "-XX:PermSize=96m", "-XX:MaxPermSize=256m"
                }
            }
            task.jvmArgs "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=3"

            // Copy GRAILS_FORK_OPTS into the fork. Or use GRAILS_OPTS if no fork options provided
            // This allows run-app etc. to run using appropriate settings and allows users to provided
            // different FORK JVM options to the build options.
            def envMap = System.getenv()
            String opts = envMap.GRAILS_FORK_OPTS ?: envMap.GRAILS_OPTS
            if(opts) {
                task.jvmArgs opts.split(' ')
            }
        }

        TaskContainer tasks = project.tasks

        String grailsEnvSystemProperty = System.getProperty(Environment.KEY)
        tasks.withType(Test).each systemPropertyConfigurer.curry(grailsEnvSystemProperty ?: Environment.TEST.name)
        tasks.withType(JavaExec).each systemPropertyConfigurer.curry(grailsEnvSystemProperty ?: Environment.DEVELOPMENT.name)
    }


    @CompileStatic
    protected void configureConsoleTask(Project project) {
        def tasks = project.tasks
        def consoleConfiguration = project.configurations.create("console")
        def findMainClass = tasks.findByName('findMainClass')
        def consoleTask = createConsoleTask(project, tasks, consoleConfiguration)
        def shellTask = createShellTask(project, tasks, consoleConfiguration)

        findMainClass.doLast {
            def bootExtension = project.extensions.findByType(SpringBootPluginExtension)
            def mainClassName = bootExtension.mainClass
            if(mainClassName) {
                consoleTask.args mainClassName
                shellTask.args mainClassName
                project.tasks.withType(ApplicationContextCommandTask) { ApplicationContextCommandTask task ->
                    task.args mainClassName
                }
            }
            project.tasks.withType(ApplicationContextScriptTask) { ApplicationContextScriptTask task ->
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

            project.configurations {
                agent
            }
            project.dependencies {
                agent "org.springframework:springloaded"
                runtimeOnly "io.methvin:directory-watcher"
            }
            project.afterEvaluate(new AgentTasksEnhancer())
        }
    }

    @CompileStatic
    protected void registerFindMainClassTask(Project project) {
        TaskContainer taskContainer = project.tasks
        def findMainClassTask = taskContainer.findByName("findMainClass")
        if (findMainClassTask == null) {
            findMainClassTask = project.tasks.create(name: "findMainClass", type: FindMainClassTask, overwrite: true)
            findMainClassTask.mustRunAfter project.tasks.withType(GroovyCompile)
            def bootRepackageTask = project.tasks.findByName("bootRepackage")
            if (bootRepackageTask) {
                bootRepackageTask.dependsOn findMainClassTask
            }
        } else if (!FindMainClassTask.class.isAssignableFrom(findMainClassTask.class)) {
            def grailsFindMainClass = project.tasks.create(name: "grailsFindMainClass", type: FindMainClassTask)
            grailsFindMainClass.dependsOn(findMainClassTask)
            findMainClassTask.finalizedBy(grailsFindMainClass)
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    protected void enableNative2Ascii(Project project, grailsVersion) {
        project.afterEvaluate {
            SourceSet sourceSet = SourceSets.findMainSourceSet(project)

            def taskContainer = project.tasks

            taskContainer.getByName(sourceSet.processResourcesTaskName) { AbstractCopyTask task ->

                def grailsExt = project.extensions.getByType(GrailsExtension)
                def native2ascii = grailsExt.native2ascii
                if(native2ascii && grailsExt.native2asciiAnt && !taskContainer.findByName('native2ascii')) {
                    def destinationDir = ((ProcessResources) task).destinationDir
                    Task native2asciiTask = createNative2AsciiTask(taskContainer, project.file('grails-app/i18n'), destinationDir)
                    task.dependsOn(native2asciiTask)
                }


                def replaceTokens = [
                        'info.app.name'         : project.name,
                        'info.app.version'      : project.version?.toString(),
                        'info.app.grailsVersion': grailsVersion
                ]

                task.from(project.relativePath("src/main/templates")) {
                    into("META-INF/templates")
                }


                if (!native2ascii) {
                    task.from(sourceSet.resources) {
                        include '**/*.properties'
                        filter(ReplaceTokens, tokens: replaceTokens)
                    }
                }
                else if(!grailsExt.native2asciiAnt) {
                    task.from(sourceSet.resources) {
                        include '**/*.properties'
                        filter(ReplaceTokens, tokens: replaceTokens)
                        filter(EscapeUnicode)
                    }
                }

                task.from(sourceSet.resources) {
                    filter( ReplaceTokens, tokens: replaceTokens )
                    include '**/*.groovy'
                    include '**/*.yml'
                    include '**/*.xml'
                }

                task.from(sourceSet.resources) {
                    exclude '**/*.properties'
                    exclude '**/*.groovy'
                    exclude '**/*.yml'
                    exclude '**/*.xml'
                }
            }
        }

    }

    protected Task createNative2AsciiTask(TaskContainer taskContainer, src, dest) {
        def native2asciiTask = taskContainer.create('native2ascii')
        native2asciiTask.doLast {
            ant.native2ascii(src: src, dest: dest,
                    includes: "**/*.properties", encoding: "UTF-8")
        }
        native2asciiTask.inputs.dir(src)
        native2asciiTask.outputs.dir(dest)
        native2asciiTask
    }

    protected Jar createPathingJarTask(Project project, String name, Configuration...configurations) {
        project.tasks.create(name, Jar) { Jar task ->
            task.dependsOn(configurations)
            task.appendix = 'pathing'

            Set files = []
            configurations.each {
                files.addAll(it.files)
            }

            task.doFirst {
                manifest { Manifest manifest ->
                    manifest.attributes "Class-Path": files.collect { File file ->
                        file.toURI().toURL().toString().replaceFirst(/file:\/+/, '/')
                    }.join(' ')
                }
            }
        }
    }

    protected void configureRunScript(Project project) {
        project.tasks.create("runScript", ApplicationContextScriptTask) {
            classpath = project.sourceSets.main.runtimeClasspath + project.configurations.console
            systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            if (project.hasProperty('args')) {
                args(CommandLineParser.translateCommandline(project.args))
            }
        }
    }

    protected void configureRunCommand(Project project) {
        project.tasks.create("runCommand", ApplicationContextCommandTask) {
            classpath = project.sourceSets.main.runtimeClasspath + project.configurations.console
            systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            if (project.hasProperty('args')) {
                args(CommandLineParser.translateCommandline(project.args))
            }
        }
    }

    @CompileDynamic
    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        FileCollection classesDirs
        try {
            classesDirs = output?.classesDirs ?: project.files(new File(project.buildDir, "classes/main"))
        }
        catch(e) {
            classesDirs = output?.classesDir ? project.files(output.classesDir) : project.files(new File(project.buildDir, "classes/main"))
        }
        return classesDirs
    }

    protected void configurePathingJar(Project project) {
        project.afterEvaluate {
            ConfigurationContainer configurations = project.configurations
            Configuration runtime = configurations.getByName('runtime')
            Configuration console = configurations.getByName('console')

            if( project.plugins.findPlugin(WarPlugin) ) {
                def allTasks = project.tasks
                allTasks.withType(RepackageTask) { RepackageTask t ->
                    t.withJarTask = allTasks.findByName('war')
                }
            }
            else {
                def allTasks = project.tasks
                allTasks.withType(RepackageTask) { RepackageTask t ->
                    t.withJarTask = allTasks.findByName('jar')
                }
            }

            SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
            SourceSetOutput output = mainSourceSet?.output
            FileCollection mainFiles = resolveClassesDirs(output, project)

            Jar pathingJar = createPathingJarTask(project, "pathingJar", runtime)
            FileCollection pathingClasspath = project.files("${project.buildDir}/resources/main", "${project.projectDir}/gsp-classes", pathingJar.archivePath) + mainFiles

            Jar pathingJarCommand = createPathingJarTask(project, "pathingJarCommand", runtime, console)
            FileCollection pathingClasspathCommand = project.files("${project.buildDir}/resources/main", "${project.projectDir}/gsp-classes", pathingJarCommand.archivePath) + mainFiles

            GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)

            if (grailsExt.pathingJar && Os.isFamily(Os.FAMILY_WINDOWS)) {
                project.tasks.withType(JavaExec) { JavaExec task ->
                    if (task.name in ['console', 'shell'] || task instanceof ApplicationContextCommandTask || task instanceof ApplicationContextScriptTask) {
                        task.dependsOn(pathingJarCommand)
                        task.doFirst {
                            classpath = pathingClasspathCommand
                        }
                    } else {
                        task.dependsOn(pathingJar)
                        task.doFirst {
                            classpath = pathingClasspath
                        }
                    }
                }

            }
        }
    }



}
