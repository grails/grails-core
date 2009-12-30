package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.opensymphony.module.sitemesh.html.util.CharArray;
import com.opensymphony.module.sitemesh.parser.TokenizedHTMLPage;

public class GrailsTokenizedHTMLPage extends TokenizedHTMLPage {
    private CharArray body;
    private CharArray head;
    
	public GrailsTokenizedHTMLPage(char[] original, CharArray body,
			CharArray head) {
		super(original, body, head);
		this.body=body;
		this.head=head;
	}
	
    public void writeHead(Writer out) throws IOException {
    	if(out instanceof PrintWriter) {
    		head.writeTo((PrintWriter)out);
    	} else {
    		super.writeHead(out);
    	}
    }

    public void writeBody(Writer out) throws IOException {
    	if(out instanceof PrintWriter) {
    		body.writeTo((PrintWriter)out);
    	} else {
    		super.writeBody(out);
    	}
    }	
}
