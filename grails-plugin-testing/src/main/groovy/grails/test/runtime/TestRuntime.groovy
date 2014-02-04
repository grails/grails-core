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
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * TestRuntime is the container for the test runtime state
 * 
 * it has methods for getting and setting values to the registry and for sending events
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class TestRuntime {
    Set<String> features
    List<TestPlugin> plugins
    private Map<String, Object> registry = [:]
    private boolean runtimeClosed = false
    private boolean shared
    
    public TestRuntime(Set<String> features, List<TestPlugin> plugins, boolean shared) {
        changeFeaturesAndPlugins(features, plugins)
        this.@shared=shared
    }
    
    public boolean isShared() {
        return this.@shared
    }
    
    public void changeFeaturesAndPlugins(Set<String> features, List<TestPlugin> plugins) {
        this.features = new LinkedHashSet<String>(features).asImmutable()
        this.plugins =  new ArrayList<TestPlugin>(plugins).asImmutable()
    }
    
    public Object getValue(String name, Map callerInfo = [:]) {
        if(!containsValueFor(name)) {
            publishEvent("valueMissing", [name: name, callerInfo: callerInfo], [immediateDelivery: true])
        }
        getValueIfExists(name, callerInfo)
    }
    
    public Object getValueIfExists(String name, Map callerInfo = [:]) {
        Object val = registry.get(name)
        if(val instanceof LazyValue) {
            return val.get(callerInfo)
        } else {
            return val
        }
    }
    
    public Object getValueOrCreate(String name, Closure valueCreator) {
        if(containsValueFor(name)) {
            return getValue(name)
        } else {
            Object value = valueCreator.call()
            putValue(name, value)
            return value
        }
    }
    
    public boolean containsValueFor(String name) {
        registry.containsKey(name)
    }
    
    public Object removeValue(String name) {
        Object value = registry.remove(name)
        publishEvent("valueRemoved", [name: name, value: value, lazy: (value instanceof LazyValue)])
        value
    }
    
    public void putValue(String name, Object value) {
        registry.put(name, value)
        publishEvent("valueChanged", [name: name, value: value, lazy: false])
    }
    
    public void putLazyValue(String name, Closure closure) {
        def lazyValue = new LazyValue(this, name, closure)
        registry.put(name, lazyValue)
        publishEvent("valueChanged", [name: name, value: lazyValue, lazy: true])
    }
    
    @Immutable
    static class LazyValue {
        TestRuntime runtime
        String name
        Closure closure
        
        public Object get(Map callerInfo = [:]) {
            if(closure.getMaximumNumberOfParameters()==1) {
                closure.call(runtime)
            } else if(closure.getMaximumNumberOfParameters()==2) {
                closure.call(runtime, name)
            } else if (closure.getMaximumNumberOfParameters() > 2) {
                closure.call(runtime, name, callerInfo)
            } else {
                closure.call()
            }
        }
    }
    
    protected boolean inEventLoop = false
    protected List<TestEvent> deferredEvents = new ArrayList<TestEvent>()
    
    public void publishEvent(String name, Map arguments = [:], Map extraEventProperties = [:]) {
        doPublishEvent(createEvent([runtime: this, name: name, arguments: arguments] + extraEventProperties))
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    protected TestEvent createEvent(Map properties) {
        new TestEvent(properties)
    }

    protected synchronized doPublishEvent(TestEvent event) {
        if(inEventLoop) {
            if(event.immediateDelivery) {
                List<TestEvent> previousDeferredEvents = deferredEvents
                try {
                    deferredEvents = new ArrayList<TestEvent>()
                    processEvents(event)
                } finally {
                    deferredEvents = previousDeferredEvents
                }
            } else {
                deferredEvents.add(event)
            }
        } else {
            try {
                inEventLoop = true
                processEvents(event)
            } finally {
                inEventLoop = false
            }
        }
    }

    private processEvents(TestEvent event) {
        deliverEvent(event)
        executeEventLoop()
    }

    protected executeEventLoop() {
        while(true) {
            List<TestEvent> currentLoopEvents = new ArrayList<TestEvent>(deferredEvents)
            deferredEvents.clear()
            if(currentLoopEvents) {
                for(TestEvent deferredEvent : currentLoopEvents) {
                    deliverEvent(deferredEvent)
                }
            } else {
                break
            }
        }
    }
    
    protected void deliverEvent(TestEvent event) {
        handleSpecialEventsBeforeDelivery(event)
        for(TestPlugin plugin : (event.reverseOrderDelivery ? plugins.reverse() : plugins)) {
            if(event.stopDelivery) {
                break
            }
            plugin.onTestEvent(event)
        }
        handleSpecialEventsAfterDelivery(event)
    }
    
    private void handleSpecialEventsBeforeDelivery(TestEvent event) {

    }
    
    private void handleSpecialEventsAfterDelivery(TestEvent event) {
        switch(event.name) {
            case 'closeRuntime':
                if(!event.stopDelivery) {
                    close()
                }
                break
        }
    }
    
    protected void before(Object testInstance, Description description) {
        publishEvent("before", [testInstance: testInstance, description: description], [immediateDelivery: true])
    }

    protected void after(Object testInstance, Description description, Throwable throwable) {
        publishEvent("after", [testInstance: testInstance, description: description, throwable: throwable], [immediateDelivery: true, reverseOrderDelivery: true])
    }

    protected void beforeClass(Class testClass, Description description) {
        publishEvent("beforeClass", [testClass: testClass, description: description], [immediateDelivery: true])
    }

    protected void afterClass(Class testClass, Description description, Throwable throwable) {
        publishEvent("afterClass", [testClass: testClass, description: description, throwable: throwable], [immediateDelivery: true, reverseOrderDelivery: true])
    }

    public synchronized void close() {
        if(!runtimeClosed) {
            for(TestPlugin plugin : plugins) {
                try {
                    plugin.close(this)
                } catch (Exception e) {
                    // ignore exceptions
                }
            }
            registry.clear()
            plugins = null
            runtimeClosed = true
            TestRuntimeFactory.removeRuntime(this)
        }
    }

    public void setUp(Object testInstance) {
        beforeClass(testInstance.getClass(), Description.createSuiteDescription(testInstance.getClass()))
        before(testInstance, Description.createTestDescription(testInstance.getClass(), "setUp", testInstance.getClass().getAnnotations()))
    }

    public void tearDown(Object testInstance) {
        after(testInstance, Description.createTestDescription(testInstance.getClass(), "tearDown", testInstance.getClass().getAnnotations()), null)
        afterClass(testInstance.getClass(), Description.createSuiteDescription(testInstance.getClass()), null)
    }

    public boolean isClosed() {
        return runtimeClosed;
    }
}
