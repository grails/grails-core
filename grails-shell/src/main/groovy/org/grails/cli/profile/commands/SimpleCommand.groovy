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
package org.grails.cli.profile.commands

import org.grails.cli.profile.AbstractStep
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.MultiStepCommand
import org.grails.cli.profile.Profile
import org.grails.cli.profile.Step
import org.grails.cli.profile.steps.GradleStep
import org.grails.cli.profile.steps.RenderStep
import org.grails.cli.profile.steps.StepRegistry

/**
 * Simple implementation of the {@link MultiStepCommand} abstract class that parses commands defined in YAML or JSON
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 * @since 3.0
 */
class SimpleCommand extends MultiStepCommand {
    File file
    private Map<String, Object> data
    private List<AbstractStep> steps

    SimpleCommand(String name, Profile profile, File file, Map<String, Object> data) {
        super(name, profile)
        this.file = file
        this.data = data
    }

    CommandDescription getDescription() {
        new CommandDescription(name: name, description: data?.description, usage: data?.usage)
    }

    List<Step> getSteps() {
        if(steps==null) {
            steps = []
            data.steps?.each { 
                Map<String, Object> stepParameters = it.collectEntries { k,v -> [k as String, v] }
                AbstractStep step = createStep(stepParameters)
                if (step != null) {
                    steps.add(step)
                }
            }
        }
        steps
    }

    protected AbstractStep createStep(Map stepParameters) {
        StepRegistry.getStep(stepParameters.command?.toString(), this, stepParameters)
    }

}
