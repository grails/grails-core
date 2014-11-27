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
package org.grails.cli.profile

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jline.console.completer.Completer

/**
 * Describes a {@link Command}
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Canonical
class CommandDescription {
    /**
     * The name of the command
     */
    String name
    /**
     * The description of the command
     */
    String description
    /**
     * The usage instructions for the command
     */
    String usage

    /**
     * Arguments to the command
     */
    List<CommandArgument> arguments = []

    /**
     * Flags to the command. These differ as they are optional and are prefixed with a hyphen (Example -debug)
     */
    List<CommandArgument> flags = []

    /**
     * A completer for the command
     */
    Completer completer = null

    /**
     * Sets the completer
     *
     * @param completer The class of the completer to set
     * @return The description instance
     */
    CommandDescription completer(Class<Completer> completer) {
        this.completer = completer.newInstance()
        return this
    }

    /**
     * Sets the completer
     *
     * @param completer The completer to set
     * @return The description instance
     */
    CommandDescription completer(Completer completer) {
        this.completer = completer
        return this
    }

    /**
     * Adds an argument for the given named arguments
     *
     * @param args The named arguments
     */
    @CompileDynamic
    CommandDescription argument(Map args) {
        arguments << new CommandArgument(args)
        return this
    }

    /**
     * Adds a flag for the given named arguments
     *
     * @param args The named arguments
     */
    @CompileDynamic
    CommandDescription flag(Map args) {
        def arg = new CommandArgument(args)
        arg.required = false
        flags << arg
        return this
    }
}
