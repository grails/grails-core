/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.codehaus.groovy.grails.web.errors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.exceptions.GrailsException;
import org.codehaus.groovy.grails.exceptions.SourceCodeAware;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  An exception that wraps a Grails RuntimeException and attempts to extract more relevent diagnostic messages from the stack trace
 * 
 * @author Graeme Rocher
 * @since 0.1
 *
 * Created: 22 Dec, 2005
 */
public class GrailsWrappedRuntimeException extends GrailsException {

    private static final Pattern PARSE_DETAILS_STEP1 = Pattern.compile("\\((\\w+)\\.groovy:(\\d+)\\)");
    private static final Pattern PARSE_DETAILS_STEP2 = Pattern.compile("at\\s{1}(\\w+)\\$_closure\\d+\\.doCall\\(\\1:(\\d+)\\)");
    private static final Pattern PARSE_GSP_DETAILS_STEP1 = Pattern.compile("(\\S+?)_\\S+?_gsp.run\\((\\S+?\\.gsp):(\\d+)\\)");
    public static final String URL_PREFIX = "/WEB-INF/grails-app/";
    private static final Log LOG  = LogFactory.getLog(GrailsWrappedRuntimeException.class);
    private String className = UNKNOWN;
    private int lineNumber = - 1;
    private String stackTrace;
    private String[] codeSnippet = new String[0];
    private String gspFile;
	private Throwable cause;
    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private String[] stackTraceLines;
    private static final String UNKNOWN = "Unknown";
    private String fileName;


    /**
     * @param servletContext The ServletContext instance
     * @param t The exception that was thrown
     */
    public GrailsWrappedRuntimeException(ServletContext servletContext, Throwable t) {
        super(t.getMessage(), t);
        this.cause = t;
        FastStringWriter pw = new FastStringWriter();
        cause.printStackTrace(pw);
        this.stackTrace = pw.toString();

        while(cause.getCause()!=cause) {
        	if(cause.getCause() == null) {
        		break;
        	}
        	cause = cause.getCause();
        }
        
        this.stackTraceLines = this.stackTrace.split("\\n");

        if(cause instanceof MultipleCompilationErrorsException) {
        	MultipleCompilationErrorsException mcee = (MultipleCompilationErrorsException)cause;
        	Object message = mcee.getErrorCollector().getErrors().iterator().next();
        	if(message instanceof SyntaxErrorMessage) {
        		SyntaxErrorMessage sem = (SyntaxErrorMessage)message;
        		this.lineNumber = sem.getCause().getLine();
                sem.write(pw);
        	}
        	
        }
        else {

            Matcher m1 = PARSE_DETAILS_STEP1.matcher(stackTrace);
            Matcher m2 = PARSE_DETAILS_STEP2.matcher(stackTrace);
            Matcher gsp = PARSE_GSP_DETAILS_STEP1.matcher(stackTrace);
            try {
                if(gsp.find()) {
                    this.className = gsp.group(2);
                    this.lineNumber = Integer.parseInt(gsp.group(3));
                    this.gspFile = URL_PREFIX + "views/" + gsp.group(1)  + '/' + this.className;
                }
                else {
                    if(m1.find()) {
                        do {
                            this.className = m1.group(1);
                            this.lineNumber = Integer.parseInt(m1.group(2));
                        }  while(m1.find());
                    }
                    else {
                        while(m2.find()) {
                            this.className = m2.group(1);
                            this.lineNumber = Integer.parseInt(m2.group(2));                            
                        }
                    }
                }
            }
            catch(NumberFormatException nfex) {
                // ignore
            }

        }
        LineNumberReader reader = null;
        try {
            checkIfSourceCodeAware(cause);
            checkIfSourceCodeAware(t);




            if(getLineNumber() > -1) {
                String fileLocation;
                String url = null;

                if(fileName != null) {
                    fileLocation = fileName;
                }
                else {

                    String urlPrefix = "";
                    if(gspFile == null) {
                        this.fileName = this.className.replace('.', '/') + ".groovy";

                        GrailsApplication application = ApplicationHolder.getApplication();
                        // @todo Refactor this to get the urlPrefix from the ArtefactHandler
                        if(application.isArtefactOfType(ControllerArtefactHandler.TYPE, className)) {
                            urlPrefix += "/controllers/";
                        }
                        else if(application.isArtefactOfType(TagLibArtefactHandler.TYPE, className)) {
                            urlPrefix += "/taglib/";
                        }
                        else if(application.isArtefactOfType(ServiceArtefactHandler.TYPE, className)) {
                            urlPrefix += "/services/";
                        }
                        url = URL_PREFIX + urlPrefix + fileName;
                    }
                    else {
                        url = gspFile;
                        GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(servletContext);
                        GroovyPagesTemplateEngine engine = attrs.getPagesTemplateEngine();
                        int[] lineNumbers = engine.calculateLineNumbersForPage(servletContext,url);
                        if(this.lineNumber < lineNumbers.length) {
                            this.lineNumber = lineNumbers[this.lineNumber - 1];
                        }
                    }
                    fileLocation = "grails-app" + urlPrefix + fileName;

                }



                InputStream in = null;
                if(!StringUtils.isBlank(url)) {
                    in = servletContext.getResourceAsStream(url);
                    LOG.debug("Attempting to display code snippet found in url " + url);

                }
                if(in == null) {
                    Resource r = null;
                    try {
                        r = resolver.getResource(fileLocation);
                        in = r.getInputStream();
                    } catch (Throwable e) {
                        r = resolver.getResource("file:" + fileLocation);
                        if(r.exists()) {
                            try {
                                in = r.getInputStream();
                            }
                            catch (IOException e1) {
                                // ignore
                            }
                        }
                    }
                }

                if(in != null) {
                    reader = new LineNumberReader(new InputStreamReader( in ));
                    String currentLine = reader.readLine();
                    StringBuilder buf = new StringBuilder();
                    while(currentLine != null) {

                        int currentLineNumber = reader.getLineNumber();
                        if((this.lineNumber>0 && currentLineNumber == lineNumber-1)||(currentLineNumber == this.lineNumber)) {
                            buf.append(currentLineNumber)
                               .append(": ")
                               .append(currentLine)
                               .append("\n");

                        }
                        else if(currentLineNumber == this.lineNumber + 1) {
                            buf.append(currentLineNumber)
                               .append(": ")
                               .append(currentLine);
                            break;
                        }
                        currentLine = reader.readLine();
                    }
                    this.codeSnippet = buf.toString().split("\n");
                }
            }
        }
        catch (IOException e) {
            LOG.warn("[GrailsWrappedRuntimeException] I/O error reading line diagnostics: " + e.getMessage(), e);
        }
        finally {
            if(reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
        }
        
    }

    private void checkIfSourceCodeAware(Throwable cause) {
        if(cause instanceof SourceCodeAware) {
            final SourceCodeAware codeAware = (SourceCodeAware) cause;
            if(codeAware.getFileName()!= null) {
                if(className == null || UNKNOWN.equals(className)) {
                    className = codeAware.getFileName();
                    fileName = codeAware.getFileName();
                }
            }
            if(codeAware.getLineNumber()>-1)
                lineNumber = codeAware.getLineNumber();
        }
    }

    /**
     * @return Returns the line.
     */
    public String[] getCodeSnippet() {
        return this.codeSnippet;
    }

    /**
     * @return Returns the className.
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return Returns the lineNumber.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * @return Returns the stackTrace.
     */
    public String getStackTraceText() {
        return stackTrace;
    }

    /**
     * @return Returns the stackTrace lines
     */
    public String[] getStackTraceLines() {
        return stackTraceLines;
    }

    /* (non-Javadoc)
      * @see groovy.lang.GroovyRuntimeException#getMessage()
      */
    public String getMessage() {
        return cause.getMessage();
    }


}
