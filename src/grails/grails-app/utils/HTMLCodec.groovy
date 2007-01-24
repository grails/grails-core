import org.springframework.web.util.HtmlUtils

class HTMLCodec {
    static def encode = { theTarget ->
        HtmlUtils.htmlEscape(theTarget.toString())
    }
    
    static def decode = { theTarget ->
    	HtmlUtils.htmlUnescape(theTarget.toString())
    }
}
