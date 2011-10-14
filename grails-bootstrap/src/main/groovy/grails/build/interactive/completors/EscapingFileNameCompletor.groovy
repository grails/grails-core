package grails.build.interactive.completors

import jline.FileNameCompletor

import java.util.regex.Pattern

/**
 * JLine Completor that does file path matching like FileNameCompletor,
 * but in addition it escapes whitespace in completions with the '\'
 * character.
 *
 * @author Peter Ledbrook
 * @since 2.0
 */
class EscapingFileNameCompletor extends FileNameCompletor {
    /**
     * <p>Gets FileNameCompletor to create a list of candidates and then
     * inserts '\' before any whitespace characters in each of the candidates. 
     * If a candidate ends in a whitespace character, then that is <em>not</em>
     * escaped.</p>
     */
    int complete(String buffer, int cursor, List candidates) {
        def retval = super.complete(buffer, cursor, candidates)

        for (int i = 0; i < candidates.size(); i++) {
            candidates[i] = candidates[i].replaceAll(/(\s)(?!$)/, '\\\\$1')
        }

        return retval
    }
}
