import org.grails.bookmarks.*

class DeliciousTests extends GroovyTestCase {

	DeliciousService deliciousService
	
	void testFindAllForTag() {
		assert deliciousService != null  
		try {
			def user = new User(login:'graeme.rocher',password:'...')
			def bookmarks = deliciousService.findAllForTag(new Tag(name:'grails'),user)
			assert bookmarks.size() > 0
			def b =  bookmarks.find { it.url == new URI("http://grails.org/") }
			assert b != null			
		}                   
		catch(Exception e) {
			println "Warning: Calling del.icio.us produced error: ${e.message}" 
		}
	}
}
