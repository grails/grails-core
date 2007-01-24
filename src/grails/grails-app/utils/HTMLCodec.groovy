import org.springframework.web.util.HtmlUtils

class HTMLCodec {
    static encode = { theTarget ->
        HtmlUtils.htmlEscape(theTarget.toString())
    }
    
    static decode = { theTarget ->
    	HtmlUtils.htmlUnescape(theTarget.toString())
    }
}
