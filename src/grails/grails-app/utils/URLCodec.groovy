import java.net.URLEncoder
import java.net.URLDecoder
import org.springframework.web.context.request.RequestContextHolder

class URLCodec {
    static encode = { obj ->
        URLEncoder.encode(obj.toString(), URLCodec.getEncoding())
    }

    static decode = { obj ->
        URLEncoder.decode(obj.toString(), URLCodec.getEncoding())
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
