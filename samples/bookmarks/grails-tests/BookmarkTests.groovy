import org.grails.bookmarks.*
import groovy.mock.interceptor.*

class BookmarkTests extends GroovyTestCase {
        
	User user  
	Bookmark b1
	Bookmark b2
	Bookmark b3
	Bookmark b4
	
	void setUp() {
		user = User.findByLogin("Graeme")     
 		if(!user)user = new User(login:"Graeme",
							password:"test",
							firstName:"Graeme",
							lastName:"Rocher",
							email:"graeme@grails.org").save()
									
        b1 = new Bookmark(title:"SkillsMatter",url:"http://www.skillsmatter.com",user:user)
		b1.save()
        b2 = new Bookmark(title:"Canoo",url:"http://www.canoo.com",user:user).save()
        b3 = new Bookmark(title:"Grails",url:"http://grails.org",user:user).save()
        b4 = new Bookmark(title:"Groovy",url:"http://groovy.codehaus.org",user:user).save() 
        
		b1.errors.allErrors.each { println it }
  		assert b1 != null
		assert b2 != null
		assert b3 != null
		assert b4 != null
	}
		
	void testEditInPlace() {
		def tagLibStub = new StubFor(BookmarkTagLib)
		
		def sw = new StringWriter()
		def link = "http://grails.org"
		def paramName = "testParam"
		def editorId = "testEditor"
		def rowCount = 10
		def colCount = 5
		def attrs = [rows:rowCount,cols:colCount,paramName:paramName,id:editorId]
		    
		tagLibStub.demand.setOut { new PrintWriter(sw)  }
		tagLibStub.demand.createLink { arg -> sw << link }
		
		tagLibStub.use {
			def tl = new BookmarkTagLib()
			tl.out =  new PrintWriter(sw) 
			def eip = tl.editInPlace
			eip.call(attrs, { tl.out << "test"})
			
			def mkp = sw.toString()
			println(mkp)
			assert mkp.startsWith("<span id='${editorId}'>test</span>")		
			assert mkp.endsWith("<script type='text/javascript'>new Ajax.InPlaceEditor('${editorId}',"+
			                    " '${link}'," +
			                    "{rows:${rowCount},cols:${colCount},"+
			                    "callback: function(form, value) { return '${paramName}=' + escape(value) }"+
			                    "});</script>")
		}
	}     
	
	void testBadUrl() {
		def badUrl = "aninvalidurl"
		def b = new Bookmark()    
		shouldFail(MalformedURLException) {
			b.url = new URI(badUrl)    			
		}
	}     
	      
	void testListAction() {
		def bc = new BookmarkController()
		bc.session.user = user
		def bookmarks = bc.list()?.bookmarkList
		assert bookmarks != null
		assertLength(4, bookmarks as Object[])   
		assert bookmarks[0].url?.toString() == "http://www.skillsmatter.com"
		assert bookmarks[1].url?.toString() == "http://www.canoo.com"
		assert bookmarks[2].url?.toString() == "http://grails.org"				
		assert bookmarks[3].url?.toString() == "http://groovy.codehaus.org"
	}   
	
	void testListActionPaginate() {
		def bc = new BookmarkController() 
		bc.session.user = user		
		bc.params.offset = 2
		bc.params.max = 2
		def bookmarks = bc.list()?.bookmarkList
		assert bookmarks != null
		assertLength(2, bookmarks as Object[])    
		assert bookmarks[0].url?.toString() == "http://grails.org"				
		assert bookmarks[1].url?.toString() == "http://groovy.codehaus.org"		
	}
	    
	BookmarkController bc
	final Closure UPDATE_TEST_CODE =  {  id ->
		bc  = new BookmarkController()  
		bc.params.id = id
     	bc.update.call()
	}  
	     
	void testUpdateNotFound() {
		def mock1 = new MockFor(BookmarkController)                         
		def mock2 = new MockFor(Bookmark) 		
		
		mock2.demand.get { return null }
		mock1.demand.render { String text -> 
			assert text == "Bookmark not found"
		}   
		mock1.use {
			mock2.use(UPDATE_TEST_CODE.curry(5))			
		}
	}
	
	void testUpdateSuccess() {        
		def mock1 = new MockFor(BookmarkController) 		
		def mock2 = new MockFor(Bookmark)
		def b = Bookmark.findByTitle('Grails')        
		mock2.demand.get {  
			return b
		}		            
		mock2.demand.setProperties {}    
		mock1.demand.addTags {}
		mock2.demand.save {
			return true
		}
		mock1.demand.render { Map params ->
			assert params.template == 'bookmark'
			assert params.model?.bookmark == b
		}   	
		mock1.use {
			mock2.use(UPDATE_TEST_CODE.curry(b.id))
		}  
	}     
	
	void testUpdateFailure() {
		def mock1 = new MockFor(BookmarkController) 		
		def mock2 = new MockFor(Bookmark)        
		def b = Bookmark.findByTitle('Grails')        
		mock2.demand.get {   
			b.title = "" 			
			return b
		}		   
		mock2.demand.setProperties {}    
		mock1.demand.addTags {}
		mock2.demand.save {
			return false
		}		
		mock1.demand.render { Map params ->
			assert params.view == 'edit'
			assert params.model?.bookmark == b
		}  
		mock1.use {
			mock2.use(UPDATE_TEST_CODE.curry(b.id))
		}		           
	}
	
    void testSortBookmarks() {

        def sortUrlsBy = {  property ->  
           Bookmark.findAll().sort { it."$property" }.url  
        }
        def urls = sortUrlsBy("title")
        assert urls != null
        assert urls.size() == 4
        assert urls[0].toString() == "http://www.canoo.com"
        assert urls[1].toString() == "http://grails.org"
    }
	     
	void tearDown() {
        b1?.delete()
        b2?.delete()
        b3?.delete()
        b4?.delete()
        user?.delete()
  		
	}
	
}
