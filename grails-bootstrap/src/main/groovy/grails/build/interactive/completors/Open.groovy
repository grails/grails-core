package grails.build.interactive.completors

import jline.SimpleCompletor
import jline.ArgumentCompletor
import jline.FileNameCompletor

/**
 * @author Graeme Rocher
 * @since 2.0
 */
class Open extends ArgumentCompletor{

    Open(String[] candidateStrings) {
        super([new SimpleCompletor("open"), new FileNameCompletor()] )
    }
}
