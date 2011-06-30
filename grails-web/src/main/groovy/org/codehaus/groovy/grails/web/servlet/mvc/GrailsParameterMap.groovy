/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder
import org.codehaus.groovy.grails.web.binding.StructuredDateEditor
import org.codehaus.groovy.grails.web.binding.StructuredPropertyEditor
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.context.i18n.LocaleContextHolder

import javax.servlet.http.HttpServletRequest
import java.io.UnsupportedEncodingException
import java.text.DateFormat
import java.text.SimpleDateFormat
import org.codehaus.groovy.grails.web.util.TypeConvertingMap

/**
 * A parameter map class that allows mixing of request parameters and controller parameters. If a controller
 * parameter is set with the same name as a request parameter the controller parameter value is retrieved.
 *
 * @author Graeme Rocher
 * @author Kate Rhodes
 *
 * @since Oct 24, 2005
 */
class GrailsParameterMap extends TypeConvertingMap {

    private HttpServletRequest request

    /**
     * Does not populate the GrailsParameterMap from the request but instead uses the supplied values.
     *
     * @param values The values to populate with
     * @param request The request object
     */
    GrailsParameterMap(Map values,HttpServletRequest request) {
        this.request = request
        this.@wrappedMap.putAll(values)
    }

    /**
     * Creates a GrailsParameterMap populating from the given request object
     * @param request The request object
     */
    GrailsParameterMap(HttpServletRequest request) {
        this.request = request
        final Map requestMap = new LinkedHashMap(request.getParameterMap())
        if (request instanceof MultipartHttpServletRequest) {
            def fileMap = request.fileMap
            for (fileName in fileMap.keySet()) {
                requestMap.put(fileName, request.getFile(fileName))
            }
        }
        for (key in requestMap.keySet()) {
            Object paramValue = getParameterValue(requestMap, key)

            this.@wrappedMap.put(key, paramValue)
            processNestedKeys(request, requestMap, key, key, this.@wrappedMap)
        }
    }

    Object clone() {
        new GrailsParameterMap(new HashMap(this.@wrappedMap), request)
    }

    private Object getParameterValue(Map requestMap, String key) {
        Object paramValue = requestMap.get(key)
        if (paramValue instanceof String[]) {
            if (paramValue.length == 1) {
                paramValue = paramValue[0]
            }
        }
        return paramValue
    }

    /*
     * Builds up a multi dimensional hash structure from the parameters so that nested keys such as
     * "book.author.name" can be addressed like params['author'].name
     *
     * This also allows data binding to occur for only a subset of the properties in the parameter map.
     */
    private void processNestedKeys(HttpServletRequest request, Map requestMap, String key,
            String nestedKey, Map nestedLevel) {
        final int nestedIndex = nestedKey.indexOf('.')
        if (nestedIndex > -1) {
            // We have at least one sub-key, so extract the first element
            // of the nested key as the prfix. In other words, if we have
            // 'nestedKey' == "a.b.c", the prefix is "a".
            String nestedPrefix = nestedKey.substring(0, nestedIndex)
            boolean prefixedByUnderscore = false

            // Use the same prefix even if it starts with an '_'
            if (nestedPrefix.startsWith('_')) {
                prefixedByUnderscore = true
                nestedPrefix = nestedPrefix[1..-1]
            }
            // Let's see if we already have a value in the current map for the prefix.
            Object prefixValue = nestedLevel.get(nestedPrefix)
            if (prefixValue == null) {
                // No value. So, since there is at least one sub-key,
                // we create a sub-map for this prefix.
                prefixValue = new GrailsParameterMap(new HashMap(), request)
                nestedLevel.put(nestedPrefix, prefixValue)
            }

            // If the value against the prefix is a map, then we store the sub-keys in that map.
            if (prefixValue instanceof Map) {
                Map nestedMap = (Map)prefixValue
                if (nestedIndex < nestedKey.length()-1) {
                    String remainderOfKey = nestedKey.substring(nestedIndex + 1, nestedKey.length())
                    // GRAILS-2486 Cascade the '_' prefix in order to bind checkboxes properly
                    if (prefixedByUnderscore) {
                        remainderOfKey = '_' + remainderOfKey
                    }
                    nestedMap.put(remainderOfKey,getParameterValue(requestMap, key))
                    if (remainderOfKey.indexOf('.') >-1) {
                        processNestedKeys(request, requestMap, key, remainderOfKey, nestedMap)
                    }
                }
            }
        }
    }

    /**
     * @return Returns the request.
     */
    HttpServletRequest getRequest() { request }

    int size() { this.@wrappedMap.size() }

    boolean isEmpty() { this.@wrappedMap.empty }

    boolean containsKey(Object key) { this.@wrappedMap.containsKey(key) }

    boolean containsValue(Object value) { this.@wrappedMap.containsValue(value)    }

    private Map nestedDateMap = [:]


    Object get(Object key) {
        // removed test for String key because there
        // should be no limitations on what you shove in or take out
        def returnValue
        if (nestedDateMap.containsKey(key)) {
            returnValue = nestedDateMap.get(key)
        }
        else if (this.@wrappedMap.get(key) instanceof String[]) {
            String[] valueArray = this.@wrappedMap.get(key)
            if (valueArray == null) {
                return null
            }

            if (valueArray.length == 1) {
                returnValue = valueArray[0]
            }
            else {
                returnValue = valueArray
            }
        }
        else {
            returnValue = this.@wrappedMap.get(key)
        }

        if ("date.struct".equals(returnValue)) {
            returnValue = lazyEvaluateDateParam(key)
            nestedDateMap[key] = returnValue
        }
        return returnValue
    }

    private Date lazyEvaluateDateParam(Object key) {
        // parse date structs automatically
        def dateParams = [:]
        for (entry in entrySet()) {
            final entryKey = entry.key
            if (entryKey instanceof String) {
                String paramName = entryKey
                final String prefix = key + "_"
                if (paramName.startsWith(prefix)) {
                    dateParams.put(paramName.substring(prefix.length(), paramName.length()), entry.getValue())
                }
            }
        }

        def dateFormat = new SimpleDateFormat(GrailsDataBinder.DEFAULT_DATE_FORMAT,
                LocaleContextHolder.getLocale())
        def editor = new StructuredDateEditor(dateFormat, true)
        try {
            return editor.assemble(Date, dateParams)
        }
        catch (IllegalArgumentException e) {
            return null
        }
    }

    Object put(Object key, Object value) {
        if (value instanceof CharSequence) value = value.toString()
        if (nestedDateMap.containsKey(key)) nestedDateMap.remove(key)
        return this.@wrappedMap.put(key, value)
    }

    Object remove(Object key) {
        nestedDateMap.remove(key)
        return this.@wrappedMap.remove(key)
    }

    void putAll(Map map) {
        for (entry in map) {
            put entry.key, entry.value
        }
    }

    void clear() {
        this.@wrappedMap.clear()
    }

    Set keySet() { this.@wrappedMap.keySet() }

    Collection values() { this.@wrappedMap.values() }

    Set entrySet() { this.@wrappedMap.entrySet() }

    /**
     * Converts this parameter map into a query String. Note that this will flatten nested keys separating them with the
     * . character and URL encode the result
     *
     * @return A query String starting with the ? character
     */
    String toQueryString() {

        String encoding = request.characterEncoding
        try {
            return WebUtils.toQueryString(this,encoding)
        }
        catch (UnsupportedEncodingException e) {
            throw new ControllerExecutionException("Unable to convert parameter map [" + this +
                 "] to a query string: " + e.getMessage(), e)
        }
    }

    String toString() {
        return DefaultGroovyMethods.inspect(this.@wrappedMap)
    }

    
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Character 'char'(String name) { getChar(name) }
    
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Character 'char'(String name, Character defaultValue) { 
        'char'(name, (Integer)defaultValue)
    }
    
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Character 'char'(String name, Integer defaultValue) { 
        Character value = getChar(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Byte 'byte'(String name) { getByte(name) }

    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to a Byte
     * @return The integer value or null if there isn't one
     */
    Byte 'byte'(String name, Integer defaultValue) { 
        Byte value = getByte(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Integer 'int'(String name) { getInt(name) }

    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to an Integer
     * @return The integer value or null if there isn't one
     */
    Integer 'int'(String name, Integer defaultValue) {
        Integer value = getInt(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    
    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @return The long value or null if there isn't one
     */
    Long 'long'(String name) { getLong(name) }

    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to a Long
     * @return The long value or null if there isn't one
     */
    Long 'long'(String name, Long defaultValue) {
        Long value = getLong(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    /**
     * Helper method for obtaining short value from parameter
     * @param name The name of the parameter
     * @return The short value or null if there isn't one
     */
    Short 'short'(String name) { getShort(name) }

    /**
     * Helper method for obtaining short value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to a Short
     * @return The short value or null if there isn't one
     */
    Short 'short'(String name, Integer defaultValue) { 
        Short value = getShort(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    /**
     * Helper method for obtaining double value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Double 'double'(String name) { getDouble(name) }

    /**
     * Helper method for obtaining double value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to a Double
     * @return The double value or null if there isn't one
     */
    Double 'double'(String name, Double defaultValue) { 
        Double value = getDouble(name)
        if(value == null) {
            value = defaultValue
        }
        value
    }
    
    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Float 'float'(String name) { getFloat(name) }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @param defaultValue The default value to use if the parameter does not exist or cannot be converted to a Float
     * @return The double value or null if there isn't one
     */
    Float 'float'(String name, Float defaultValue) { 
        Float value = getFloat(name)
        if(value == null) {
            value = defaultValue
        } 
        value
    }
    
    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Boolean 'boolean'(String name) {
        getBoolean(name)
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Boolean 'boolean'(String name, Boolean defaultValue) {
        Boolean value
        if(containsKey(name)) {
            value = getBoolean(name)
        } else {
            value = defaultValue
        }
        value
    }
    
    /**
     * Obtains a list of values from parameter.
     * @param name The name of the parameter
     * @return A list of values
     */
    List list(String name) {
        getList(name)
    }
}
