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
import org.gradle.tooling.ProjectConnection
import org.grails.cli.gradle.GradleUtil
import org.grails.cli.profile.AbstractStep
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProfileCommand

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
        GradleUtil.withProjectConnection(context.baseDir, false) { ProjectConnection projectConnection ->
            BuildLauncher buildLauncher = projectConnection.newBuild().forTasks(tasks as String[])
            fillArguments(context, buildLauncher)
            GradleUtil.wireCancellationSupport(context, buildLauncher)
            buildLauncher.run()
        }
        return true;
    }

    protected void initialize() {
        tasks = parameters.tasks
        baseArguments = parameters.baseArguments ?: ''
        passArguments = Boolean.valueOf(parameters.passArguments?.toString() ?: 'true' )
    }

    protected BuildLauncher fillArguments(ExecutionContext context, BuildLauncher buildLauncher) {
        String args = baseArguments
        if(passArguments) {
            String commandLineArgs = context.commandLine.remainingArgsString?.trim()
            if(commandLineArgs) {
                args += " " + commandLineArgs
            }
        }
        args = args?.trim()
        if(args) {
            buildLauncher.withArguments(args)
        }
        buildLauncher
    }

}
