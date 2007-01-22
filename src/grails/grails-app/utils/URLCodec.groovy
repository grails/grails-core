import java.net.URLEncoder
import java.net.URLDecoder
import org.springframework.web.context.request.RequestContextHolder

class URLCodec {
    static def encode = { str ->
        URLEncoder.encode(str, URLCodec.getEncoding())
    }

    static def decode = { str ->
        URLEncoder.decode(str, URLCodec.getEncoding())
    }

	private static def getEncoding() {
		def request = RequestContextHolder.currentRequestAttributes().currentRequest
		def encoding = "UTF-8"
		if (request?.characterEncoding) {
			encoding = request?.characterEncoding
		}
		return encoding
	}
}
