package grails.doc.filters
/**
 * @author Graeme Rocher
 * @since 1.1
 */

import org.radeox.filter.regex.RegexFilter
import org.radeox.filter.context.FilterContext
import org.radeox.filter.regex.RegexTokenFilter
import org.radeox.regex.MatchResult

class HeaderFilter extends RegexTokenFilter{

    public HeaderFilter() {
        super(/(?m)^h(\d)\.\s+?(.*?)$/);
    }


    public void handleMatch(StringBuffer out, MatchResult matchResult, FilterContext filterContext) {

          def header = matchResult.group(1)
          def content = matchResult.group(2)
          out << "<h$header>$content</h$header>"
    }


}