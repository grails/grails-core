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
package org.grails.cli.profile.commands.script

import grails.util.Environment
import groovy.transform.CompileStatic
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile

/**
 * A binding for a script execution in the context of a {@link Profile}
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ProfileBinding extends Binding {

    ProfileBinding(Profile profile, ExecutionContext executionContext) {
        setVariable("context", executionContext)
        setVariable("profile", profile)
        setVariable("grailsConsole", executionContext.console)
        setVariable("basedir", executionContext.baseDir)
        setVariable("argsMap", executionContext.commandLine.undeclaredOptions)
        setVariable("grailsEnv", Environment.current.name)
        setVariable("userHome", System.getProperty('user.home'))
        setVariable("grailsVersion", getClass().getPackage().getImplementationVersion())
    }
}
