package org.grails.bookmarks;

import java.util.List;
/**
 * A generic interface for different types of Bookmark services
 * 
 * @author graemerocher
 */
public interface IBookmarkService {

	/**
	 * Finds a list of the most recent Bookmark instances for the specified
	 * User 
	 * @return A List of Bookmark instances
	 */
	List<Bookmark> findRecent( User user);
	
	/**
	 * Finds a list of all the Bookmark instance for the specified Tag and User
	 * @return A List of Bookmark instances
	 */
	List<Bookmark> findAllForTag( String tag, User user);
}
