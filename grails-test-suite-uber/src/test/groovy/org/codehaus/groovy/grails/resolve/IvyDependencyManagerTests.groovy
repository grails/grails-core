package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import groovy.xml.MarkupBuilder
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.apache.ivy.util.url.CredentialsStore

 /**
 * @author Graeme Rocher
 * @since 1.1
 */
class IvyDependencyManagerTests extends GroovyTestCase {

    protected void setUp() {
        System.metaClass.static.getenv = { String name -> "." }
    }

    protected void tearDown() {
        GroovySystem.metaClassRegistry.removeMetaClass(System)
    }

    void testUseOriginSetting() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            useOrigin true
        }
        assert manager.ivySettings.defaultUseOrigin == true
    }

    // test for GRAILS-7101
    void testDisableChecksums() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            checksums false
        }
        assert manager.ivySettings.getVariable('ivy.checksums') == ''
    }

    void testInheritRepositoryResolvers() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            repositories {
                inherit true
                ebr()
            }
        }

        manager.parseDependencies "myplugin", {
            repositories {
                mavenCentral()
            }
        }

        assert 3 == manager.chainResolver.resolvers.size()

        manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            repositories {
                inherit false
                ebr()
            }
        }

        manager.parseDependencies "myplugin", {
            repositories {
                mavenCentral()
            }
        }

        assert 2 == manager.chainResolver.resolvers.size()
    }
    void testChanging() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", classifier:"source"]) {
                    changing = true
                }
            }
        }

        def dep = manager.dependencyDescriptors.iterator().next()
        assert dep.changing
    }

    void testClassifier() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", classifier:"source"])
            }
        }

        ModuleRevisionId dep = manager.dependencies.iterator().next()

        assertEquals "source",dep.extraAttributes['classifier']
    }

    void testPluginResolve() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            repositories {
                mavenLocal()
            }
            plugins {
                runtime name:"easy", classifier:"plugin", version:"latest.integration"
            }
        }

        def report = manager.resolvePluginDependencies()
    }

    void testCheckPluginDependencyScope() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            plugins {
                runtime name:"one", version:"latest.integration"
                test name:"two", version:"latest.integration"
            }
        }

        EnhancedDefaultDependencyDescriptor dd = manager.getPluginDependencyDescriptor("one")
        assertNotNull "should have returned a dependency descriptor instance", dd

        assertTrue "should have included configuration", dd.isSupportedInConfiguration("runtime")
        assertFalse "should not have included configuration", dd.isSupportedInConfiguration("test ")
    }

    void testDeclarePluginDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            plugins {
                runtime "org.grails.plugins:feeds:1.5"
                runtime ":searchable:0.5.5"
                test ":functional-test:1.2"
                runtime name:"quartz", version:"0.5.6"
            }
        }

        assertEquals "Should not have any application JAR dependencies", 0, manager.dependencyDescriptors.size()
        assertEquals "Should not have any application JAR dependencies", 4, manager.pluginDependencyDescriptors.size()

        def deps = manager.pluginDependencyDescriptors
        EnhancedDefaultDependencyDescriptor dd = deps.find { DependencyDescriptor dep -> dep.dependencyId.name == 'feeds'}
        assertNotNull "should have defined feeds plugin dependency", dd
        assertEquals "org.grails.plugins", dd.dependencyId.organisation
        assertEquals "feeds", dd.dependencyId.name
        assertEquals "1.5", dd.getDependencyRevisionId().revision
        assertEquals "runtime", dd.scope

        dd = deps.find { DependencyDescriptor dep -> dep.dependencyId.name == 'searchable'}
        assertNotNull "should have defined searchable plugin dependency", dd
        assertEquals "org.grails.plugins", dd.dependencyId.organisation
        assertEquals "searchable", dd.dependencyId.name
        assertEquals "0.5.5", dd.getDependencyRevisionId().revision
        assertEquals "runtime", dd.scope

        dd = deps.find { DependencyDescriptor dep -> dep.dependencyId.name == 'quartz'}
        assertNotNull "should have defined searchable plugin dependency", dd
        assertEquals "org.grails.plugins", dd.dependencyId.organisation
        assertEquals "quartz", dd.dependencyId.name
        assertEquals "0.5.6", dd.getDependencyRevisionId().revision
        assertEquals "runtime", dd.scope
    }

    void testEbrResolver() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            repositories {
                ebr()
            }
        }

        assertEquals 2,manager.chainResolver.resolvers.size()
    }

    void testCredentials() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            credentials {
                realm = 'foo'
                host = 'bar'
                username = 'test'
                password = 'pass'
            }
        }

        def store = CredentialsStore.INSTANCE
        def creds = store.getCredentials('foo', 'bar')

        assertEquals 'test', creds.userName
        assertEquals 'pass', creds.passwd
    }

    void testUseBranch() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", branch:"jdk14"])
            }
        }

        ModuleRevisionId dep = manager.dependencies.iterator().next()
        assertEquals "jdk14",dep.branch
    }

    void testModuleConf() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", branch:"jdk14"]) {
                    dependencyConfiguration("oscache-runtime")
                }
            }
        }

        DependencyDescriptor dep = manager.dependencyDescriptors.iterator().next()

        assertEquals 1, dep.getModuleConfigurations().length
        def configs = dep.getDependencyConfigurations("runtime")
        assertEquals 1, configs.length
        assertEquals "oscache-runtime", configs[0]
    }

    void testWithoutModuleConf() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", branch:"jdk14"])
            }
        }

        DependencyDescriptor dep = manager.dependencyDescriptors.iterator().next()

        def configs = dep.getDependencyConfigurations("runtime")
        assertEquals 1, configs.length
        assertEquals "default", configs[0]
    }

    void testExportedDependenciesAndResolvers() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime group:"opensymphony", name:"oscache", version:"2.4.1"
                runtime group:"opensymphony", name:"oscache2", version:"2.4.2", export:true
                runtime group:"opensymphony", name:"oscache3", version:"2.4.3", export:false
                runtime "opensymphony:oscache4:2.4.5", [export: false]
            }
        }

        def dd = manager.dependencyDescriptors.find { DependencyDescriptor dd -> dd.dependencyRevisionId.name == 'oscache' }
        assert dd.exported : "should be an exported dependency"

        dd = manager.dependencyDescriptors.find { DependencyDescriptor d -> d.dependencyRevisionId.name == 'oscache2' }
        assert dd.exported : "should be an exported dependency"

        dd = manager.dependencyDescriptors.find { DependencyDescriptor d -> d.dependencyRevisionId.name == 'oscache3' }
        assert !dd.exported : "should be an exported dependency"

        dd = manager.dependencyDescriptors.find { DependencyDescriptor d -> d.dependencyRevisionId.name == 'oscache4' }
        assert !dd.exported : "should be an exported dependency"

        def list = manager.getExportedDependencyDescriptors('runtime')
        assertEquals 2, list.size()
    }

    void testOverridePluginDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false])
                runtime([group:"opensymphony", name:"foocache", version:"2.4.1", transitive:false])
                plugins {
                    runtime(":foo:1.5") {
                        excludes "junit"
                    }
                }
                build "junit:junit:4.8.1"
            }
        }

        manager.parseDependencies("foo") {
            dependencies {
                build "org.grails:grails-test:1.2"
                build "junit:junit:4.8.1"
            }
        }

        final IvyNode[] buildDeps = manager.listDependencies("build")
        assertEquals 2, buildDeps.size()
        assertNotNull "grails-test should be a dependency", buildDeps.find { it.moduleId.name == "grails-test" }

        def junit = buildDeps.find { it.moduleId.name == "junit" }
        assertEquals "junit", junit.moduleId.name
        assertEquals "4.8.1", junit.id.revision

        def dd = manager.dependencyDescriptors.find { DependencyDescriptor dd -> dd.dependencyRevisionId.name == 'junit' }
    }

    void testConfigurePluginDependenciesWithExclude() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false])
                runtime([group:"opensymphony", name:"foocache", version:"2.4.1", transitive:false])
            }
            plugins {
                runtime(":foo:1.0") {
                    excludes "junit"
                }
            }
        }

        manager.parseDependencies("foo") {
            dependencies {
                build "org.grails:grails-test:1.2"
                build "junit:junit:4.8.1"
            }
        }

        final IvyNode[] buildDeps = manager.listDependencies("build")
        assertEquals 1, buildDeps.size()
        assertEquals "grails-test", buildDeps[0].moduleId.name
    }

    void testIsPluginConfiguredByApplication() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1", settings)

        manager.parseDependencies {
            plugins { runtime(":foo:1.0") }
        }

        manager.parseDependencies("a-plugin") {
            plugins { runtime(":bar:1.0") }
        }

        assertTrue manager.isPluginConfiguredByApplication("foo")
        assertFalse manager.isPluginConfiguredByApplication("bar")
    }

    void testDependenciesWithGString() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        def springDMVersion = '2.0.0.M1'
        manager.parseDependencies {
            dependencies {
                build("org.springframework.osgi:spring-osgi-core:$springDMVersion",
                      "org.springframework.osgi:spring-osgi-extender:$springDMVersion",
                      "org.springframework.osgi:spring-osgi-io:$springDMVersion",
                      "org.springframework.osgi:spring-osgi-web:$springDMVersion",
                      "org.springframework.osgi:spring-osgi-web-extender:$springDMVersion") {
                            transitive = false
                        }
            }
        }

        assertEquals 5, manager.listDependencies("build").size()
    }

    void testResolveApplicationDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "org.grails:grails-test:1.2"
            }
        }
        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false])
                test([group:"junit", name:"junit", version:"4.8.1", transitive:true])
            }
        }

        manager.resolveApplicationDependencies()
    }

    void testGetApplicationDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "org.grails:grails-test:1.2"
            }
        }
        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false])
                test([group:"junit", name:"junit", version:"4.8.1", transitive:true])
            }
        }

        assertEquals 2, manager.getApplicationDependencyDescriptors().size()
        assertEquals 1, manager.getApplicationDependencyDescriptors('runtime').size()
        assertEquals 1, manager.getApplicationDependencyDescriptors('test').size()
        assertEquals 0, manager.getApplicationDependencyDescriptors('build').size()
    }

    void testReadMavenPom() {
        def settings = new BuildSettings()
        def manager = new DummyMavenAwareDependencyManager("test", "0.1",settings)

        assertFalse "shouldn't be reading POM", manager.readPom

        manager.parseDependencies {
            pom true
        }

        assertTrue "should be reading POM", manager.readPom
        def deps = manager.listDependencies("test")
        assertEquals 1, deps.size()
        assertEquals "junit", deps[0].moduleId.name
    }

    void testHasApplicationDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "org.grails:grails-test:1.2"
            }
        }

        manager.parseDependencies {
            inherits "test"
        }

        assertEquals 1, manager.listDependencies("test").size()
        assertFalse "application has only inherited dependencies!",manager.hasApplicationDependencies()

        manager.parseDependencies {
            inherits "test"

            dependencies {
                runtime("opensymphony:foocache:2.4.1") {
                    excludes 'jms'
                }
            }
        }

        assertTrue "application has dependencies!",manager.hasApplicationDependencies()
    }

    void testSerializerToMarkup() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "org.grails:grails-test:1.2"
            }
        }

        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            resolvers {
                grailsHome()
                mavenRepo "http://snapshots.repository.codehaus.org"
            }
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false],
                        [group:"junit", name:"junit", version:"4.8.1", transitive:true])

                runtime("opensymphony:foocache:2.4.1") {
                    excludes 'jms'
                }
            }
        }

        def w = new StringWriter()
        def builder = new MarkupBuilder(w)

        manager.serialize(builder)

        def xml = new XmlSlurper().parseText(w.toString())
        def dependencies = xml.dependency

        assertEquals 3, dependencies.size()

        def oscache = dependencies.find { it.@name == 'oscache' }

        assertEquals 'opensymphony', oscache.@group.text()
        assertEquals 'oscache', oscache.@name.text()
        assertEquals '2.4.1', oscache.@version.text()
        assertEquals 'runtime', oscache.@conf.text()
        assertEquals 'false', oscache.@transitive.text()

        def foocache = dependencies.find { it.@name == 'foocache' }

        assertEquals 'opensymphony', foocache.@group.text()
        assertEquals 'foocache', foocache.@name.text()
        assertEquals '2.4.1', foocache.@version.text()
        assertEquals 'runtime', foocache.@conf.text()
        assertEquals 'true', foocache.@transitive.text()

        assertEquals 'jms', foocache.excludes.@name.text()
        assertEquals '*', foocache.excludes.@group.text()

        // should not include inherited dependencies
        def inherited = dependencies.find { it.@name == 'grails-test' }

        assertEquals 0, inherited.size()
    }

    void testMapSyntaxForDependencies() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            dependencies {
                runtime([group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false],
                        [group:"junit", name:"junit", version:"4.8.1", transitive:true])
            }
        }

        assertEquals 2, manager.listDependencies('runtime').size()
    }

    void testDefaultDependencyDefinition() {

        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        def grailsVersion = getCurrentGrailsVersion()
        manager.parseDependencies(new GrailsCoreDependencies(grailsVersion).createDeclaration())

        manager.parseDependencies {
            inherits "global"
        }

        assertTrue("all default dependencies should be inherited", manager.dependencyDescriptors.every { it.inherited == true })
        assertEquals 53, manager.dependencyDescriptors.findAll { it.scope == 'compile'}.size()
        assertEquals 14, manager.dependencyDescriptors.findAll { it.scope == 'runtime'}.size()
        assertEquals 4, manager.dependencyDescriptors.findAll { it.scope == 'test'}.size()
        assertEquals 22, manager.dependencyDescriptors.findAll { it.scope == 'build'}.size()
        assertEquals 3, manager.dependencyDescriptors.findAll { it.scope == 'provided'}.size()
        assertEquals 3, manager.dependencyDescriptors.findAll { it.scope == 'docs'}.size()
    }

    void testDefaultDependencyDefinitionWithDefaultDependenciesProvided() {

        def settings = new BuildSettings()
        def grailsVersion = getCurrentGrailsVersion()

        def manager = new IvyDependencyManager("project", "0.1",settings)
        def defaultDependencyClosure = settings.coreDependencies.createDeclaration()
        manager.parseDependencies {
            defaultDependenciesProvided true
            defaultDependencyClosure.delegate = delegate
            defaultDependencyClosure()
        }

        assertEquals 0, manager.dependencyDescriptors.findAll { it.scope == 'compile'}.size()
        assertEquals 0, manager.dependencyDescriptors.findAll { it.scope == 'runtime'}.size()
        assertEquals 4, manager.dependencyDescriptors.findAll { it.scope == 'test'}.size()
        assertEquals 22, manager.dependencyDescriptors.findAll { it.scope == 'build'}.size()
        assertEquals 70, manager.dependencyDescriptors.findAll { it.scope == 'provided'}.size()
        assertEquals 3, manager.dependencyDescriptors.findAll { it.scope == 'docs'}.size()

        manager = new IvyDependencyManager("project", "0.1",settings)
        defaultDependencyClosure = settings.coreDependencies.createDeclaration()
        manager.parseDependencies {
            defaultDependenciesProvided false
            defaultDependencyClosure.delegate = delegate
            defaultDependencyClosure()
        }

        assertEquals 53, manager.dependencyDescriptors.findAll { it.scope == 'compile'}.size()
        assertEquals 14, manager.dependencyDescriptors.findAll { it.scope == 'runtime'}.size()
        assertEquals 4, manager.dependencyDescriptors.findAll { it.scope == 'test'}.size()
        assertEquals 22, manager.dependencyDescriptors.findAll { it.scope == 'build'}.size()
        assertEquals 3, manager.dependencyDescriptors.findAll { it.scope == 'provided'}.size()
        assertEquals 3, manager.dependencyDescriptors.findAll { it.scope == 'docs'}.size()
    }

    def getCurrentGrailsVersion() {
        def props = new Properties()
        def file = new File("../build.properties")
        if (!file.exists()) file = new File("build.properties")
        file.withInputStream {
            props.load(it)
        }
        def grailsVersion = props.'grails.version'
        return grailsVersion
    }

    void testInheritanceAndExcludes() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)

        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "junit:junit:4.8.1"
            }

        }
        // test simple exclude
        manager.parseDependencies {
            inherits('test') {
                excludes 'junit'
            }
            dependencies {
                runtime("commons-lang:commons-lang:2.4") {
                }
            }
        }

        assertEquals 1, manager.listDependencies("test").size()
    }

    void testInheritence() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)

        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            dependencies {
                test "junit:junit:4.8.1"
            }
        }
        // test simple exclude
        manager.parseDependencies {
            inherits 'test'
            dependencies {
                runtime("commons-lang:commons-lang:2.4") {
                }
            }
        }

        assertEquals 2, manager.listDependencies("test").size()
    }

    void testExcludes() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            dependencies {
                runtime("opensymphony:oscache:2.4.1") {
                    excludes 'jms'
                }
            }
        }

        DefaultDependencyDescriptor dd = manager.getDependencyDescriptors().iterator().next()
        ArtifactId aid = createExcludeArtifactId("jms")
        assertTrue "should have contained exclude",dd.doesExclude(['runtime'] as String[], aid)
        aid = createExcludeArtifactId("jdbc")
        assertFalse "should not contain exclude", dd.doesExclude(['runtime'] as String[], aid)

        manager = new IvyDependencyManager("test", "0.1")
        // test complex exclude
        manager.parseDependencies {
            dependencies {
                runtime("opensymphony:oscache:2.4.1") {
                    excludes group:'javax.jms',name:'jms'
                }
            }
        }

        dd = manager.getDependencyDescriptors().iterator().next()
        aid = createExcludeArtifactId("jms", 'javax.jms')
        assertTrue "should have contained exclude",dd.doesExclude(['runtime'] as String[], aid)
    }

    void testTranstiveWithPlugin() {
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            plugins {
                runtime(":feeds:1.5") {
                    transitive = false
                }
            }
        }
        DefaultDependencyDescriptor dd = manager.getPluginDependencyDescriptors().iterator().next()
        assertFalse "should not be transtive",dd.isTransitive()
    }

    void testExcludesWithPlugin() {
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            plugins {
                runtime(":feeds:1.5") {
                    excludes 'jms'
                }
            }
        }
        DefaultDependencyDescriptor dd = manager.getPluginDependencyDescriptors().iterator().next()
        ArtifactId aid = createExcludeArtifactId("jms")
        assertTrue "should have contained exclude",dd.doesExclude(['runtime'] as String[], aid)
    }

    protected ArtifactId createExcludeArtifactId(String excludeName, String group = PatternMatcher.ANY_EXPRESSION) {
        def mid = ModuleId.newInstance(group, excludeName)
        return new ArtifactId(
            mid, PatternMatcher.ANY_EXPRESSION,
            PatternMatcher.ANY_EXPRESSION,
            PatternMatcher.ANY_EXPRESSION)
    }

    void testResolve() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO)
        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies TEST_DATA
        manager.resolveDependencies()
    }

    void testParseDependencyDefinition() {
        def manager = new IvyDependencyManager("test", "0.1")

        manager.parseDependencies TEST_DATA

        assertNotNull manager.dependencies
        assertFalse "should have resolved some dependencies",manager.dependencies.isEmpty()

        def orgDeps = manager.getModuleRevisionIds("org.apache.ant")
        assertEquals "should have found 2 dependencies for the given organization", 2, orgDeps.size()

        ModuleRevisionId entry = orgDeps.find { ModuleRevisionId rev -> rev.name == 'ant-junit'}
        assertEquals "org.apache.ant", entry.organisation
        assertEquals "ant-junit", entry.name
        assertEquals "1.8.2", entry.revision

        def resolvers = manager.chainResolver.resolvers
        assertEquals 6, resolvers.size()

        assertTrue "should have a file system resolver",resolvers[0] instanceof FileSystemResolver
        assertEquals "mine", resolvers[0].name
        assertTrue "should resolve to grails home",resolvers[0].artifactPatterns[0].endsWith("lib/[module]-[revision](-[classifier]).[ext]")
        assertTrue "grailsHome() should be a file system resolver",resolvers[1] instanceof FileSystemResolver
        assertTrue "grailsHome() should be a file system resolver",resolvers[2] instanceof FileSystemResolver

        ModuleRevisionId junit = manager.dependencies.find {  ModuleRevisionId m -> m.organisation == 'junit'}
    }

    // Having the dependencies come from config can change the method resolution semantics
    // so this test verifies that everything still works in this context
    void testParseDependencyDefinitionFromConfig() {
        def config = new ConfigSlurper().parse("""
            dependencyConfig = {
                dependencies {
                    runtime("opensymphony:oscache:2.4.1") {
                        excludes 'jms'
                    }
                }
            }
        """)

        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies(config.dependencyConfig)

        DefaultDependencyDescriptor dd = manager.getDependencyDescriptors().iterator().next()
        ArtifactId aid = createExcludeArtifactId("jms")
        assertTrue "should have contained exclude",dd.doesExclude(['runtime'] as String[], aid)
        aid = createExcludeArtifactId("jdbc")
        assertFalse "should not contain exclude", dd.doesExclude(['runtime'] as String[], aid)

        manager = new IvyDependencyManager("test", "0.1")
        // test complex exclude
        manager.parseDependencies {
            dependencies {
                runtime("opensymphony:oscache:2.4.1") {
                    excludes group:'javax.jms',name:'jms'
                }
            }
        }

        dd = manager.getDependencyDescriptors().iterator().next()
        aid = createExcludeArtifactId("jms", 'javax.jms')
        assertTrue "should have contained exclude",dd.doesExclude(['runtime'] as String[], aid)
    }

    void testCreateModuleDescriptor() {
        def manager = new IvyDependencyManager("test", "0.1")
        def md = manager.createModuleDescriptor()

        assert md.moduleRevisionId.organisation == "org.grails.internal"
        assert md.moduleRevisionId.name == "test"
        assert md.moduleRevisionId.revision == "0.1"
    }

    static final TEST_DATA = {
        repositories {
            flatDir name: 'mine', dirs: "lib"
            grailsHome()
            mavenCentral()
        }
        dependencies {

            build "org.tmatesoft.svnkit:svnkit:1.2.0",
                  "org.apache.ant:ant-junit:1.8.2",
                  "org.apache.ant:ant-trax:1.7.1",
                  "org.grails:grails-radeox:1.0-b4",
                  "com.h2database:h2:1.2.144",
                  "apache-tomcat:jasper-compiler:5.5.15",
                  "jline:jline:0.9.94",
                  "javax.servlet:servlet-api:2.5",
                  "javax.servlet:jsp-api:2.1",
                  "javax.servlet:jstl:1.1.2",
                  "xalan:serializer:2.7.1",
                  "test:test:0.5"

            test "junit:junit:4.8.1"

            runtime "apache-taglibs:standard:1.1.2",
                    "org.aspectj:aspectjweaver:1.6.2",
                    "org.aspectj:aspectjrt:1.6.2",
                    "cglib:cglib-nodep:2.1_3",
                    "commons-beanutils:commons-beanutils:1.8.0",
                    "commons-collections:commons-collections:3.2.1",
                    "commons-dbcp:commons-dbcp:1.3",
                    "commons-fileupload:commons-fileupload:1.2.1",
                    "commons-io:commons-io:1.4",
                    "commons-lang:commons-lang:2.4",
                    "javax.transaction:jta:1.1",
                    "log4j:log4j:1.2.16",
                    "net.sf.ehcache:ehcache:1.6.1",
                    "opensymphony:sitemesh:2.4",
                    "org.slf4j:jcl-over-slf4j:1.5.6",
                    "org.slf4j:slf4j-api:1.5.6",
                    "oro:oro:2.0.8",
                    "xpp3:xpp3_min:1.1.3.4.O"

            runtime "commons-validator:commons-validator:1.3.1",
                    "commons-el:commons-el:1.0"

            [transitive: false]
        }
    }
}

class DummyMavenAwareDependencyManager extends IvyDependencyManager {

    DummyMavenAwareDependencyManager(String applicationName, String applicationVersion, BuildSettings settings) {
        super(applicationName, applicationVersion, settings)
    }

    DependencyDescriptor[] readDependenciesFromPOM() {
        ModuleId moduleId = new ModuleId("junit", "junit")
        ModuleRevisionId moduleRevisionId = new ModuleRevisionId(moduleId, "4.8.1")
        DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor(moduleRevisionId, false)
        dependencyDescriptor.addDependencyConfiguration("test", "")

        [dependencyDescriptor] as DependencyDescriptor[]
    }
}
