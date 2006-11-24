package org.grails.bookmarks.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.bookmarks.Bookmark;
import org.grails.bookmarks.IBookmarkService;
import org.grails.bookmarks.User;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.UrlPathHelper;

import com.thoughtworks.xstream.XStream;

public class BookmarkXmlController implements Controller {

	private static final Log LOG = LogFactory.getLog(BookmarkXmlController.class);	
	
	private static final String BOOKMARK_API_ALL = "/api/all";
	private static final String BOOKMARK_API_RECENT = "/api/recent";
	
	private IBookmarkService deliciousService;
	private IBookmarkService bookmarkService;
	
	public void setBookmarkService(IBookmarkService bookmarkService) {
		this.bookmarkService = bookmarkService;
	}
	public void setDeliciousService(IBookmarkService deliciousService) {
		this.deliciousService = deliciousService;
	}
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		                    		
		User user = (User)request
							.getSession()
							.getAttribute("user");		
														
		if(user == null) {
			writeError("No user in session. Please authenticate first.", response);
		} 
		else {
			UrlPathHelper helper = new UrlPathHelper();
			String path = helper.getLookupPathForRequest(request);		
			
			List<Bookmark> bookmarks = new ArrayList<Bookmark>();   
			try {  
				boolean success = false; 
				if(path.equals(BookmarkXmlController.BOOKMARK_API_RECENT)) {
					success = handleFindRecent(user, bookmarks);
				}
				else if(path.equals(BookmarkXmlController.BOOKMARK_API_ALL)) {
					String tag = request.getParameter("tag");   				
					success = handleFindAll(tag, user, response, bookmarks);
				}      
				else {
					writeError("API call ["+path+"] not supported.", response);
				} 
				if(success) {    
					response.setContentType("text/xml");   					
					XStream xs = createSerializer();
					xs.toXML(bookmarks, response.getWriter());					
				}
			}
			catch(Exception e) {
				LOG.error("Error invoking bookmark services: " +e.getMessage() , e);
				writeError("Bookmark service is currently unavailable. Please try again later",response );
			}
			
		}   	   	
		return null;
	}

	private boolean handleFindRecent(User user, List<Bookmark> bookmarks) {
		bookmarks.addAll( deliciousService.findRecent(user) );
		bookmarks.addAll( bookmarkService.findRecent(user) );  
		return true;
	}

	private boolean handleFindAll(String tag, User user, HttpServletResponse response, List<Bookmark> bookmarks) {
		if(StringUtils.isBlank(tag)) {
			writeError("Tag parameter not specified", response);
			return false;
		}    
		else {
			bookmarks.addAll( deliciousService.findAllForTag(tag,user) );
			bookmarks.addAll( bookmarkService.findAllForTag(tag,user) ); 
			return true;   								
		}
	}
	/**
	 * Creates a XStream serializer
	 */
	private XStream createSerializer() {
		XStream xs = new XStream();
		xs.alias("bookmark", Bookmark.class);
		xs.alias("bookmarks", List.class);
		xs.omitField(Bookmark.class, "user");
		xs.omitField(Bookmark.class, "tags");
		return xs;
	}
	
	/**
	 * Writes the error response 
	 */
	private void writeError(String message, HttpServletResponse response) {
		response.setContentType("text/xml");   		
		PrintWriter w;
		try {
			w = response.getWriter();
			w.write("<error message=\"");
			w.write(message);
			w.write("\" />");			
		} catch (IOException e) {
			LOG.error("Problem sending <error> tag to response: " + e.getMessage(), e); 
			throw new RuntimeException(e.getMessage(),e);
		}

	}

}
