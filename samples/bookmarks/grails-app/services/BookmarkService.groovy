import org.grails.bookmarks.Bookmark
import org.grails.bookmarks.User
import org.grails.bookmarks.Tag

class BookmarkService implements org.grails.bookmarks.IBookmarkService {
	
	boolean transactional = false
	
	List findAllForTag(String tag,User user) {		
		def t = tag ? Tag.findByName(tag) : null   
		if(user) user = User.findByLogin(user.login)		
		List bookmarks  = Collections.EMPTY_LIST
		if(t && user) {
			def criteria = Bookmark.createCriteria()
			bookmarks = criteria.list {
				eq('user',user)
				tags {
					eq('tag', t)
				}					
			}     
		}
		return bookmarks
	}
	
	List findRecent(User user) {
		def now = new Date()   
		if(user) user = User.findByLogin(user.login)
		if(user) {  		
			return Bookmark
					.findAllByUserAndDateCreatedBetween(user,now-7,now)
		}
	}
}

