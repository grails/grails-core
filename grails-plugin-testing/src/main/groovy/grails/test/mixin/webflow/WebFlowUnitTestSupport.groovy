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

/**
 * Support class for building a mock Web Flow. Based on https://gist.github.com/881935
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class WebFlowUnitTestSupport {

    static final String BUILD = "build"

    Map flowMap
    Map currentEvent
    String currentOnEvent
    String currentEventName

    Boolean done = false

    Closure setEventOnActionCallback

    static Map translate(Closure closure, Closure setEventOnActionCallback) {
        return new WebFlowUnitTestSupport(setEventOnActionCallback)."$BUILD"(closure)
    }

    WebFlowUnitTestSupport(Closure setEventOnActionCallback) {
        this.setEventOnActionCallback = setEventOnActionCallback
    }

    Object invokeMethod(String name, Object obj) {
        Object[] args = obj.class.isArray() ? obj : [obj] as Object[]

        if (!done) {
            if (name == BUILD) {
                return doBuild(name, args)
            }

            if (!isFlowInitialized()) {
                throw new IllegalArgumentException("Call to [$name] not supported here.")
            }

            if (!isCurrentEventInitialized()) {
                return handleEvent(name, args)
            }
        }

        MetaMethod metaMethod = metaClass.getMetaMethod(name, args)

        if (metaMethod == null) {
            throw new MissingMethodException(name, this.class, args)
        }

        return metaMethod.invoke(this, args)
    }

    Object invokeClosureNode(Object args) {
        Closure callable = args
        callable.delegate = this
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            return callable()
        } finally {
            callable.delegate = callable.owner
        }
    }

    Map doBuild(String name, Object[] args) {
        if (isFlowInitialized()) {
            throw new IllegalArgumentException("Call to [$name] not supported here. Must call $BUILD first.")
        }

        flowMap = [:]
        invokeClosureNode(args[0])

        done = true
        return flowMap
    }

    void handleEvent(String name, Object[] args) {
        currentEvent = [:]
        currentEventName = name

        if (args.length > 0 && args[0] instanceof Closure) {
            invokeClosureNode(args[0])
        }

        flowMap.put name, currentEvent
        currentEvent = null
        currentEventName = null
    }

    Boolean isFlowInitialized() {
        return flowMap != null
    }

    Boolean isCurrentEventInitialized() {
        return currentEvent != null
    }

    void action(Closure actionClosure) {
        currentEvent.action = wrapWithEventName(actionClosure)
    }

    Object on(String event) {
        return on(event, null)
    }

    Object on(String event, Closure closure) {
        if (!currentEvent.on) {
            currentEvent.on = [:]
        }

        currentOnEvent = event

        currentEvent.on."$event" = [:]

        if (closure) {
            currentEvent.on."$event".action = wrapWithEventName(closure)
        }

        return this
    }

    void to(String state) {
        if (!currentOnEvent) {
            throw new IllegalArgumentException("Call to [to] not supported here. Must call on first.")
        }
        currentEvent.on."$currentOnEvent".to = state
        currentOnEvent = null
    }

    void subflow(Object subflow) {
        currentEvent.subflow = wrapWithEventName(subflow)
    }

    Closure wrapWithEventName(Closure action) {
        String event = currentEventName
        String transition = currentOnEvent

        Closure wrap = {->
            setEventOnActionCallback.call(event:event, transition:transition)
            return action()
        }

        return wrap
    }
}
