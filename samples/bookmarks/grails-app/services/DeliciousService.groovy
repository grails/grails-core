import org.grails.bookmarks.*
import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod

class DeliciousService implements IBookmarkService {
	
	static final API = "https://api.del.icio.us/v1"
	
	boolean transactional = false

	List findAllForTag(String tag,User user) {
		return withDelicious(name:"posts/all",params:[tag:tag],user:user) { xml ->
			def bookmarks = []
			xml.post.each {
				bookmarks << new Bookmark(title:"${it.@description}",url:"${it.@href}")
			}
			return bookmarks			
		}
	}
	
	List findRecent(User user) {
		return withDelicious(name:"posts/recent",params:[count:5],user:user) { xml ->
			def bookmarks = []
			xml.post.each {
				bookmarks << new Bookmark(title:"${it.@description}",url:"${it.@href}")
			}
			return bookmarks			
		}		
	}
	
	private withDelicious(args,callable) {
		if(!args.user) throw new IllegalArgumentException("Property [user] is required and cannot be null")

	
		def url = "${API}/${args.name}?"
		args.params?.each {k,v->
			url+="&$k=$v"
		}
		
		def client = new HttpClient()
		client
		 .state
		 .setCredentials(AuthScope.ANY,new UsernamePasswordCredentials(args.user.login, args.user.password))
				
        def get = new GetMethod(url)
		get.doAuthentication = true
        client.executeMethod(get)		

		callable( new XmlSlurper().parse(get.responseBodyAsStream) )		
	}
}

