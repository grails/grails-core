package grails.io

import grails.util.BuildSettings
import spock.lang.Specification


class IOUtilsSpec extends Specification{

    void "Test findClassResource finds a class resource"() {
        expect:
        IOUtils.findClassResource(BuildSettings)
        IOUtils.findClassResource(BuildSettings).path.contains('grails-bootstrap')
    }

    void "Test findJarResource finds a JAR resource"() {
        expect:
        IOUtils.findJarResource(Specification)
        IOUtils.findJarResource(Specification).path.endsWith('spock-core-2.3-groovy-4.0.jar!/')
    }
}
