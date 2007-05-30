package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.io.*
import org.codehaus.groovy.grails.web.servlet.mvc.*


class UrlMappingTests extends AbstractGrailsControllerTests {

    def topLevelMapping = '''
mappings {
    "/competition/$action?"{
        controller = "competition"
    }

    "/survey/$action?"{
        controller = "survey"
    }

    "/$id?"{
        controller = "content"
        action = "view"
    }
}
'''

    void testReverseTopLevelMapping() {
       def res = new ByteArrayResource(topLevelMapping.bytes)

       def evaluator = new DefaultUrlMappingEvaluator()
       def mappings = evaluator.evaluateMappings(res)

       def holder = new DefaultUrlMappingsHolder(mappings)

        def reverse = holder.getReverseMapping("competition",null,null)

        assertEquals "/competition/foo", reverse.createURL("competition", "foo", null, "utf-8")
        assertEquals "/competition/foo?name=bob", reverse.createURL("competition", "foo", [name:"bob"], "utf-8")

        reverse = holder.getReverseMapping("competition","enter",[name:"bob"])

        assert reverse
        assertEquals "/competition/enter", reverse.createURL("competition", "enter", null, "utf-8")
        assertEquals "/competition/enter?name=bob", reverse.createURL("competition", "enter", [name:"bob"], "utf-8")

        reverse = holder.getReverseMapping("content", null,null)

        assert reverse
        assertEquals "/tsandcs", reverse.createURL(id:"tsandcs", "utf-8")
        assertEquals "/tsandcs?foo=bar", reverse.createURL(id:"tsandcs", foo:"bar", "utf-8")

        reverse = holder.getReverseMapping("content", null,[foo:"bar"])
        assert reverse
        assertEquals "/tsandcs", reverse.createURL(id:"tsandcs", "utf-8")
        assertEquals "/tsandcs?foo=bar", reverse.createURL(id:"tsandcs", foo:"bar", "utf-8")

    }

    void testTopLevelMapping() {
           def res = new ByteArrayResource(topLevelMapping.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)

           def info = holder.match("/competition/foo")
           assert info
           assertEquals "competition", info.controllerName

           info = holder.match("/survey/bar")
           assert info
           assertEquals "survey", info.controllerName

           info = holder.match("/tsandcs")

           assert info

           assertEquals "content", info.controllerName
           assertEquals "view", info.actionName

    }
}

