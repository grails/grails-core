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
package grails.test.mixin.support

import grails.config.Config
import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import junit.framework.AssertionFailedError
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.MessageSource
/**
 * A base unit testing mixin that watches for MetaClass changes and unbinds them on tear down.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class GrailsUnitTestMixin extends TestMixinRuntimeSupport {

    private static final Set<String> REQUIRED_FEATURES = (["grailsApplication", "coreBeans"] as Set).asImmutable() 
    
    public GrailsUnitTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set)
    }
    
    public GrailsUnitTestMixin() {
        super(REQUIRED_FEATURES)
    }
    
    /**
     * Asserts that the given code closure fails when it is evaluated
     *
     * @param code
     * @return the message of the thrown Throwable
     */
    String shouldFail(Closure code) {
        boolean failed = false
        String result = null
        try {
            code.call()
        }
        catch (GroovyRuntimeException gre) {
            failed = true
            result = ScriptBytecodeAdapter.unwrap(gre).getMessage()
        }
        catch (Throwable e) {
            failed = true
            result = e.getMessage()
        }
        if (!failed) {
            throw new AssertionFailedError("Closure " + code + " should have failed")
        }

        return result
    }

    /**
     * Asserts that the given code closure fails when it is evaluated
     * and that a particular exception is thrown.
     *
     * @param clazz the class of the expected exception
     * @param code  the closure that should fail
     * @return the message of the expected Throwable
     */
    String shouldFail(Class<?> clazz, Closure code) {
        Throwable th = null
        try {
            code.call()
        } catch (GroovyRuntimeException gre) {
            th = ScriptBytecodeAdapter.unwrap(gre)
        } catch (Throwable e) {
            th = e
        }

        if (th == null) {
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name")
        }

        if (!clazz.isInstance(th)) {
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name, instead got Exception $th")
        }

        return th.message
    }
    
    /**
     * Loads the given codec, adding the "encodeAs...()" and "decode...()"
     * methods to objects.
     * @param codecClass The codec to load, e.g. HTMLCodec.
     */
    void mockCodec(Class<?> codecClass) {
        runtime.publishEvent("mockCodec", [codecClass: codecClass])
    }
    
    void defineBeans(boolean immediateDelivery = true, Closure<?> closure) {
        runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: immediateDelivery])
    }

    ConfigurableApplicationContext getApplicationContext() {
        getMainContext()
    }

    ConfigurableApplicationContext getMainContext() {
        (ConfigurableApplicationContext)grailsApplication.mainContext
    }
    
    GrailsApplication getGrailsApplication() {
        (GrailsApplication)runtime.getValue("grailsApplication")
    }
    
    Config getConfig() {
        getGrailsApplication().getConfig()
    }
    
    MessageSource getMessageSource() {
        applicationContext.getBean("messageSource", MessageSource)
    }
}
