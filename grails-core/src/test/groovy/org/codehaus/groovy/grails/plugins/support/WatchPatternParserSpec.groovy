package org.codehaus.groovy.grails.plugins.support

import spock.lang.Specification

 /**
 * @author Graeme Rocher
 */
class WatchPatternParserSpec extends Specification{

    WatchPatternParser parser = new WatchPatternParser()

    void "Test concrete file parsing"() {
        when:
            def patterns = parser.getWatchPatterns(["file:./grails-app/conf/spring/resources.xml",
                                                    "file:./grails-app/conf/spring/resources.groovy"])

        then:
           patterns.size() == 2
           patterns[0].directory == null
           patterns[0].file != null
           patterns[0].extension == "xml"
           patterns[0].file.name == "resources.xml"
           patterns[0].matchesPath("./grails-app/conf/spring/resources.xml")

           patterns[1].directory == null
           patterns[1].file != null
           patterns[1].extension == "groovy"
           patterns[1].file.name == "resources.groovy"
           patterns[1].matchesPath("./grails-app/conf/spring/resources.groovy")

    }

    void "Test files in directory watch"() {
        when:
            def patterns = parser.getWatchPatterns(["file:./grails-app/conf/spring/*.xml",
                                                    "file:./grails-app/conf/spring/**/*.groovy"])

        then:
           patterns.size() == 2
           patterns[0].directory.path == './grails-app/conf/spring'
           patterns[0].extension == "xml"
           patterns[0].file == null
           patterns[0].matchesPath("./grails-app/conf/spring/resources.xml")

           patterns[1].directory.path == './grails-app/conf/spring'
           patterns[1].extension == "groovy"
           patterns[1].file == null
           patterns[1].matchesPath("./grails-app/conf/spring/resources.groovy")
    }

}
