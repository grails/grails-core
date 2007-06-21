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
class ConfigObject extends LinkedHashMap {

    URL file

    ConfigObject(URL file) {
        this.file = file
    }

    ConfigObject() {}

    URL getConfigFile() {
        return this.file    
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
