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
package org.grails.cli.profile.steps

import org.gradle.tooling.BuildLauncher
import org.grails.build.parsing.CommandLine
import org.grails.cli.gradle.GradleUtil
import org.grails.cli.profile.*

/**
 * A {@link org.grails.cli.profile.Step} that invokes Gradle
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 *
 * @since 3.0
 */
class GradleStep extends AbstractStep {
    protected List<String> tasks = []
    protected String baseArguments = ""
    protected boolean passArguments = true

    GradleStep(ProfileCommand command, Map<String, Object> parameters) {
        super(command, parameters)
        initialize()
    }


    @Override
    String getName() { "gradle" }

    @Override
    public boolean handle(ExecutionContext context) {
        GradleUtil.runBuildWithConsoleOutput(context) { BuildLauncher buildLauncher ->
            buildLauncher.forTasks(tasks as String[])
            fillArguments(context, buildLauncher)
        }
        return true;
    }

    protected void initialize() {
        tasks = parameters.tasks
        baseArguments = parameters.baseArguments ?: ''
        passArguments = Boolean.valueOf(parameters.passArguments?.toString() ?: 'true' )
    }

    protected BuildLauncher fillArguments(ExecutionContext context, BuildLauncher buildLauncher) {
        def commandLine = context.commandLine

        List<String> argList = baseArguments ? [baseArguments] : []

        for(Map.Entry<String, Object> entry in commandLine.undeclaredOptions) {
            def flagName = entry.key
            def flag = command.description.getFlag(flagName)
            if(flag) {
                flagName = flag.target ?: "-$flagName".toString()
            }

            argList << flagName
        }

        if(passArguments) {
            argList.addAll(commandLine.remainingArgsArray)
        }


        if(argList) {
            buildLauncher.withArguments(argList as String[])
        }
        buildLauncher
    }

}
