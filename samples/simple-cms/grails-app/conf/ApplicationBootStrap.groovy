class ApplicationBootStrap {


     def Closure init = { servletContext ->
	 
        // create default site
		def defaultSite = Site.findByName(Site.DEFAULT)
		def homePage = null 
        if(!defaultSite) {
            defaultSite = new Site(name:Site.DEFAULT,domain:ApplicationConfig.DEFAULT_SITE_DOMAIN).save()
            homePage = new Page(title:'Home Page',
                                    template:'default',
                                    site:defaultSite,
                                    content:'Welcome to the HomePage!'
									)   
            defaultSite.homePage = homePage									

        }


        // create system roles
        def sa = Role.findByName(Role.SYSTEM_ADMINISTRATOR)
        if(!sa) {
            sa = new Role(title:'Default '+Role.SYSTEM_ADMINISTRATOR,name:Role.SYSTEM_ADMINISTRATOR,site:defaultSite)
            sa.save()
        }
        if(!Role.findByName(Role.CONTENT_EDITOR))
            new Role(title:'Default '+Role.CONTENT_EDITOR,name:Role.CONTENT_EDITOR,site:defaultSite).save()
        if(!Role.findByName(Role.CONTENT_APPROVER))
            new Role(title:'Default '+Role.CONTENT_APPROVER,name:Role.CONTENT_APPROVER,site:defaultSite).save()
        if(!Role.findByName(Role.GENERAL_USER))
            new Role(title:'Default '+Role.GENERAL_USER,name:Role.GENERAL_USER,site:defaultSite).save()
		
        // create the default user
		def defaultUser = User.findByLogin(ApplicationConfig.DEFAULT_USER)
        if(!defaultUser) {
            defaultUser = new User( title: 'Mr',
                              firstName: 'System',
                              lastName: 'Administrator',
                              email: ApplicationConfig.DEFAULT_USER_EMAIL,
                              company: 'None',
                              login: ApplicationConfig.DEFAULT_USER,
                              pwd: 'letmein',
                              role: sa
                              ).save()

        }	


        defaultSite.homePage.createdBy = defaultUser
        defaultSite.save()
        
		if(homePage) {
			 homePage.addRevision(new Revision(	updatedBy:defaultUser,
			 										page:homePage,
			 										content:homePage.content,
													state: Revision.PUBLISHED ))
			homePage.save()													
		}
     }
     def Closure destroy = {
     }
} 