package grails.io

import grails.util.BuildSettings
import spock.lang.Specification


class IOUtilsSpec extends Specification{

    void "Test findClassResource finds a class resource"() {
        expect:
        IOUtils.findClassResource(BuildSettings)
        IOUtils.findClassResource(BuildSettings).path.contains('grails-bootstrap')
    }

    void "Test findJarResource finds a the JAR resource"() {
        expect:
        IOUtils.findJarResource(Specification)
        IOUtils.findJarResource(Specification).path.endsWith('spock-core-1.3-groovy-2.5.jar!/')
    }
}
