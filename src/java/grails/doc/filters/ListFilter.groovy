package grails.doc.filters
/**
 * @author Graeme Rocher
 * @since 1.1
 */

import org.radeox.regex.MatchResult
import org.radeox.filter.context.FilterContext

class ListFilter extends org.radeox.filter.ListFilter{

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        super.handleMatch(buffer, result, context);
        buffer << "\n\n"
    }



}