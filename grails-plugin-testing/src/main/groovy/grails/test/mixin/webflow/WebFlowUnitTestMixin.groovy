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

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
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
    Boolean isOutput
    Boolean isInput
    Map conversation = [:]
    Map flow = [:]
    Map flowMap = [:]
    Map currentEvent = [:]
    Map inputParams = [:]

    @Override
    def <T> T mockController(Class<T> controllerClass) {
        super.mockController(controllerClass)
        def mc = GrailsMetaClassUtils.getExpandoMetaClass(controllerClass)
        mc.getFlow = {-> this.testState.flow }
        mc.getConversation = {-> this.testState.conversation }
        mc.getCurrentEvent = {-> this.testState.currentEvent }
        return controllerClass.newInstance()
    }

    def propertyMissing(String name) {
        def controller = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.CONTROLLER)
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

        if (webFlowClosure instanceof Closure) {
            flowMap = WebFlowUnitTestSupport.translate(webFlowClosure, {
                lastEventName = it.event
                lastTransitionName = it.transition
                isOutput = it.isOutput
                isInput = it.isInput
                inputParams = it.inputParams
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
        if (isOutput) {
            doOutput(name, args[0])
            return
        }
        if (isInput) {
            args ? doInput(name, args[0]) : doInput(name)
            return
        }
        if (lastEventName && flowMap[lastEventName].on."$name") {
            stateTransition = name
            return name
        }
        if (lastEventName && lastTransitionName) {
            if (name == 'error') {
                stateTransition = lastEventName
                return name
            }
            if (name == 'success' && flowMap[lastEventName].on?."$lastTransitionName"?.to) {
                stateTransition = flowMap[lastEventName].on?."$lastTransitionName"?.to
                return name
            }
        }
        throw new MissingMethodException(name, this.class, args)
    }

    protected doOutput(String name, Closure valueClosure) {
        if (currentEvent.attributes == null) {
            currentEvent.attributes = [:]
        }
        currentEvent.attributes[name] = valueClosure()
    }

    protected doOutput(String name, Map valueMap) {
        if (currentEvent.attributes == null) {
            currentEvent.attributes = [:]
        }
        currentEvent.attributes[name] = valueMap.value
    }

    protected doOutput(String name, Object value) {
        if (currentEvent.attributes == null) {
            currentEvent.attributes = [:]
        }
        currentEvent.attributes[name] = value
    }

    protected doInput(String name) {
        doInput (name, false, null)
    }

    protected doInput(String name, Closure valueClosure) {
        doInput(name, false, valueClosure)
    }

    protected doInput(String name, Object defaultValue) {
        doInput(name, false, defaultValue)
    }

    protected doInput(String name, Map map) {
        doInput(name, map.required, map.value)
    }

    protected doInput(String name, Boolean required, Closure valueClosure) {
        def value = valueClosure()
        doInput(name, required, value)
    }

    protected doInput(String name, Boolean required, Object defaultValue) {
        def value = inputParams[name] ?: defaultValue
        if (required && !value) {
            throw new MissingPropertyException("Missing required attribute $name")
        }
        flow[name] = value
    }
}
