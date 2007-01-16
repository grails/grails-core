import org.springframework.web.util.HtmlUtils

class HTMLCodec {
    static def encode = { str ->
        HtmlUtils.htmlEscape(str)
    }
    
    static def decode = { str ->
    	HtmlUtils.htmlUnescape(str)
    }
}
