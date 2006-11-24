import org.grails.bookmarks.*

abstract class SecureController {
	def beforeInterceptor = {
			if(!session.user) {
				session.user = User.findByLogin(request.remoteUser)
			}
	}

}

