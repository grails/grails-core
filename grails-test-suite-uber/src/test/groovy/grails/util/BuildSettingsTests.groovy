package grails.util

import grails.build.GrailsBuildListener
import groovy.mock.interceptor.StubFor

/**
 * Test case for {@link BuildSettings}.
 */
class BuildSettingsTests extends GroovyTestCase {
    private String userHome
    private String version
    private File defaultWorkPath
    private Map savedSystemProps

    protected void setUp() {
        def props = new Properties()
        new File("../build.properties").withInputStream { InputStream is ->
            props.load(is)
        }

        userHome = System.getProperty("user.home")
        version = props.getProperty("grails.version")
        MockBuildSettings.version = version
        defaultWorkPath = new File(System.getProperty("user.home") + "/.grails/" + version)

        savedSystemProps = new HashMap()
    }

    protected void tearDown() {
        // Restore any overridden system properties.
        savedSystemProps.each { String key, String value ->
            if (value == null) {
                System.clearProperty(key)
            }
            else {
                System.setProperty(key, value)
            }
        }
    }

    void testDefaultConstructor() {
        def cwd = new File(".").canonicalFile
        def settings = new MockBuildSettings()

        // Core properties first.
        assertEquals userHome, settings.userHome.path
        assertEquals cwd, settings.baseDir
        assertEquals version, settings.grailsVersion
        assertFalse settings.defaultEnv
        assertNull settings.grailsEnv
        assertNull settings.grailsHome

        // Project paths.
        assertEquals defaultWorkPath, settings.grailsWorkDir
        assertEquals new File("$defaultWorkPath/projects/.core"), settings.projectWorkDir
        assertEquals new File("${settings.projectWorkDir}/classes"), settings.classesDir
        assertEquals new File("${settings.projectWorkDir}/test-classes"), settings.testClassesDir
        assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("${settings.projectWorkDir}/plugins"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir

        // Dependencies.
        /*
        TODO Disabled for the moment until I can work out a reasonable way of doing this.
        assertTrue settings.compileDependencies.isEmpty()
        assertTrue settings.testDependencies.isEmpty()
        assertTrue settings.runtimeDependencies.isEmpty()

        // Set up a test "lib" directory and try again.
        def deleteLibDir = false
        def libDir = new File(cwd, "lib")
        if (!libDir.exists()) {
            libDir.mkdir()
            deleteLibDir = true
        }
        def libs = [ new File(libDir, "gwt.jar"), new File(libDir, "a.jar"), new File(libDir, "bcd.jar") ]
        libs.each { File file ->
            file.createNewFile()
        }

        // Check that the dependencies are picked up.
        try {
            settings = new BuildSettings()
            libs.each { File file ->
                assertTrue "Build missing compile dependency: $file", settings.compileDependencies.contains(libs[0])
                assertTrue "Build missing test dependency: $file", settings.testDependencies.contains(libs[0])
                assertTrue "Build missing runtime dependency: $file", settings.runtimeDependencies.contains(libs[0])
            }
        }
        finally {
            libs.each { it.delete() }
            if (deleteLibDir) libDir.delete()
        }
        */
    }

    void testGrailsHomeConstructor() {
        def cwd = new File(".").canonicalFile
        def grailsHome = new File(cwd, "my-grails")
        try {
            // Create the Grails home directory and "lib" and "dist"
            // directories inside it, otherwise the tests will bomb.
            grailsHome.mkdir()
            new File(grailsHome, "lib").mkdir()
            new File(grailsHome, "dist").mkdir()

            def settings = new MockBuildSettings(new File("my-grails"))

            // Core properties first.
            assertEquals userHome, settings.userHome.path
            assertEquals cwd, settings.baseDir
            assertEquals version, settings.grailsVersion
            assertEquals "my-grails", settings.grailsHome.path
            assertFalse settings.defaultEnv
            assertNull settings.grailsEnv

            // Project paths.
            assertEquals defaultWorkPath, settings.grailsWorkDir
            assertEquals new File("$defaultWorkPath/projects/.core"), settings.projectWorkDir
            assertEquals new File("${settings.projectWorkDir}/classes"), settings.classesDir
            assertEquals new File("${settings.projectWorkDir}/test-classes"), settings.testClassesDir
            assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
            assertEquals new File("${settings.projectWorkDir}/plugins"), settings.projectPluginsDir
            assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
        }
        finally {
            grailsHome.deleteDir()
        }
    }

    void testSystemPropertyOverride() {
        setSystemProperty("grails.project.work.dir", "work")
        setSystemProperty("grails.project.plugins.dir", "$userHome/my-plugins")

        def settings = new MockBuildSettings()

        // Project paths.
        assertEquals defaultWorkPath, settings.grailsWorkDir
        assertEquals new File("work"), settings.projectWorkDir
        assertEquals new File("${settings.projectWorkDir}/classes"), settings.classesDir
        assertEquals new File("${settings.projectWorkDir}/test-classes"), settings.testClassesDir
        assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("${userHome}/my-plugins"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
    }

    void testExplicitValues() {
        def settings = new MockBuildSettings()
        settings.grailsWorkDir = new File("workDir")
        settings.projectWorkDir = new File("projectDir")
        settings.projectPluginsDir = new File("target/pluginsDir")

        // Check that these values have been set.
        String defaultProjectWorkDir = "${defaultWorkPath}/projects/.core"
        assertEquals new File("workDir"), settings.grailsWorkDir
        assertEquals new File("projectDir"), settings.projectWorkDir
        assertEquals new File("${defaultProjectWorkDir}/classes"), settings.classesDir
        assertEquals new File("${defaultProjectWorkDir}/test-classes"), settings.testClassesDir
        assertEquals new File("${defaultProjectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("target/pluginsDir"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
        assertEquals new File("target").canonicalFile, settings.projectTargetDir

        // Load a configuration file and check that the values we set
        // explicitly haven't changed.
        settings.rootLoader = new URLClassLoader(new URL[0], getClass().classLoader)
        settings.loadConfig(new File("test/resources/grails-app/conf/BuildConfig.groovy"))

        assertEquals new File("workDir"), settings.grailsWorkDir
        assertEquals new File("projectDir"), settings.projectWorkDir
        assertEquals new File("build/classes"), settings.classesDir
        assertEquals new File("build/test-classes"), settings.testClassesDir
        assertEquals new File("projectDir/resources"), settings.resourcesDir
        assertEquals new File("target/pluginsDir"), settings.projectPluginsDir
        assertEquals new File("workDir/global-plugins"), settings.globalPluginsDir
        assertEquals new File("target").canonicalFile, settings.projectTargetDir
    }

    void testWorkDirIsBasedOnAppNameNotBaseDirName() {
        // GRAILS-6232
        def stubMetaData = new StubFor(Metadata)
        stubMetaData.demand.getInstance(2) {}
        stubMetaData.demand.getCurrent(2) {
            [getApplicationName: {'myappname'}, getApplicationVersion: {'1.1'}]
        }

        stubMetaData.use {
            def settings = new BuildSettings()
            settings.baseDir = new File("base/dir")
            assertEquals 'myappname', settings.projectWorkDir.name
        }
    }

    void testWorkDirIsDotCoreWhenCreatingNewApp() {
        // GRAILS-6232
        def stubMetaData = new StubFor(Metadata)
        stubMetaData.demand.getInstance(2) {}
        stubMetaData.demand.getCurrent(2) {
            [getApplicationName: {}, getApplicationVersion: {}]
        }

        stubMetaData.use {
            def settings = new BuildSettings()
            settings.baseDir = new File("base/dir")
            assertEquals '.core', settings.projectWorkDir.name
        }
    }

    void testSetBaseDir() {
        def settings = new MockBuildSettings()
        settings.baseDir = new File("base/dir")

        assertEquals new File("base/dir"), settings.baseDir
        assertEquals defaultWorkPath, settings.grailsWorkDir
        assertEquals new File("$defaultWorkPath/projects/.core"), settings.projectWorkDir
        assertEquals new File("${settings.projectWorkDir}/classes"), settings.classesDir
        assertEquals new File("${settings.projectWorkDir}/test-classes"), settings.testClassesDir
        assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("${settings.projectWorkDir}/plugins"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
    }

    void testLoadConfig() {
        setSystemProperty("grails.project.work.dir", "work")
        setSystemProperty("grails.project.plugins.dir", "$userHome/my-plugins")

        def settings = new MockBuildSettings()
        settings.rootLoader = new URLClassLoader(new URL[0], getClass().classLoader)
        settings.loadConfig(new File("test/resources/grails-app/conf/BuildConfig.groovy"))

        // Project paths.
        assertEquals defaultWorkPath, settings.grailsWorkDir
        assertEquals new File("work"), settings.projectWorkDir
        assertEquals new File("build/classes"), settings.classesDir
        assertEquals new File("build/test-classes"), settings.testClassesDir
        assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("${userHome}/my-plugins"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
    }

    void testLoadConfigNoFile() {
        setSystemProperty("grails.project.work.dir", "work")
        setSystemProperty("grails.project.plugins.dir", "$userHome/my-plugins")

        def settings = new MockBuildSettings()
        settings.loadConfig(new File("test/BuildConfig.groovy"))

        // Project paths.
        assertEquals defaultWorkPath, settings.grailsWorkDir
        assertEquals new File("work"), settings.projectWorkDir
        assertEquals new File("${settings.projectWorkDir}/classes"), settings.classesDir
        assertEquals new File("${settings.projectWorkDir}/test-classes"), settings.testClassesDir
        assertEquals new File("${settings.projectWorkDir}/resources"), settings.resourcesDir
        assertEquals new File("${userHome}/my-plugins"), settings.projectPluginsDir
        assertEquals new File("$defaultWorkPath/global-plugins"), settings.globalPluginsDir
    }

    private void setSystemProperty(String name, String value) {
        if (!savedSystemProps[name]) {
            savedSystemProps[name] = System.getProperty(name)
        }

        System.setProperty(name, value)
    }

    void testBuildListenersViaSystemProperty() {
        try {
            def config = new ConfigObject()
            config.grails.build.listeners = 'java.lang.String' // anything, just verify that the system property trumps.
            System.setProperty(BuildSettings.BUILD_LISTENERS, 'java.lang.Exception')

            def settings = new BuildSettings()
            settings.loadConfig(config)

            assertEquals(['java.lang.Exception'] as Object[], settings.buildListeners as List)
        } finally {
            System.clearProperty(BuildSettings.BUILD_LISTENERS)
        }
    }

    void testBuildListenersMultipleClassNames() {
        def config = new ConfigObject()
        config.grails.build.listeners = 'java.lang.String,java.lang.String'
        def settings = new BuildSettings()
        settings.loadConfig(config)

        assertEquals(['java.lang.String', 'java.lang.String'], settings.buildListeners as List)
    }

    void testBuildListenersCollection() {
        def config = new ConfigObject()
        config.grails.build.listeners = [String, String]
        def settings = new BuildSettings()
        settings.loadConfig(config)
        assertEquals([String, String], settings.buildListeners as List)

        config = new ConfigObject()
        config.grails.build.listeners = ['java.lang.String', 'java.lang.String']
        settings = new BuildSettings()
        settings.loadConfig(config)
        assertEquals(['java.lang.String', 'java.lang.String'], settings.buildListeners as List)
    }

    void testBuildListenersBadValue() {
        def config = new ConfigObject()
        config.grails.build.listeners = 1
        def settings = new BuildSettings()

        shouldFail(IllegalArgumentException) {
            settings.loadConfig(config)
        }
    }
}
class MockBuildSettings extends BuildSettings {

    MockBuildSettings() {
    }

    MockBuildSettings(File grailsHome) {
        super(grailsHome)
    }

    static version

    @Override protected loadBuildPropertiesFromClasspath(Properties buildProps) {
        buildProps['grails.version'] = version
    }


}
class BuildSettingsTestsGrailsBuildListener implements GrailsBuildListener {
    void receiveGrailsBuildEvent(String name, Object[] args) {}
}
