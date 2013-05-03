package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import spock.lang.Ignore
import spock.lang.Specification

class PluginResolveEngineSpec extends Specification {

    def "Test install-plugin no longer valid message"() {
        given:"An instance of the resolve engine"
            def resolveEngine = systemUnderTest()

        when:"We resolve the 'feeds' plugin"
            def sw = new StringWriter()
            resolveEngine.renderInstallInfo("feeds", '1.5', sw)
            def info = sw.toString()

        then:"The correct metadata is obtained"
            info == '''
Since Grails 2.3, it is no longer possible to install plugins using the install-plugin command.
Plugins must be declared in the grails-app/conf/BuildConfig.groovy file.

Example:
grails.project.dependency.resolution = {
   ...
   plugins {
      compile ":feeds:1.5"
   }
}
'''
    }

    def "Test that plugin-info obtains relevant plugin information in a plugin exists"() {
        given:"An instance of the resolve engine"
            def resolveEngine = systemUnderTest()

        when:"We resolve the 'feeds' plugin"
            def metadata = resolveEngine.resolvePluginMetadata("feeds", "1.4")

        then:"The correct metadata is obtained"
            metadata != null
            metadata.@name == 'feeds'
            metadata.@version == '1.4'
            metadata.title.text() == 'Render RSS/Atom feeds with a simple builder'
    }

    def "Test rendering plugin-info"() {
        given:"An instance of the resolve engine"
            def resolveEngine = systemUnderTest()

        when:"We resolve the 'feeds' plugin"
            def sw = new StringWriter()
            resolveEngine.renderPluginInfo("feeds", "1.4", sw)
            def info = sw.toString()

        then:"The correct metadata is obtained"
            info.contains 'Name: feeds	| Latest release: 1.4'
            info.contains 'Render RSS/Atom feeds with a simple builder'
            info.contains 'Author: Marc Palmer'
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
