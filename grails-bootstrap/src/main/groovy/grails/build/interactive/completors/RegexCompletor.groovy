package grails.build.interactive.completors

import jline.Completor

import java.util.regex.Pattern

/**
 * JLine Completor that accepts a string if it matches a given regular
 * expression pattern.
 *
 * @author Peter Ledbrook
 * @since 2.0
 */
class RegexCompletor implements Completor {
    Pattern pattern

    RegexCompletor(String pattern) {
        this(Pattern.compile(pattern))
    }

    RegexCompletor(Pattern pattern) {
        this.pattern = pattern
    }

    /**
     * <p>Check whether the whole buffer matches the configured pattern.
     * If it does, the buffer is added to the <tt>candidates</tt> list
     * (which indicates acceptance of the buffer string) and returns 0,
     * i.e. the start of the buffer. This mimics the behaviour of SimpleCompletor.
     * </p>
     * <p>If the buffer doesn't match the configured pattern, this returns
     * -1 and the <tt>candidates</tt> list is left empty.</p>
     */
    int complete(String buffer, int cursor, List candidates) {
        if (buffer ==~ pattern) {
            candidates << buffer
            return 0
        }
        else return -1
    }
}
