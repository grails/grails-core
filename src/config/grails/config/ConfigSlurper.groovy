/* Copyright 2006-2007 Graeme Rocher
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
package grails.config

import java.beans.Introspector
import java.beans.BeanInfo

/**
* <p>
* ConfigSlurper2 is a utility class for reading configuration files defined in the form of Groovy
* scripts. Configuration settings can be defined using dot notation or scoped using closures
*
* <pre><code>
*   grails.webflow.stateless = true
*    smtp {
*        mail.host = 'smtp.myisp.com'
*        mail.auth.user = 'server'
*    }
*    resources.URL = "http://localhost:80/resources"
* </pre></code>
*
* <p>Settings can either be bound into nested maps or onto a specified JavaBean instance. In the case
* of the latter an error will be thrown if a property cannot be bound.
*
* @author Graeme Rocher
* @since 0.6
*
*        <p/>
*        Created: Jun 19, 2007
*        Time: 3:53:48 PM
*/

class ConfigSlurper {

    private BeanInfo bean
    private instance
    GroovyClassLoader classLoader = new GroovyClassLoader()

    ConfigSlurper() { }
    ConfigSlurper(Class beanClass) {
        if(!beanClass) throw new IllegalArgumentException("Argument [beanClass] cannot be null")

        this.bean = Introspector.getBeanInfo(beanClass)
        this.instance = beanClass.newInstance()
    }

    /**
     * Parse the given script as a string and return the configuration object
     */
    def parse(String script) {
        return parse(classLoader.parseClass(script))
    }

    def parse(Class scriptClass) {
        return parse(scriptClass.newInstance())
    }

    def parse(Script script) {
        def config = [:]
        def mc = script.class.metaClass
        Stack stack = new Stack()
        mc.getProperty = { String name ->
            def result
            def current
            if(stack) current = stack.peek()
            else {
                current = config
            }

            if(current[name]) {
                result = current[name]
            }
            else {
                result = new ConfigObject()
                current[name] = result
            }
            return result
        }
        mc.invokeMethod = { String name, args ->
            def result
            if(args.length == 1 && args[0] instanceof Closure) {
                def co = new ConfigObject()
                if(stack) {
                    stack.peek()[name] = co
                }
                else {
                    config[name] = co
                }
                stack.push(co)
                args[0].call()
                stack.pop()
            }
            else {
                MetaMethod mm = mc.getMetaMethod(name, args)
                result = mm.invoke(delegate, args)
            }
            result
        }
        script.metaClass = mc

        def setProperty = { String name, value ->
            def current
            if(stack) current = stack.peek()
            else {
                current = config
            }
            current[name] = value
        }
        script.binding = new ConfigBinding(setProperty)

        
        script.run()

        return config        
    }
}
class ConfigObject extends HashMap {

    def getProperty(String name) {
        def prop = get(name)
        if(!prop) prop = new ConfigObject()
        put(name, prop)
        return prop
    }
}
class ConfigBinding extends Binding {
    def callable
    ConfigBinding(Closure c) {
        this.callable = c
    }

    void setVariable(String name, Object value) {
        callable(name, value)
    }
}