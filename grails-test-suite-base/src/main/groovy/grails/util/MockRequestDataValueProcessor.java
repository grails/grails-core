package grails.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

public class MockRequestDataValueProcessor implements org.springframework.web.servlet.support.RequestDataValueProcessor {
	public Map<String,String> getExtraHiddenFields(HttpServletRequest request) {
		Map<String,String> extraHiddenFields = new HashMap<String,String>();
		extraHiddenFields.put("requestDataValueProcessorHiddenName","hiddenValue");
		return extraHiddenFields;
	}
	public String processAction(HttpServletRequest request, String action) {
		String resultAction = action;
		if(action.indexOf("requestDataValueProcessorParamName=paramValue")>-1) {
			resultAction = resultAction.replace("?requestDataValueProcessorParamName=paramValue&","?");
			resultAction = resultAction.replace("?requestDataValueProcessorParamName=paramValue","");
			resultAction = resultAction.replace("&requestDataValueProcessorParamName=paramValue","");
		}
		return resultAction;
 	}
	public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
		return value+"_PROCESSED_";
	}
 	public String processUrl(HttpServletRequest request, String url) {
 		String resultUrl = url;
 		String toAppend = null;
 		if(resultUrl.indexOf("?")>-1) {
 			toAppend = "&requestDataValueProcessorParamName=paramValue";
 		} else {
 			toAppend = "?requestDataValueProcessorParamName=paramValue";
 		}
 		if(resultUrl.indexOf("#") > -1) {
 			resultUrl = resultUrl.replace("#",toAppend+"#");
 		} else {
 			resultUrl = resultUrl + toAppend;
 		}
 		return resultUrl;
 	}
}