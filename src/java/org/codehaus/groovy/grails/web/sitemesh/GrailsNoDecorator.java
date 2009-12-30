package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.sitemesh.webapp.decorator.BaseWebAppDecorator;


/**
 * Grails version of Sitemesh's NoDecorator
 * 
 * original version always calls response.setContentLength which would require the calculation of 
 * resulting bytes. Calculation would be extra overhead.
 * 
 * bug exists for OutputStream / byte version: http://jira.opensymphony.com/browse/SIM-196
 * skip setting ContentLength because of that bug.
 * 
 * @author Lari Hotari, Sagire Software Oy
 */
public class GrailsNoDecorator extends BaseWebAppDecorator {

    protected void render(Content content, HttpServletRequest request, HttpServletResponse response,
                          ServletContext servletContext, SiteMeshWebAppContext webAppContext)
            throws IOException, ServletException {

        if (webAppContext.isUsingStream()) {
        	// http://jira.opensymphony.com/browse/SIM-196 , skip setting setContentLength
            //response.setContentLength(content.originalLength());
            OutputStream output=response.getOutputStream();
        	PrintWriter writer = new PrintWriter(output);
            content.writeOriginal(writer);
            writer.flush();
        } else {
            PrintWriter writer = response.getWriter();
            content.writeOriginal(writer);
            writer.flush();
        }
    }

}
