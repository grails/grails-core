
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
public interface Config extends PropertyResolver, ConfigMap {

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
     * Merge another config and return this config
     * @param toMerge The map to merge
     * @return This config
     */
    Config merge(Map<String,Object> toMerge)

}