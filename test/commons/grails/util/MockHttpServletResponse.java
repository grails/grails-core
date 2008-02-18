package grails.util;

/**
 * Subclass Spring's MockHttpServletResponse to tag the methods that have been deprecated in
 * the Servlet API.  
 *
 * Because Spring's MockHttpServletResponse doesn't tag these methods as deprecated, the 
 * compiler outputs noisy warnings complaining that we're using deprecated methods if we use
 * the raw MockHttpServletResponse from Spring.  By subclassing Spring's 
 * MockHttpServletResponse and tagging the methods as deprecated, we acknowledge to the 
 * compiler that these methods are deprecated, and we silence the compiler warnings.  
 *
 * Created: 08-Feb-2008
 */
class MockHttpServletResponse extends org.springframework.mock.web.MockHttpServletResponse {
  /** @deprecated */
	public String encodeRedirectUrl(String url) {
		return super.encodeRedirectURL(url);
	}

  /** @deprecated */
	public String encodeUrl(String url) {
		return super.encodeURL(url);
	}

  /** @deprecated */
	public void setStatus(int status, String errorMessage) {
		super.setStatus(status, errorMessage);
	}
}
