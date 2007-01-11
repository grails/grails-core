import org.springframework.web.util.HtmlUtils

class HTMLEncoder {
    def encode = { str ->
        HtmlUtils.htmlEscape(str)
    }
}
