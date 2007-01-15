import org.springframework.web.util.HtmlUtils

class HTMLEncoder {
    static def encode = { str ->
        HtmlUtils.htmlEscape(str)
    }
}
