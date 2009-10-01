package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.util.Message
import org.apache.ivy.util.MessageLogger
import org.apache.ivy.util.DefaultMessageLogger
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.GrailsUtil
import groovy.xml.MarkupBuilder
import org.apache.ivy.plugins.parser.m2.PomDependencyMgt
import org.apache.ivy.core.resolve.IvyNode

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class IvyDependencyManagerTests extends GroovyTestCase{

    protected void setUp() {
        System.metaClass.static.getenv = { String name -> "." }
    }

    protected void tearDown() {
        GroovySystem.metaClassRegistry.removeMetaClass(System) 
    }

    void testOverridePluginDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false] )
            runtime( [group:"opensymphony", name:"foocache", version:"2.4.1", transitive:false] )
            plugin("foo") {
                build "junit:junit:3.8.4"
            }
        }

        manager.parseDependencies("foo") {
            build "org.grails:grails-test:1.2"
            build "junit:junit:3.8.3"
        }

        final IvyNode[] buildDeps = manager.listDependencies("build")
        assertEquals 2, buildDeps.size()
        assertNotNull "grails-test should be a dependency", buildDeps.find { it.moduleId.name == "grails-test" }

        def junit = buildDeps.find { it.moduleId.name == "junit" }
        assertEquals "junit", junit.moduleId.name
        assertEquals "3.8.4", junit.id.revision
    }


    void testConfigurePluginDependenciesWithExclude() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)

        manager.parseDependencies {
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false] )
            runtime( [group:"opensymphony", name:"foocache", version:"2.4.1", transitive:false] )            
            plugin("foo") {
                excludes "junit"
            }
        }

        manager.parseDependencies("foo") {
            build "org.grails:grails-test:1.2"
            build "junit:junit:3.8.3"
        }

        final IvyNode[] buildDeps = manager.listDependencies("build")
        assertEquals 1, buildDeps.size()
        assertEquals "grails-test", buildDeps[0].moduleId.name
    }

    void testResolveApplicationDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            test "org.grails:grails-test:1.2"
        }
        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false] )
            test( [group:"junit", name:"junit", version:"3.8.2", transitive:true] )
        }

        manager.resolveApplicationDependencies()
        
    }

    void testGetApplicationDependencies() {
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            test "org.grails:grails-test:1.2"
        }
        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false] )
            test( [group:"junit", name:"junit", version:"3.8.2", transitive:true] )
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
            test "org.grails:grails-test:1.2"
        }

        manager.parseDependencies {
            inherits "test"
        }

        assertEquals 1, manager.listDependencies("test").size()
        assertFalse "application has only inherited dependencies!",manager.hasApplicationDependencies()

        manager.parseDependencies {
            inherits "test"

            runtime("opensymphony:foocache:2.4.1") {
                     excludes 'jms'
            }

        }

        assertTrue "application has dependencies!",manager.hasApplicationDependencies()

    }

    void testDynamicAddDependencyDescriptor() {

        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies {}

        manager.addPluginDependency("foo",[group:"org.grails", name:"grail-test", version:"1.2"])

        assertEquals 1, manager.listDependencies("runtime").size()
    }
    void testSerializerToMarkup() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            test "org.grails:grails-test:1.2"
        }


        // test simple exclude
        manager.parseDependencies {
            inherits "test"
            resolvers {
                grailsHome()
                mavenRepo "http://snapshots.repository.codehaus.org"
            }
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false],
                     [group:"junit", name:"junit", version:"3.8.2", transitive:true] )

            runtime("opensymphony:foocache:2.4.1") {
                     excludes 'jms'
            }
        }

        def w = new StringWriter()
        def builder = new MarkupBuilder(w)

        manager.serialize(builder)

        println w
        def xml = new XmlSlurper().parseText(w.toString())

        println xml
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
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false],
                     [group:"junit", name:"junit", version:"3.8.2", transitive:true] )
        }


        assertEquals 2, manager.listDependencies('runtime').size()
    }

    void testDefaultDependencyDefinition() {

        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        def grailsVersion = getCurrentGrailsVersion()
        manager.parseDependencies(IvyDependencyManager.getDefaultDependencies(grailsVersion))

        assertEquals 52, manager.listDependencies('runtime').size()
        assertEquals 55, manager.listDependencies('test').size()
        assertEquals 19, manager.listDependencies('build').size()
        assertEquals 3, manager.listDependencies('provided').size()

        // This should be a functional test since it relies on the Grails
        // JAR files being built. It also runs Ivy, which isn't ideal
        // in unit tests.
//        def report = manager.resolveDependencies()
//        assertFalse "dependency resolve should have no errors!",report.hasError()
    }

    def getCurrentGrailsVersion() {
        def props = new Properties()
        new File("./build.properties").withInputStream {
            props.load(it)
        }
        def grailsVersion = props.'grails.version'
        return grailsVersion
    }

    void testInheritanceAndExcludes() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);

          def settings = new BuildSettings()
          def manager = new IvyDependencyManager("test", "0.1",settings)
          settings.config.grails.test.dependency.resolution = {
              test "junit:junit:3.8.2"
          }
          // test simple exclude
          manager.parseDependencies {
               inherits('test') {
                   excludes 'junit'
               }
               runtime("opensymphony:oscache:2.4.1") {
                   excludes 'jms'
               }
          }

          assertEquals 1, manager.listDependencies("test").size()


    }

    void testInheritence() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);

        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        settings.config.grails.test.dependency.resolution = {
            test "junit:junit:3.8.2"
        }
        // test simple exclude
        manager.parseDependencies {
             inherits 'test'
             runtime("opensymphony:oscache:2.4.1") {
                 excludes 'jms'
             }
        }

        assertEquals 2, manager.listDependencies("test").size()



    }

    void testExcludes() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            runtime("opensymphony:oscache:2.4.1") {
               excludes 'jms'
            }
        }

        // test complex exclude
        manager.parseDependencies {
            runtime("opensymphony:oscache:2.4.1") {
                excludes group:'javax.jms',module:'jms'
            }
        }


    }
    void testResolve() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO); 
        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies TEST_DATA
        manager.resolveDependencies()        
    }

    void testListDependencies() {
        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies TEST_DATA
        assertEquals 12, manager.listDependencies("build").size()
        assertEquals 21, manager.listDependencies("runtime").size()
        assertEquals 22, manager.listDependencies("test").size()
    }



    void testParseDependencyDefinition() {
        def manager = new IvyDependencyManager("test", "0.1")

        manager.parseDependencies TEST_DATA

        assertNotNull manager.dependencies
        assertFalse "should have resolved some dependencies",manager.dependencies.isEmpty()

        def orgDeps = manager.getModuleRevisionIds("org.apache.ant")

        assertEquals "should have found 3 dependencies for the given organization", 3, orgDeps.size()

        ModuleRevisionId entry = orgDeps.find { ModuleRevisionId rev -> rev.name == 'ant-junit'}

        assertEquals "org.apache.ant", entry.organisation
        assertEquals "ant-junit", entry.name
        assertEquals "1.7.1", entry.revision

        def resolvers = manager.chainResolver.resolvers

        assertEquals 4, resolvers.size()

        assertTrue "should have a file system resolver",resolvers[0] instanceof FileSystemResolver
        assertEquals "mine", resolvers[0].name
        assertTrue "should resolve to grails home",resolvers[0].artifactPatterns[0].endsWith("lib/[artifact]-[revision].[ext]")
        assertTrue "grailsHome() should be a file system resolver",resolvers[1] instanceof FileSystemResolver
        assertTrue "grailsHome() should be a file system resolver",resolvers[2] instanceof FileSystemResolver

        ModuleRevisionId junit = manager.dependencies.find {  ModuleRevisionId m -> m.organisation == 'junit'}
    }


    static final TEST_DATA = {
                repositories {
                    flatDir name: 'mine', dirs: "lib"
                    grailsHome()
                    mavenCentral()
                }
                dependencies {

                    build "org.tmatesoft.svnkit:svnkit:1.2.0",
                          "org.apache.ant:ant-junit:1.7.1",
                          "org.apache.ant:ant-nodeps:1.7.1",
                          "org.apache.ant:ant-trax:1.7.1",
                          "radeox:radeox:1.0-b2",
                          "hsqldb:hsqldb:1.8.0.10",
                          "apache-tomcat:jasper-compiler:5.5.15",
                          "jline:jline:0.9.91",
                          "javax.servlet:servlet-api:2.5",
                          "javax.servlet:jsp-api:2.1",
                          "javax.servlet:jstl:1.1.2",
                          "xalan:serializer:2.7.1"

                    test "junit:junit:3.8.2"

                    runtime "apache-taglibs:standard:1.1.2",
                            "org.aspectj:aspectjweaver:1.6.2",
                            "org.aspectj:aspectjrt:1.6.2",
                            "cglib:cglib-nodep:2.1_3",
                            "commons-beanutils:commons-beanutils:1.8.0",
                            "commons-collections:commons-collections:3.2.1",
                            "commons-dbcp:commons-dbcp:1.2.2",
                            "commons-fileupload:commons-fileupload:1.2.1",
                            "commons-io:commons-io:1.4",
                            "commons-lang:commons-lang:2.4",
                            "javax.transaction:jta:1.1",
                            "log4j:log4j:1.2.15",
                            "net.sf.ehcache:ehcache:1.6.1",
                            "opensymphony:oscache:2.4.1",
                            "opensymphony:sitemesh:2.4",
                            "org.slf4j:jcl-over-slf4j:1.5.6",
                            "org.slf4j:slf4j-api:1.5.6",
                            "oro:oro:2.0.8",
                            "xpp3:xpp3_min:1.1.3.4.O"

                    runtime "commons-validator:commons-validator:1.3.1",
                            "commons-el:commons-el:1.0"
                            [transitive:false]
                }

    }
}
class DummyMavenAwareDependencyManager extends IvyDependencyManager {

    public DummyMavenAwareDependencyManager(String applicationName, String applicationVersion, BuildSettings settings) {
        super(applicationName, applicationVersion, settings);    
    }

    public List readDependenciesFromPOM() {
        return [
                [getGroupId:{"junit"}, getArtifactId:{"junit"}, getVersion:{"3.8.3"}, getScope:{"test"}] as PomDependencyMgt
        ]
    }

}
