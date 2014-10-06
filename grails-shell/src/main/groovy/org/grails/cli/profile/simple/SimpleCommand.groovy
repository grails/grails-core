package org.grails.cli.profile.simple

import groovy.transform.Immutable;

import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext

class SimpleCommand {
    String name
    File file
    Map<String, Object> data
    SimpleProfile profile
    private List<SimpleCommandStep> steps
    int minArguments = 1
    
    CommandDescription getDescription() {
        new CommandDescription(name: name, description: data?.description, usage: data?.usage)
    }

    List<SimpleCommandStep> getSteps() {
        if(steps==null) {
            steps = []
            data.steps?.each { 
                Map<String, Object> stepParameters = it.collectEntries { k,v -> [k as String, v] }
                SimpleCommandStep step = createStep(stepParameters)
                if (step != null) {
                    steps.add(step)
                }
            }
        }
        steps
    }

    protected SimpleCommandStep createStep(Map stepParameters) {
        switch(stepParameters.command) {
            case 'render':
                return new RenderCommandStep(commandParameters: stepParameters, command: this)
            case 'gradle':
                GradleCommandStep step = new GradleCommandStep(commandParameters: stepParameters, command: this)
                step.initialize()
                return step
        }
        return null
    }
        
    public boolean handleCommand(ExecutionContext context) {
        if(minArguments > 0 && (!context.commandLine.getRemainingArgs() || context.commandLine.getRemainingArgs().size() < minArguments)) {
            context.console.error("Expecting ${minArguments ? 'an argument' : minArguments + ' arguments'} to $name.")
            context.console.info("${description.usage}")
            return true
        }
        for(SimpleCommandStep step : getSteps()) {
            if(!step.handleStep(context)) {
                break
            }
        }
        true
    }
}
