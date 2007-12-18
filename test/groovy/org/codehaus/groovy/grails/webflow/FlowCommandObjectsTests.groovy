/**
 * Tests the functionality of command objects in web flows
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 12, 2007
 * Time: 2:02:11 PM
 * 
 */
package org.codehaus.groovy.grails.webflow

import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder

class FlowCommandObjectsTests extends AbstractGrailsTagAwareFlowExecutionTests{


   void testCommandObjectsInFlowAction() {
       request.addParameter("command1.one1", "foo")
       request.addParameter("command2.two1", "bar")
       //request.addParameter("command1.one2", "foo2")

       def viewSelection = startFlow()
       assertEquals "two",viewSelection.viewName
       assert viewSelection.model.one1
       assert viewSelection.model.two1

       def c1 = viewSelection.model.one1
       def c2 = viewSelection.model.two1
       assertEquals "foo", c1.one1
       assertEquals "bar", c2.two1

       assert c1.hasErrors()
       assert !c1.validate()
       assert c2.hasErrors()
       assert !c2.validate()       
   }

   void testCommandObjectsInFlowTransition() {
       request.addParameter("one1", "yes1")
       request.addParameter("one2", "yes2")
       //request.addParameter("command1.one2", "foo2")

       def viewSelection = startFlow()
       assertEquals "two",viewSelection.viewName
       viewSelection = signalEvent("go")

       def stuff = viewSelection.model.stuff

       assert stuff

        def c1 = stuff.one2
       assert c1
       assertEquals "yes1", c1.one1
       assertEquals "yes2", c1.one2

   }

   void testCommandObjectAutowiringInFlow() {
       def vs = startFlow()

       vs = signalEvent("else")
       vs = signalEvent("go")

       def cmd = vs.model.cmd

       assert cmd
       assert cmd.groovyPagesTemplateEngine
   }
   
   FlowDefinition getFlowDefinition() {
        new FlowBuilder("myFlow").flow {
            one {
                action { Command1 c1, Command2 c2 ->
                    [one1:c1, two1:c2]
                }
                on("success").to "two"


            }
            two {
                on("go") { Command1 c1 ->
                    flow.put('stuff',[one2:c1])
                }.to "end"
                on("else").to "three"

            }
            three {
               on("go") { AutoWireCommand1 c1 ->
                    flow.put('cmd', c1)
               }.to "end"
               
            }
            end()
        }
    }

}
class AutoWireCommand1 {
    def groovyPagesTemplateEngine
}
class Command1 {
    String one1
    String one2

    static constraints = {
        one2(blank:false, nullable:false)
    }
}
class Command2 {
    String two1
    String two2

    static constraints = {
        two2(blank:false, nullable:false)
    }    
}