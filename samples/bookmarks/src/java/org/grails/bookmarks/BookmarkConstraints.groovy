package org.grails.bookmarks

constraints = {
	title(blank:false)		
	rating(range:1..10)
	type(inList:['blog','article','general','news'])		
	notes(maxSize:1000)	
}