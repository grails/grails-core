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

package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.web.util.TypeConvertingMap

/**
 * Class used to define attributes passed to a GSP tag. Mixes in
 * TypeConvertingMap for ease of type conversion
 *
 * @author Graeme Rocher
 * @since 1.2
 */


public class GroovyPageAttributes extends TypeConvertingMap{

    GroovyPageAttributes() {
        this([:])
    }

    GroovyPageAttributes(Map map) {
        super(map);
    }

    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    private Byte 'byte'(String name) { getByte(name) }
    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    private Integer 'int'(String name) { getInt(name) }

    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @return The long value or null if there isn't one
     */
    private Long 'long'(String name) { getLong(name) }

    /**
     * Helper method for obtaining short value from parameter
     * @param name The name of the parameter
     * @return The short value or null if there isn't one
     */
    private Short 'short'(String name) { getShort(name) }

    /**
     * Helper method for obtaining double value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    private Double 'double'(String name) { getDouble(name) }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    private Float 'float'(String name) { getFloat(name) }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    private Boolean 'boolean'(String name) {
        getBoolean(name)
    }

  /**
     * Helper method for obtaining a list of values from parameter
     * @param name The name of the parameter
     * @return A list of values
     */
    List list(String name) {
        getList(name)
    }    
}