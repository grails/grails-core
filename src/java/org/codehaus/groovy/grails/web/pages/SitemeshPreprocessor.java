package org.codehaus.groovy.grails.web.pages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * This class is used to add GSP Sitemesh integration directly to compiled GSP.
 * 
 * head, meta, title, body and component tags are replaced with <g:capture*>...</g:capture*> taglibs
 * 
 * The taglib is used to capture the content of each tag. This prevents the need to parse the content output like Sitemesh normally does.
 *  
 * 
 * @author <a href="mailto:lari.hotari@sagire.fi">Lari Hotari, Sagire Software Oy</a> 
 *
 */
public class SitemeshPreprocessor {
	Pattern metaPattern=Pattern.compile("<meta(\\s[^>]+)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	Pattern titlePattern=Pattern.compile("<title(\\s[^>]*)?>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	Pattern headPattern=Pattern.compile("<head(\\s[^>]*)?>(.*?)</head>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	Pattern bodyPattern=Pattern.compile("<body(\\s[^>]*)?>(.*?)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	Pattern componentPattern=Pattern.compile("<component(\\s+tag[^>]+)>(.*?)</component>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	public String addGspSitemeshCapturing(String gspSource) {
		StringBuffer sb = addHeadCapturing(gspSource);
		sb = addBodyCapturing(sb);
		sb = addComponentCapturing(sb);
		return sb.toString();
	}

	StringBuffer addHeadCapturing(String gspSource) {
		StringBuffer sb=new StringBuffer((int)(gspSource.length() * 1.2));
		Matcher m=headPattern.matcher(gspSource);
		if(m.find()) {
			m.appendReplacement(sb, "");
			sb.append("<g:captureHead");
			if(m.group(1) != null)
				sb.append(m.group(1));
			sb.append(">");
			sb.append(addMetaCapturing(addTitleCapturing(m.group(2))));
			sb.append("</g:captureHead>");
		}
		m.appendTail(sb);
		return sb;
	}
	
	String addMetaCapturing(String headContent) {
		StringBuffer sb=new StringBuffer((int)(headContent.length() * 1.2));
		Matcher m=metaPattern.matcher(headContent);
		while(m.find()) {
			m.appendReplacement(sb, "");
			sb.append("<g:captureMeta");
			String tagContent=m.group(1);
			sb.append(tagContent);
			if(!tagContent.endsWith("/")) {
				sb.append("/");
			}
			sb.append(">");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	String addTitleCapturing(String headContent) {
		StringBuffer sb=new StringBuffer((int)(headContent.length() * 1.2));
		Matcher m=titlePattern.matcher(headContent);
		if(m.find()) {
			m.appendReplacement(sb, "");
			sb.append("<g:captureTitle");
			if(m.group(1) != null)
				sb.append(m.group(1));
			sb.append(">");
			sb.append(m.group(2));
			sb.append("</g:captureTitle>");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	StringBuffer addBodyCapturing(StringBuffer sb) {
		StringBuffer sb2=new StringBuffer((int)(sb.length() * 1.2));
		Matcher m=bodyPattern.matcher(sb);
		if(m.find()) {
			m.appendReplacement(sb2, "");
			sb2.append("<g:captureBody");
			if(m.group(1) != null)
				sb2.append(m.group(1));
			sb2.append(">");
			sb2.append(m.group(2));
			sb2.append("</g:captureBody>");
		}
		m.appendTail(sb2);
		return sb2;
	}

	StringBuffer addComponentCapturing(StringBuffer sb) {
		StringBuffer sb2=new StringBuffer((int)(sb.length() * 1.2));
		Matcher m=componentPattern.matcher(sb);
		if(m.find()) {
			m.appendReplacement(sb2, "");
			sb2.append("<g:captureComponent").append(m.group(1)).append(">");
			sb2.append(m.group(2));
			sb2.append("</g:captureComponent>");
		}
		m.appendTail(sb2);
		return sb2;
	}
}
