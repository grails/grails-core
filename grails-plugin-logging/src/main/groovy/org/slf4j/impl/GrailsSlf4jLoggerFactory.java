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

package org.slf4j.impl;

import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates Slf4j loggers
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsSlf4jLoggerFactory implements ILoggerFactory {

    Map<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    public Logger getLogger(String name) {
        Logger logger = loggers.get(name);
        if (logger == null) {
            org.apache.log4j.Logger log4jLogger;
            if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME)) {
                log4jLogger = LogManager.getRootLogger();
            } else {
                log4jLogger = LogManager.getLogger(name);
            }
            logger = new GrailsLog4jLoggerAdapter(log4jLogger);
            loggers.put(name, logger);
        }
        return logger;
    }
}
