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


/**
* A ConfigObject at a simple level is a Map that creates configuration entries (other ConfigObjects) when referencing them.
* This means that navigating to foo.bar.stuff will not return null but nested ConfigObjects which are of course empty maps
* The Groovy truth can be used to check for the existance of "real" entries.
*
* @author Graeme Rocher
* @since 0.6
*/
class ConfigObject extends LinkedHashMap implements Writable {

    URL file

    ConfigObject(URL file) {
        this.file = file
    }

    ConfigObject() {}

    URL getConfigFile() {
        return this.file    
    }         

    /**
	 * Writes this config object into a String serialized representation which can later be parsed back using the parse()
	 * method
	 *
     *  @see groovy.lang.Writable#writeTo(java.io.Writer)
     */ 
	Writer writeTo(Writer outArg) {
        def out
        try {
            out = new BufferedWriter(outArg)
            writeConfig("",this, out, 0, false)
        } finally {
            out.flush()
        }

		return outArg
	}
                  
    private writeConfig(String prefix,ConfigObject map, out, Integer tab, boolean apply) {
        def space = apply ? '\t'*tab : ''
        for(key in map.keySet()) {
			def value = map.get(key)

			if(value instanceof ConfigObject) {
                def dotsInKeys = value.find { entry -> entry.key.indexOf('.') > -1 }
                def configSize = value.size()
                def firstKey = value.keySet().iterator().next()
                def firstValue = value.values().iterator().next()
                def firstSize
                if(firstValue instanceof ConfigObject){
                    firstSize = firstValue.size()
                }
                else { firstSize = 1 }
				if(configSize == 1|| dotsInKeys )  {

                    if(firstSize == 1 && firstValue instanceof ConfigObject) {
                        writeConfig("${key}.${firstKey}.", firstValue, out, tab, true)
                    }
                    else if(!dotsInKeys) {
                       writeNode(key, space, tab,value, out)
                    }  else {
                        for(j in value.keySet()) {
                            def v2 = value.get(j)
                            def k2 = j.indexOf('.') > -1 ? j.inspect() : j
                            if(v2 instanceof ConfigObject) {
                                writeConfig("${key}", v2, out, tab, false)
                            }
                            else {
                                writeValue("${key}.${k2}", space, prefix, v2, out)
                            }
                        }
                    }

				}
				else {
                    writeNode(key, space,tab, value, out)
				}
			}   
			else {

                writeValue(key, space, prefix, value, out)
			}
		}	
	}

    private writeValue(key, space, prefix, value, out) {
        key = key.indexOf('.') > -1 ? key.inspect() : key
        out << "${space}${prefix}$key=${value.inspect()}"
        out.newLine()
    }

    private writeNode(key, space, tab, value, out) {
        out << "${space}$key {"
        out.newLine()
        writeConfig("",value, out, tab+1, true)
        def last = "${space}}"
        out << last
        out.newLine()
    }

    /**
     * Overrides the default getProperty implementation to create nested ConfigObject instances on demand
     * for non-existant keys
     */
    def getProperty(String name) {
        def prop = get(name)
        if(prop == null) prop = new ConfigObject(this.file)
        put(name, prop)
        return prop
    }

    /**
     * A ConfigObject is a tree structure consisting of nested maps. This flattens the maps into
     * a single level structure like a properties file
     */
    Map flatten() {
        return flatten(null)
    }
    /**
     * Flattens this ConfigObject populating the results into the target Map
     *
     * @see ConfigObject#flatten()
     */
    Map flatten(Map target) {
        if(target == null)target = [:]
        populate("", target, this)
        target
    }

    /**
     * Converts this ConfigObject into a the java.util.Properties format, flattening the tree structure beforehand
     * @return A java.util.Properties instance
     */
    Properties toProperties() {
        def props = new Properties()
        flatten(props)
        props = convertValuesToString(props)
        return props
    }

    /**
     * Converts this ConfigObject ino the java.util.Properties format, flatten the tree and prefixing all entries with the given prefix
     * @param prefix The prefix to append before property entries
     * @return A java.util.Properties instance
     */
    Properties toProperties(String prefix) {
        def props = new Properties()
        populate("${prefix}.", props, this)
        props = convertValuesToString(props)
        return props
    }

    private convertValuesToString(props) {
        def newProps = [:]
        for(e in props) {
            newProps[e.key] = e.value?.toString()
        }
        return newProps
    }

    private populate(suffix, config, map) {
        for(key in map.keySet()) {
            def value = map.get(key)
            if(value instanceof Map) {
                populate(suffix+"${key}.", config, value)
            }
            else {
                config[suffix+key] = value
            }
        }
    }
}
