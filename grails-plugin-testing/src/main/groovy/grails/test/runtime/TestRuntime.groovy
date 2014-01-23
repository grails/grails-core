package grails.test.runtime;

import java.util.Map;

import grails.async.Promises
import grails.spring.BeanBuilder
import grails.util.Holders
import grails.util.Metadata
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import groovy.transform.CompileStatic
import groovy.transform.Immutable;

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.grails.async.factory.SynchronousPromiseFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.springframework.beans.CachedIntrospectionResults
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

@CompileStatic
class TestRuntime {
    private List<TestPlugin> plugins = new ArrayList<TestPlugin>()
    
    private Map<String, Object> registry = [:]
    
    public Object getValue(String name) {
        publishEvent("valueRequested", [name: name])
        if(!containsValueFor(name)) {
            publishEvent("valueMissing", [name: name])
        }
        registry.get(name)
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
        
        public Object get() {
            if(closure.getMaximumNumberOfParameters()==1) {
                closure.call(runtime)
            } else if (closure.getMaximumNumberOfParameters() > 1) {
                closure.call(runtime, name)
            } else {
                closure.call()
            }
        }
    }
    
    public void publishEvent(String name, Map arguments) {
        TestEvent event = new TestEvent(name: name, arguments: arguments)
        plugins*.onTestEvent(event)
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
        publishEvent("after", [description: description, throwable: throwable])
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
        publishEvent("afterClass", [description: description, throwable: throwable])
    }

    public void setUp(Object testInstance) {
        beforeClass(Description.createSuiteDescription(testInstance.getClass()))
        before(Description.createTestDescription(testInstance.getClass(), "setUp"))
    }

    public void tearDown(Object testInstance) {
        after(Description.createTestDescription(testInstance.getClass(), "tearDown"), null)
        afterClass(Description.createSuiteDescription(testInstance.getClass()), null)
    }
}
