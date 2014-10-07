package org.grails.cli.profile.simple

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.grails.cli.gradle.GradleUtil
import org.grails.cli.profile.ExecutionContext

class GradleCommandStep extends SimpleCommandStep {
    List<String> tasks = []
    String baseArguments = ""
    boolean passArguments = true

    void initialize() {
        tasks = commandParameters.tasks
        baseArguments = commandParameters.baseArguments?:''
        passArguments = Boolean.valueOf(commandParameters.passArguments?:'true')
    }

    @Override
    public boolean handleStep(ExecutionContext context) {
        GradleUtil.withProjectConnection(context.getBaseDir(), false) { ProjectConnection projectConnection ->
            BuildLauncher buildLauncher = projectConnection.newBuild().forTasks(tasks as String[])
            fillArguments(context, buildLauncher)
            GradleUtil.wireCancellationSupport(context, buildLauncher)
            buildLauncher.run()
        }
        return true;
    }

    protected BuildLauncher fillArguments(ExecutionContext context, BuildLauncher buildLauncher) {
        String args = baseArguments
        if(passArguments) {
            String commandLineArgs = context.getCommandLine().getRemainingArgsString()?.trim()
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
