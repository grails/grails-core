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
    private List<TestEventInterceptor> interceptors
    private List<TestEventInterceptor> pluginsRegisteredAsInterceptors
    private Map<String, Object> registry = [:]
    private boolean runtimeClosed = false
    private boolean shared
    private boolean runtimeStarted = false
    
    protected TestRuntime(Set<String> features, List<TestPlugin> plugins, TestEventInterceptor interceptor, boolean shared) {
        this.interceptors=new ArrayList<TestEventInterceptor>()
        interceptors.add(new TestRuntimeEventInterceptor())
        if(interceptor != null) {
            interceptors.add(interceptor)
        }
        changeFeaturesAndPlugins(features, plugins)
        this.@shared=shared
    }
    
    public boolean isShared() {
        return this.@shared
    }
    
    public void changeFeaturesAndPlugins(Set<String> features, List<TestPlugin> plugins) {
        if(pluginsRegisteredAsInterceptors) {
            interceptors.removeAll(pluginsRegisteredAsInterceptors)
        }
        this.features = new LinkedHashSet<String>(features).asImmutable()
        this.plugins =  new ArrayList<TestPlugin>(plugins).asImmutable()
        pluginsRegisteredAsInterceptors=new ArrayList(plugins.findAll { it instanceof TestEventInterceptor })
        interceptors.addAll(pluginsRegisteredAsInterceptors)
    }
    
    public void addInterceptor(TestEventInterceptor interceptor) {
        if(!interceptors.contains(interceptor)) {
            interceptors.add(interceptor)
        }
    }
    
    public void removeInterceptor(TestEventInterceptor interceptor) {
        interceptors.remove(interceptor)
    }
    
    @CompileStatic
    private class TestRuntimeEventInterceptor implements TestEventInterceptor {
        private void eventProcessed(TestEvent event) {
            switch(event.name) {
                case 'closeRuntime':
                    if(!event.stopDelivery) {
                        close()
                    }
                    break
                case 'requestFreshRuntime':
                    if(!event.stopDelivery) {
                        publishEvent('startFreshRuntime')
                    }
                    break
            }
        }
        
        @Override
        public void eventPublished(TestEvent event) {
            
        }
    
        @Override
        public void eventsProcessed(TestEvent event, List<TestEvent> consequenceEvents) {
            eventProcessed(event)
            for(TestEvent consequenceEvent : consequenceEvents) {
                eventProcessed(event)
            }
        }
    
        @Override
        public void eventDelivered(TestEvent event) {
            
        }
    
        @Override
        public void mutateDeferredEvents(TestEvent initialEvent, List<TestEvent> deferredEvents) {
            
        }
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
    
    protected TestEvent currentInitialEvent = null
    protected List<TestEvent> deferredEvents = new ArrayList<TestEvent>()
    
    public void publishEvent(String name, Map arguments = [:], Map extraEventProperties = [:]) {
        doPublishEvent(createEvent([runtime: this, name: name, arguments: arguments, parentEvent: currentInitialEvent] + extraEventProperties))
    }
    
    public void requestClose() {
        publishEvent("closeRuntime")
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    protected TestEvent createEvent(Map properties) {
        new TestEvent(properties)
    }

    protected synchronized void doPublishEvent(TestEvent event) {
        if(runtimeClosed) {
            throw new IllegalStateException("TestRuntime has already been closed.")
        }
        sendStartRuntimeEventOnFirstEvent(event)
        handleEventPublished(event)
        if(event.stopDelivery) {
            return
        }
        if(currentInitialEvent != null) {
            if(event.immediateDelivery) {
                List<TestEvent> previousDeferredEvents = deferredEvents
                TestEvent previousInitialEvent = currentInitialEvent
                try {
                    deferredEvents = new ArrayList<TestEvent>()
                    processEvents(event)
                } finally {
                    deferredEvents = previousDeferredEvents
                    currentInitialEvent = previousInitialEvent
                }
            } else {
                deferredEvents.add(event)
            }
        } else {
            try {
                processEvents(event)
            } finally {
                currentInitialEvent = null
            }
        }
    }

    private sendStartRuntimeEventOnFirstEvent(TestEvent event) {
        if(!runtimeStarted) {
            runtimeStarted=true
            publishEvent('startRuntime', [initialEvent: event], [immediateDelivery: true])
        }
    }

    private processEvents(TestEvent event) {
        currentInitialEvent = event
        deliverEvent(event)
        List<TestEvent> handledDeferredEvents=executeEventLoop(event)
        currentInitialEvent = null
        handleEventProcessed(event, handledDeferredEvents)
    }

    protected List<TestEvent> executeEventLoop(TestEvent initialEvent) {
        List<TestEvent> handledDeferredEvents=new ArrayList<TestEvent>()
        while(true) {
            List<TestEvent> currentLoopEvents = new ArrayList<TestEvent>(deferredEvents)
            deferredEvents.clear()
            if(initialEvent != null) {
                filterDeferredEvents(initialEvent, currentLoopEvents)
            }
            if(currentLoopEvents && !runtimeClosed) {
                for(TestEvent deferredEvent : currentLoopEvents) {
                    deliverEvent(deferredEvent)
                    handledDeferredEvents.add(deferredEvent)
                }
            } else {
                break
            }
        }
        handledDeferredEvents
    }
    
    protected void deliverEvent(TestEvent event) {
        for(TestPlugin plugin : (event.reverseOrderDelivery ? plugins.reverse() : plugins)) {
            if(event.stopDelivery) {
                break
            }
            plugin.onTestEvent(event)
        }
        handleEventDelivered(event)
        event.delivered = true
    }
    
    private void filterDeferredEvents(TestEvent initialEvent, List<TestEvent> deferredEvents) {
        for(TestEventInterceptor interceptor : interceptors) {
            interceptor.mutateDeferredEvents(initialEvent, deferredEvents)
        }
    }
    
    private void handleEventPublished(TestEvent event) {
        for(TestEventInterceptor interceptor : interceptors) {
            interceptor.eventPublished(event)
        }
    }
    
    private void handleEventDelivered(TestEvent event) {
        for(TestEventInterceptor interceptor : interceptors) {
            interceptor.eventDelivered(event)
        }
    }

    private void handleEventProcessed(TestEvent event, List<TestEvent> handledDeferredEvents) {
        for(TestEventInterceptor interceptor : (event.reverseOrderDelivery ? interceptors.reverse() : interceptors)) {
            interceptor.eventsProcessed(event, handledDeferredEvents)
        }
    }
    
    protected void close() {
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
            currentInitialEvent = null
            deferredEvents.clear()
            TestRuntimeFactory.removeRuntime(this)
        }
    }

    public boolean isClosed() {
        return runtimeClosed;
    }
}
