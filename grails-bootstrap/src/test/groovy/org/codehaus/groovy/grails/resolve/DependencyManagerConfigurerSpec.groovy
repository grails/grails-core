package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class DependencyManagerConfigurerSpec extends Specification{

    void "Test create ivy dependency manager" () {
        when:"An ivy dependency manager is created from BuildSettings"
            final grailsHome = new File("..")
            def buildSettings = new BuildSettings(grailsHome)
            buildSettings.loadConfig()
            def dependencyManager = DependencyManagerConfigurer.createIvyDependencyManager(buildSettings)

        then:"The dependency manager is valid"
            dependencyManager != null
            dependencyManager instanceof IvyDependencyManager
    }
}
