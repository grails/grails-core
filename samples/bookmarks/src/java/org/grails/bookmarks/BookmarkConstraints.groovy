package org.grails.bookmarks

constraints = {
	title(blank:false)		
	rating(range:1..10)
	type(inList:['blog','article','general','news'])		
	notes(maxLength:1000)	
}