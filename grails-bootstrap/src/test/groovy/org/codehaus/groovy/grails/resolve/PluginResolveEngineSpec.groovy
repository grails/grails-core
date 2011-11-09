package org.codehaus.groovy.grails.resolve

import spock.lang.Specification
import grails.util.BuildSettings

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 11/9/11
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */
class PluginResolveEngineSpec extends Specification{

    def "Test that plugin-info obtains relevant plugin information in a plugin exists"() {
        given:"An instance of the resolve engine"
            def resolveEngine = systemUnderTest()

        when:"We resolve the 'feeds' plugin"
            def metadata = resolveEngine.resolvePluginMetadata("feeds", "1.5")

        then:"The correct metadata is obtained"
            metadata != null
            metadata.@name == 'feeds'
            metadata.@version == '1.5'
            metadata.title.text() == 'Render RSS/Atom feeds with a simple builder'
    }

    PluginResolveEngine systemUnderTest() {
        def settings = new BuildSettings()
        def dependencyManager = new IvyDependencyManager("test", "0.1", settings)
        dependencyManager.parseDependencies {
            repositories {
                grailsCentral()
            }
        }
        return new PluginResolveEngine(dependencyManager,settings)
    }
}
