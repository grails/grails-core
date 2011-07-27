/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.test.mixin.webflow

import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Assert

/**
 * A unit test mixin for testing Web Flow interactions
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class WebFlowUnitTestMixin extends ControllerUnitTestMixin {

    String stateTransition
    String lastEventName
    String lastTransitionName
    Map conversation = [:]
    Map flow = [:]
    Map flowMap = [:]

    @Override
    def <T> T mockController(Class<T> controllerClass) {
        super.mockController(controllerClass)
        controllerClass.metaClass.getFlow = {-> flow }
        controllerClass.metaClass.getConversation = {-> conversation }
        return controllerClass.newInstance()
    }

    def propertyMissing(String name) {
        def controller = webRequest.currentRequest.getAttribute(org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes.CONTROLLER)
        def webFlowName = name
        if (controller == null) {
            Assert.fail("No controller mocked. Call mockController(Class) first")
        }
        def webFlowClosure
        try {
            webFlowClosure = controller."$webFlowName"
        } catch (MissingPropertyException mpe) {
            Assert.fail("No flow named $name found in controller $controller")
        }
        if(webFlowClosure instanceof Closure) {

            flowMap = grails.test.mixin.webflow.WebFlowUnitTestSupport.translate(webFlowClosure, {
                lastEventName = it.event;
                lastTransitionName = it.transition
            })
            webFlowClosure.delegate = this
            return flowMap
        }
        return webFlowClosure
    }
    /**
     * Registers the end transition state of a web flow if it is returned as
     * <code>return success()</code>
     */
    protected Object methodMissing(String name, Object args) {
        if (lastEventName && flowMap[lastEventName].on."$name") {
            stateTransition = name
            return name
        }
        else if(lastEventName && lastTransitionName) {
            if(name == 'error') {
                stateTransition = lastEventName
                return name
            }
            else if(name == 'success' && flowMap[lastEventName].on?."$lastTransitionName"?.to) {
                stateTransition = flowMap[lastEventName].on?."$lastTransitionName"?.to
                return name
            }
        }
        throw new MissingMethodException(name, this.class, args)
    }
}
