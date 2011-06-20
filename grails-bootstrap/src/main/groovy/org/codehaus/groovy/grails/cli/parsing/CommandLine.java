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
package org.codehaus.groovy.grails.cli.parsing;

import java.util.List;
import java.util.Properties;

/**
 * Represents the parsed command line options
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public interface CommandLine {

    /**
     *
     * @return The environment specified
     */
    String getEnvironment();

    /**
     * @return Whether the environment is user specified
     */
    boolean isEnvironmentSet();

    /**
     *
     * @return The command name specified
     */
    String getCommandName();

    /**
     *
     * @return The remaining arguments after the command name
     */
    List<String> getRemainingArgs();

    /**
     *
     * @return The remaining arguments as an array
     */
    String[] getRemainingArgsArray();

    /**
     * @return The system properties specified
     */
    Properties getSystemProperties();

    /**
     *
     * @param name The name of the option
     * @return Whether the given option is specified
     */
    public boolean hasOption(String name);

    /**
     * The value of an option
     * @param name The option
     * @return The value
     */
    Object optionValue(String name);

    /**
     * @return The remaining args as one big string
     */
    String getRemainingArgsString();

    /**
     * @return The remaining args separated by the line separator char
     */
    String getRemainingArgsLineSeparated();
}
