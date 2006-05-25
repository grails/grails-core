class ApplicationConfig {
    static final DEFAULT_SITE_DOMAIN = 'localhost'
    static final DEFAULT_USER = 'admin'
    static final DEFAULT_USER_EMAIL = 'system@grails.codehaus.org'	
	static final EMAIL_SERVER  = "localhost"
	static final DEFAULT_SITE = "Default"
	static final SYSTEM_EMAIL = "support@localhost"
	static final SYSTEM_NAME = "System Support"	
    // the criteria that is evaluates to decided with a page is displayed in the 
	// site structure
	static final PAGE_DISPLAY_CRITERIA = { c -> 
			c.revisions?.last().state != Revision.DELETED && c.revisions?.find { it.state==Revision.PUBLISHED } 
	}
	// calculates the levels in the site structure 
	static final CALCULATE_LEVELS = { p ->
		def level = 0
		def pageLevel = p
		def levels = [:]
		while(pageLevel) {
			pageLevel = pageLevel.parent
			level++
		}
		for(i in 1..level) {
			def ancestor = p
			i.times {
				if(ancestor) {					
					ancestor = ancestor.parent
				}
			}
			if(ancestor) {
				levels.put('level' + (level-i), ancestor)
			}
		}
		levels.put('level'+level,p)
		return levels		
	}
}
