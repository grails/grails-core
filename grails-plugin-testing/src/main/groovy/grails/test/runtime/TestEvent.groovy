/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime;

import groovy.transform.CompileStatic

/**
 * Event value object for the TestRuntime/TestPlugin system
 *  
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
/**
 * @author lari
 *
 */
@CompileStatic
class TestEvent {
    /**
     * reference to {@link TestRuntime} instance
     */
    TestRuntime runtime
    /**
     * event's name 
     */
    String name
    /**
     * named arguments for the event
     */
    Map<String, Object> arguments
    
    public <T> T getArgument(String name, Class<T> requiredType) {
        (T)arguments.get(name)
    }
    
    /**
     * this flag can be set in event handling to prevent delivery to other event handlers that 
     * follow the current handler
     */
    boolean stopDelivery
    /**
     * this flag can be set in event publishing to deliver and process the event immediately
     * the default behaviour is to defer event handling to the next round of the event handling 
     */
    boolean immediateDelivery
    /**
     * this flag reverses the normal order of event deliver to event handlers (listener)
     * for example this is used for "after" and "afterClass" event to reverse the order of handling compared to "before" and "beforeClass" events 
     */
    boolean reverseOrderDelivery
    /**
     * this contains the event that created this event . This is null for the first event.
     */
    TestEvent parentEvent
    /**
     * this flag is set after the event has been delivered
     */
    boolean delivered
}
