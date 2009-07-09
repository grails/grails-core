package org.codehaus.groovy.grails.web.mapping

import org.springframework.core.io.ByteArrayResource

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 6, 2009
 */

public class AdditionalParamsMappingTests extends AbstractGrailsMappingTests {

    def mappingScript = '''
mappings {
    "/$controller/$action?/$id?"{
      constraints {
         // apply constraints here
      }
    }

    "/users/$id?/$action?" {
        controller = "user"
    }

}
'''

    void testMapping() {
        def res = new ByteArrayResource(mappingScript.bytes)
        def mappings = evaluator.evaluateMappings(res)

        def holder = new DefaultUrlMappingsHolder(mappings)

        UrlCreator creator = holder.getReverseMapping("user", "profile",[id:"bob"])

        assertEquals "/users/bob/profile", creator.createRelativeURL("user", "profile",[id:"bob"], "utf-8")

        creator = holder.getReverseMapping("user", "profile",[id:"bob", q:"test"])

        assertEquals "/users/bob/profile?q=test", creator.createRelativeURL("user", "profile",[id:"bob",q:"test"], "utf-8")

    }
}