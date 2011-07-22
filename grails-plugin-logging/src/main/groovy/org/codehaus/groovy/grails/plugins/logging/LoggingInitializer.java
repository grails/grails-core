/*
 * Copyright 2011 SpringSource
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

import groovy.lang.Closure;
import groovy.util.ConfigObject;
import org.apache.log4j.LogManager;

/**
 * Default logging initializer used for Log4j.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class LoggingInitializer {

    public void initialize(ConfigObject config) {
        LogManager.resetConfiguration();
        Object log4j = config.get("log4j");
        if (log4j instanceof Closure) {
            new Log4jConfig(config).configure((Closure<?>)log4j);
        } else {
            // setup default logging
            new Log4jConfig(config).configure();
        }
    }
}
