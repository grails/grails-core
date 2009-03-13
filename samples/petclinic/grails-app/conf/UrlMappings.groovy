class UrlMappings {
    static mappings = {
	  "/"(controller:'clinic')
      "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
	  "500"(view:'/error')
	}
}
