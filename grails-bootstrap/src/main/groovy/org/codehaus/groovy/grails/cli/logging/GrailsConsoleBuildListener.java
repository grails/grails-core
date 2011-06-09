package org.codehaus.groovy.grails.cli.logging;

import grails.build.logging.GrailsConsole;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

/**
 * Mainly silences a lot of redundant Ant output
 *
 */
public class GrailsConsoleBuildListener implements BuildListener {
    private GrailsConsole ui;

    public GrailsConsoleBuildListener() {
        ui = GrailsConsole.getInstance();
    }

    public GrailsConsoleBuildListener(GrailsConsole ui) {
        this.ui = ui;
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
        ui.indicateProgress();
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
