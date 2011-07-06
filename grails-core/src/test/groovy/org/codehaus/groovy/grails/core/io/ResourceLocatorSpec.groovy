package org.codehaus.groovy.grails.core.io

import spock.lang.Specification
import org.springframework.core.io.ResourceLoader
import org.codehaus.groovy.grails.support.StaticResourceLoader
import org.codehaus.groovy.grails.support.MockResourceLoader
import org.codehaus.groovy.grails.support.MockStringResourceLoader

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/6/11
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
class ResourceLocatorSpec extends Specification{
    void "test find simple URI"() {
        given: "Resource locator with mock resource loader"
            def loader = new MockStringResourceLoader()
            loader.registerMockResource("file:./web-app/css/main.css", "dummy contents")
            def resourceLocator = new MockResourceLocator(defaultResourceLoader: loader)
            resourceLocator.searchLocation = "."

        when: "An existing resource is queried"
            def res = resourceLocator.findResourceForURI("/css/main.css")

        then: "Make sure it is found"
            assert res != null

        when: "A non-existent resource is queried"
            res = resourceLocator.findResourceForURI("/css/notThere.css")

        then: "null is returned"
            res == null
    }

}
class MockResourceLocator extends DefaultResourceLocator {
    ResourceLoader defaultResourceLoader
}
