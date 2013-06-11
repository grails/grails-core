/*
 * Copyright 2011 GoPivotal, Inc. All Rights Reserved
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
package org.codehaus.groovy.grails.plugins.logging;

import groovy.util.ConfigObject;

import org.codehaus.groovy.grails.plugins.log4j.Log4jConfig;

/**
 * Default logging initializer used for Log4j.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class LoggingInitializer {

    public void initialize(ConfigObject config) {
        Log4jConfig.initialize(config);
    }
}
