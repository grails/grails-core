/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.util
/**
 * An category for use with maps that want type conversion capabilities
 * 
 * @author Graeme Rocher
 * @since 1.2
 */


class TypeConvertingMap extends LinkedHashMap {
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Byte getByte(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).byteValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Byte.parseByte(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Integer getInt(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return o.intValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Integer.parseInt(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @return The long value or null if there isn't one
     */
    Long getLong(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).longValue()
        }
        else if(o != null) {
            try {
                return Long.parseLong(o.toString())
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining short value from parameter
     * @param name The name of the parameter
     * @return The short value or null if there isn't one
     */
    Short getShort(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).shortValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Short.parseShort(string)
            }
            catch (NumberFormatException e) {
            }
        }

    }

    /**
     * Helper method for obtaining double value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Double getDouble(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).doubleValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Double.parseDouble(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Float getFloat(String name) {
        def o = get(name)
        if(o instanceof Number) {
           return ((Number)o).floatValue()
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Float.parseFloat(string)
            }
            catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Boolean getBoolean(String name) {
        def o = get(name)
        if(o instanceof Boolean) {
           return o
        }
        else if(o != null) {
            try {
                String string = o.toString()
                if(string)
                    return Boolean.parseBoolean(string)
            }
            catch (e) {
            }
        }
    }

  /**
     * Helper method for obtaining a list of values from parameter
     * @param name The name of the parameter
     * @return A list of values
     */
    List getList(String name) {
        def paramValues = get(name)
        if(paramValues == null) {
            return []
        }
        else if(paramValues?.getClass().isArray()) {
            return Arrays.asList(paramValues)
        }
        else if(paramValues instanceof Collection) {
            return new ArrayList(paramValues)
        }
        else {
            return [paramValues]
        }
    }
}