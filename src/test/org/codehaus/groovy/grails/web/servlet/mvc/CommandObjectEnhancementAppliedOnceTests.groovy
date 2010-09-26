package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.support.MockApplicationContext

/**
 * @author Graeme Rocher
 */
class CommandObjectEnhancementAppliedOnceTests extends GroovyTestCase{

    // test for GRAILS-6742
    void testThatCommandObjectEnhancementIsOnlyAppliedOnce() {
        def ctx = new MockApplicationContext()

        def t = new TestController()
        ctx.registerMockBean TestController.name, t
        def action = t.index

        assert WebMetaUtils.isCommandObjectAction(action) == true
        WebMetaUtils.createAndPrepareCommandObjectAction(t, action, "index", ctx)

        def fresh = new TestController()
        assert WebMetaUtils.isCommandObjectAction(fresh.index) == false
    }

    static class TestController {
        def index = {TestCommand cmd ->}
    }
    static class TestCommand {

    }
}
