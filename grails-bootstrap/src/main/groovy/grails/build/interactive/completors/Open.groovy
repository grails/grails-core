package grails.build.interactive.completors

import jline.ArgumentCompletor
import jline.SimpleCompletor

import org.codehaus.groovy.grails.cli.interactive.InteractiveMode

/**
 * @author Graeme Rocher
 * @since 2.0
 */
class Open extends ArgumentCompletor {

    Open() {
        super([ new SimpleCompletor("open"), new SimpleOrFileNameCompletor(InteractiveMode.FIXED_OPEN_OPTIONS) ])
    }
}
