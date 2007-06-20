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

    private static final ENV_METHOD = "env"
    static final ENV_SETTINGS = '__env_settings__'
    //private BeanInfo bean
    //private instance
    GroovyClassLoader classLoader = new GroovyClassLoader()
    String environment
    private envMode = false

    ConfigSlurper() { }


    /*ConfigSlurper(Class beanClass) {
        if(!beanClass) throw new IllegalArgumentException("Argument [beanClass] cannot be null")

        this.bean = Introspector.getBeanInfo(beanClass)
        this.instance = beanClass.newInstance()
    }*/
    

    /**
     * Parse the given script as a string and return the configuration object
     *
     * @see ConfigSlurper#parse(groovy.lang.Script)
     */
    ConfigObject parse(String script) {
        return parse(classLoader.parseClass(script))
    }

    /**
     * Create a new instance of the given script class and parse a configuration object from it
     *
     * @see ConfigSlurper#parse(groovy.lang.Script)
     */
    ConfigObject parse(Class scriptClass) {
        return parse(scriptClass.newInstance())
    }

    /**
     * Parse the given script into a configuration object (a Map)
     * @param script The script to parse
     * @return A Map of maps that can be navigating with dot de-referencing syntax to obtain configuration entries
     */
    ConfigObject parse(Script script) {
        def config = new ConfigObject()
        def mc = script.class.metaClass
        def prefix = ""
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
                if(name == ENV_METHOD) {
                    try {
                        envMode = true
                        args[0].call()
                    }
                    finally {
                        envMode = false
                    }
                }
                else if(envMode) {
                    if(name == environment) {
                        def co = new ConfigObject()
                        config[ENV_SETTINGS] = co
                        stack.push(co)
                        args[0].call()
                        stack.pop()
                    }
                }
                else {
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
            }
            else if(args.length == 2 && args[1] instanceof Closure) {
                try {
                   prefix = name +'.'
                    def conf = stack ? stack.peek() : config
                    conf[name] = args[0]
                    args[1].call()
                }  finally { prefix = "" }
            }
            else {
                MetaMethod mm = mc.getMetaMethod(name, args)
                if(mm)result = mm.invoke(delegate, args)
                else {
                    throw new MissingMethodException(name, getClass(), args)                    
                }
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
            current[prefix+name] = value
        }
        script.binding = new ConfigBinding(setProperty)

        
        script.run()

        def envSettings = config.remove(ENV_SETTINGS)
        if(envSettings) {
            config = merge(config, envSettings)
        }

        return config        
    }


    /**
     * Merges the second map with the first overriding any matching configuration entries in the first map
     *
     * @param config The root configuration Map
     * @param other The map to merge with the root
     *
     * @return The root configuration Map with the configuration entries merged from the second map
     *
     */
    def merge(Map config,Map other) {

        for(entry in other) {
            def configEntry = config[entry.key]
            if(configEntry == null) {
                config[entry.key] = entry.value
                continue
            }
            else {
               if(configEntry instanceof Map && entry.value instanceof Map) {
                    // recurse
                    merge(configEntry, entry.value)                                
               }
               else {
                   config[entry.key] = entry.value
               }
            }
        }
        return config
    }
}
/**
 * Since Groovy Script don't support overriding setProperty, we have to using a trick with the Binding to provide this
 * functionality
 */
class ConfigBinding extends Binding {
    def callable
    ConfigBinding(Closure c) {
        this.callable = c
    }

    void setVariable(String name, Object value) {
        callable(name, value)
    }
}