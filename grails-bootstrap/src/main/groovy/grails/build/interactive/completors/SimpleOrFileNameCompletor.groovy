package grails.build.interactive.completors

import jline.Completor
import jline.SimpleCompletor

/**
 * JLine Completor that mixes a fixed set of options with file path matches.
 * Fixed options that match will appear first, followed by file path matches.
 *
 * @author Peter Ledbrook
 * @since 2.0
 */
class SimpleOrFileNameCompletor implements Completor {
    private simpleCompletor
    private fileNameCompletor

    SimpleOrFileNameCompletor(List fixedOptions) {
        this(fixedOptions as String[])
    }

    SimpleOrFileNameCompletor(String[] fixedOptions) {
        this.simpleCompletor = new SimpleCompletor(fixedOptions)
        this.fileNameCompletor = new EscapingFileNameCompletor()
    }

    int complete(String buffer, int cursor, List candidates) {
        // Try the simple completor first...
        def retval = this.simpleCompletor.complete(buffer, cursor, candidates)

        // ...and then the file path completor. By using the given candidate
        // list with both completors we aggregate the results automatically.
        def fileRetval = this.fileNameCompletor.complete(buffer, cursor, candidates)

        // If the simple completor has matched, we return its value, otherwise
        // we return whatever the file path matcher returned. This ensures that
        // both simple completor and file path completor candidates appear
        // correctly in the command prompt. If neither competors have matches,
        // we of course return -1.
        if (retval == -1) retval = fileRetval
        return candidates ? retval : -1
    }
}
