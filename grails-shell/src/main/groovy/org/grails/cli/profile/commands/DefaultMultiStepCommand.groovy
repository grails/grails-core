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
import org.grails.cli.profile.*
import org.grails.cli.profile.steps.StepRegistry
/**
 * Simple implementation of the {@link MultiStepCommand} abstract class that parses commands defined in YAML or JSON
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 * @since 3.0
 */
class DefaultMultiStepCommand extends MultiStepCommand {
    private Map<String, Object> data
    private List<AbstractStep> steps

    final CommandDescription description

    DefaultMultiStepCommand(String name, Profile profile, Map<String, Object> data) {
        super(name, profile)
        this.data = data

        def description = data?.description
        if(description instanceof List) {
            List descList = (List)description
            if(descList) {

                this.description = new CommandDescription(name: name, description: descList.get(0).toString(), usage: data?.usage)

                if(descList.size()>1) {
                    for(arg in descList[1..-1]) {
                        if(arg instanceof Map) {
                            Map map = (Map)arg
                            if(map.containsKey('usage')) {
                                this.description.usage = map.get('usage')?.toString()
                            }
                            else if(map.containsKey('argument')) {
                                map.remove('argument')
                                this.description.argument(map)
                            }
                            else if(map.containsKey('flag')) {
                                map.remove('flag')
                                this.description.flag(map)
                            }
                        }
                    }
                }
            }
        }
        else {
            this.description = new CommandDescription(name: name, description: description.toString(), usage: data?.usage)
        }
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
