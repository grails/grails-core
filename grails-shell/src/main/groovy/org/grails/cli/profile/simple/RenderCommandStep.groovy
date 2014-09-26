package org.grails.cli.profile.simple

import org.grails.cli.profile.ExecutionContext

class RenderCommandStep extends SimpleCommandStep {

    @Override
    public boolean handleStep(ExecutionContext context) {
        context.console.info("-render- $commandParameters")
        return true
    }
}
