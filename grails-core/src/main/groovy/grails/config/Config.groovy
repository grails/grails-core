
/*
 * Copyright 2014 original authors
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

import org.springframework.core.env.PropertyResolver

/**
 * Interface to application configuration
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public interface Config extends PropertyResolver, Iterable<Map.Entry<String, Object>>, Map<String, Object> {

    /**
     * Enables the object[foo] syntax
     *
     * @param key The key
     * @return The value or null
     */
    def getAt(Object key)

    /**
     * Enables the object[foo] = 'stuff' syntax
     *
     * @param key The key
     * @param value The value
     */
    void setAt(Object key, Object value)

    /**
     * Gets a value for the given key
     *
     * @param key They key
     * @return The value
     */
    def get(Object key)

    /**
     * @return The flat version of the config
     */
    Map<String, Object> flatten()

    /**
     * Converts the config to properties
     *
     * @return The properties
     */
    Properties toProperties()

    /**
     * Navigate the map for the given path
     *
     * @param path The path
     * @return
     */
    Object navigate(String... path)

    /**
     * Merge another config and return this config
     * @param toMerge The map to merge
     * @return This config
     */
    Config merge(Map<String,Object> toMerge)
}