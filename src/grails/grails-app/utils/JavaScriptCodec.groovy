import org.springframework.web.util.JavaScriptUtils

class JavaScriptCodec {
    static encode = { theTarget ->
        JavaScriptUtils.javaScriptEscape(theTarget.toString())
    }
}