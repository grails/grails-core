class ParameterController {
	 String twoParametersView = "someView"
	 Map twoParametersTypedViews = [ "rss" : "someRssView" ]
	 Closure twoParameters = {
		request, response -> return [ "request" : request, "response" : response ]
	}
	 String defaultClosure = "twoParameters"
	
	 String oneParameterView = "someOtherView"
	 Map oneParameterTypedViews = [ "rss" : "someOtherRssView" ]
	 Closure oneParameter = {
		request -> return [ "request" : request ]
	}
}
