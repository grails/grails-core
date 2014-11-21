package org.grails.cli.profile.steps

import org.grails.cli.profile.Command
import org.grails.cli.profile.ProfileCommand
import org.grails.cli.profile.Step

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

/**
 * Dynamic creation of {@link Step} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class DefaultStepFactory implements StepFactory {
    @Override
    Step createStep(String name, Command command, Map parameters) {
        if(command instanceof ProfileCommand) {
            switch(name) {
                case 'render':
                    return new RenderStep((ProfileCommand)command, parameters)
                case 'gradle':
                    return new GradleStep((ProfileCommand)command, parameters)
                case 'execute':
                    return new ExecuteStep((ProfileCommand)command, parameters)
                case 'mkdir':
                    return new MkdirStep((ProfileCommand)command, parameters)
            }
        }

    }
}
