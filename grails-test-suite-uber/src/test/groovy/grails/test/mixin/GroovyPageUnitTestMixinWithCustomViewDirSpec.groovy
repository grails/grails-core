package grails.test.mixin

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Issue
import spock.lang.Specification


class GroovyPageUnitTestMixinWithCustomViewDirSpec extends Specification implements GrailsWebUnitTest {

    Closure doWithConfig() {{ c ->
        def customViewDir = new File('.', 'src/test/resources/customviews')
        c.grails.gsp.view.dir = customViewDir.absolutePath
    }}
    
    @Issue('GRAILS=11543')
    void 'test rendering a template when grails.gsp.view.dir has been assigned a value'() {
        when:
        def result = render(template: '/demo/myTemplate')
        
        then:
        result == 'this is a custom template'
    }
}
