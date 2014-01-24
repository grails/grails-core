package grails.test.runtime;

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@CompileStatic
class TestRuntime {
    private List<TestPlugin> plugins
    private Map<String, Object> registry = [:]
    private boolean runtimeClosed = false
    
    public TestRuntime(List<TestPlugin> plugins) {
        this.plugins =  new ArrayList<TestPlugin>(plugins)
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
    
    public boolean containsValueFor(String name) {
        registry.containsKey(name)
    }
    
    public void removeValue(String name) {
        Object value = registry.remove(name)
        publishEvent("valueRemoved", [name: name, value: value, lazy: (value instanceof LazyValue)])
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
                deliverEvent(event)
            } else {
                deferredEvents.add(event)
            }
        } else {
            try {
                inEventLoop = true
                deliverEvent(event)
                executeEventLoop()
            } finally {
                inEventLoop = false
            }
        }
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
        if(event.stopDelivery) {
            return
        }
        for(TestPlugin plugin : (event.reverseOrderDelivery ? plugins.reverse() : plugins)) {
            plugin.onTestEvent(event)
            if(event.stopDelivery) {
                break
            }
        }
    }
    
    public TestRule newRule(Object targetInstance) {
        return new TestRule() {
            Statement apply(Statement statement, Description description) {
                return new Statement() {
                    public void evaluate() throws Throwable {
                        before(description)
                        try {
                            statement.evaluate()
                        } catch (Throwable t) {
                            try {
                                after(description, t)
                            } catch (Throwable t2) {
                                // ignore
                            } finally {
                                // throw original exception
                                throw t
                            }
                        }
                        after(description, null)
                    }
                }
            }
        }
    }

    protected void before(Description description) {
        publishEvent("before", [description: description])
    }

    protected void after(Description description, Throwable throwable) {
        publishEvent("after", [description: description, throwable: throwable], [reverseOrderDelivery: true])
    }

    public TestRule newClassRule(Class<?> targetClass) {
        return new TestRule() {
            Statement apply(Statement statement, Description description) {
                return new Statement() {
                    public void evaluate() throws Throwable {
                        beforeClass(description)
                        try {
                            statement.evaluate()
                        } catch (Throwable t) {
                            try {
                                afterClass(description, t)
                            } catch (Throwable t2) {
                                // ignore
                            } finally {
                                // throw original exception
                                throw t
                            }
                        }
                        afterClass(description, null)
                    }
                }
            }
        }
    }

    protected void beforeClass(Description description) {
        publishEvent("beforeClass", [description: description])
    }

    protected void afterClass(Description description, Throwable throwable) {
        publishEvent("afterClass", [description: description, throwable: throwable], [reverseOrderDelivery: true])
        close()
    }

    private synchronized void close() {
        if(!runtimeClosed) {
            for(TestPlugin plugin : plugins) {
                try {
                    plugin.close()
                } catch (Exception e) {
                    // ignore exceptions
                }
            }
            registry.clear()
            plugins.clear()
            runtimeClosed = true
        }
    }

    public void setUp(Object testInstance) {
        beforeClass(Description.createSuiteDescription(testInstance.getClass()))
        before(Description.createTestDescription(testInstance.getClass(), "setUp"))
    }

    public void tearDown(Object testInstance) {
        after(Description.createTestDescription(testInstance.getClass(), "tearDown"), null)
        afterClass(Description.createSuiteDescription(testInstance.getClass()), null)
    }

    public boolean isClosed() {
        return runtimeClosed;
    }
}
