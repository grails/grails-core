package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import grails.web.mapping.UrlMappingInfo
import org.springframework.core.io.ByteArrayResource
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class DoubleWildcardUrlMappingTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {



    void testDoubleWildcardWithMatchingController() {
        given:
        def holder = urlMappingsHolder
        assert webRequest

        when:
        def infos = holder.matchAll('/someOther/index')

        then:
        assert infos

        when:
        UrlMappingInfo info = infos[0]
        info.configure webRequest

        then:
        'someOther' == info.getControllerName()

        when:
        infos = holder.matchAll('/someOther/1+2')

        then:
        assert infos

        when:
        info = infos[0]
        info.configure webRequest

        then:
        'someOther' == info.getControllerName()
        '1+2' == info.getActionName()

    }

    void testDoubleWildcardInParam() {

        given:
        def holder = urlMappingsHolder
        assert webRequest

        when:
        request.addParameter("d", "1")
        def infos = holder.matchAll("/cow/wiki/show/2/doc/?d=1")
        infos[0].configure(webRequest)
        def c = new DoubleWildCardController()

        then:
        "doc/" == c.params.path
        "1" == c.params.d
    }

    void testDoubleWildCardMappingWithSuffix() {

        given:
        def m = urlMappingsHolder.urlMappings.find { it.controllerName == 'userImage'}
        assert m

        when:
        def info = m.match("/images/foo.jpg")
        info?.configure(webRequest)
        //assert !mappings[1].match("/stuff/image")
        then:
        info
        "userImage" == info.controllerName
        "download" == info.actionName


        when:
        "foo" == info.params.image

        info = m.match("/images/foo/bar.jpg")
        //assert !mappings[1].match("/stuff/image")
        info?.configure(webRequest)

        then:
        info
        "userImage" == info.controllerName
        "download" == info.actionName
        "foo/bar" == info.params.image
    }

    void testDoubleWildCardMatching() {

        given:
        def m = urlMappingsHolder.urlMappings.find { it.toString().startsWith("/components")}
        def m2 = urlMappingsHolder.urlMappings.find { it.toString().startsWith("/stuff")}
        assert m

        when:
        def info = m.match("/components/image")
        info.configure(webRequest)

        then:
        "components" == info.controllerName
        "image" == info.actionName
        !webRequest.params.path

        when:
        info = m.match("/components/image/")
        info.configure(webRequest)

        then:
        "components" == info.controllerName
        "image" == info.actionName
        '' == webRequest.params.path

        when:
        info = m.match("/components/image/foo.bar")
        assert info
        info.configure(webRequest)

        then:
        "components" == info.controllerName
        "image" == info.actionName
        'foo.bar' == webRequest.params.path

        when:
        info = m.match('/components/image/asdf/foo.bar')
        assert info
        info.configure(webRequest)

        then:
        "components" == info.controllerName
        "image" == info.actionName
        'asdf/foo.bar' == webRequest.params.path

        !m2.match("/stuff/image")

        when:
        info = m2.match("/stuff/image/foo.bar")
        assert info
        info.configure(webRequest)

        then:
        "components" == info.controllerName
        "image" == info.actionName
        'foo.bar' == webRequest.params.path
    }

    static class UrlMappings {
        static mappings = {
            "/components/image/$path**?" {
                controller = "components"
                action = "image"
            }
            "/stuff/image/$path**" {
                controller = "components"
                action = "image"
            }

            "/cow/$controller/$action?/$id?/$path**?"()

            "/$controller/$action?/$id?"()

            "/images/$image**.jpg" (controller: 'userImage', action: 'download')
            "/**"{
                controller = 'doubleWildcard'
                action = 'otherAction'
            }
            "500"(view:'/error')
        }
    }



}

class SomeOtherController {
    def index() {}
}

@grails.artefact.Artefact('Controller')
class DoubleWildCardController {
    def index(){ params.path }
    def otherAction() {}
}
