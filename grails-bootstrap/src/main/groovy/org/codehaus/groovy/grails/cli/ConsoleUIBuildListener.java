package org.codehaus.groovy.grails.cli;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import java.io.PrintStream;

/**
 * <p>A class that demonstrates some of the functionality
 * of a custom listener.</p>
 */
public class ConsoleUIBuildListener implements BuildListener {
    private UserInterface ui;
    private PrintStream consoleOut;
    private PrintStream cachedOut = System.out;
    
    public ConsoleUIBuildListener(UserInterface ui, PrintStream consoleOut) {
        this.ui = ui;
        this.consoleOut = consoleOut;
    }
    
    /**
     * <p>Signals that a build has started. This event
     * is fired before any targets have started.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     */
    public final void buildStarted(final BuildEvent start) {
    }

    /**
     * <p>Signals that the last target has finished. This event
     * will still be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void buildFinished(final BuildEvent finish) {
    }

    /**
     * <p>Signals that a target is starting.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getTarget()
     */
    public final void targetStarted(final BuildEvent start) {
    }

    /**
     * <p>Signals that a target has finished. This event will
     * still be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void targetFinished(final BuildEvent finish) {
    }

    /**
     * <p>Signals that a task is starting.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getTask()
     */
    public final void taskStarted(final BuildEvent start) {
        if (start.getTask().getTaskName().equals("input")) {
            setToConsoleOut(start.getProject());
        }
    }

    private final void setToConsoleOut(Project project) {
        for (Object l : project.getBuildListeners()) {
            if (l instanceof BuildLogger) {
                BuildLogger logger = (BuildLogger) l;
                logger.setOutputPrintStream(consoleOut);
           }
        }
    }

    private final void setToCachedOut(Project project) {
        for (Object l : project.getBuildListeners()) {
            if (l instanceof BuildLogger) {
                BuildLogger logger = (BuildLogger) l;
                logger.setOutputPrintStream(cachedOut);
           }
        }
    }

    /**
     * <p>Signals that a task has finished. This event will still
     * be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void taskFinished(final BuildEvent finish) {
        ui.progressTicker("|");
        if (finish.getTask().getTaskName().equals("input")) {
            setToCachedOut(finish.getProject());
        }
    }

    /** <p>When a message is sent to this logger, Ant calls this method.</p>
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged(final BuildEvent event) {
        // empty
    }
}