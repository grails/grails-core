package org.codehaus.groovy.grails.support.encoding;

import java.io.IOException;
import java.io.Writer;

public class WriterEncodedAppender extends AbstractEncodedAppender {
    private Writer target;
    public WriterEncodedAppender(Writer target) {
        this.target=target;
    }

    public void flush() throws IOException {
        target.flush();
    }

    @Override
    protected void write(EncodingState encodingState, char[] b, int off, int len) throws IOException {
        target.write(b, off, len);
    }

    @Override
    protected void write(EncodingState encodingState, String str, int off, int len) throws IOException {
        target.write(str, off, len);        
    }

    @Override
    protected void appendCharSequence(EncodingState encodingState, CharSequence csq, int start, int end) throws IOException {
        if (csq instanceof String) {
            target.write((String)csq, start, end-start);
        }
        else if (csq instanceof StringBuffer) {
            char[] buf=new char[end-start];
            ((StringBuffer)csq).getChars(start, end, buf, 0); 
            target.write(buf);
        }
        else if (csq instanceof StringBuilder) {
            char[] buf=new char[end-start];
            ((StringBuilder)csq).getChars(start, end, buf, 0);
            target.write(buf);
        } else {
            if (start==0 && end==csq.length()) {
                String str=csq.toString();
                target.write(str, start, end-start);
            } else {
                String str=csq.subSequence(start, end).toString();
                target.write(str, 0, str.length());
            }
        }
    }

    @Override
    public void append(Encoder encoder, char character) throws IOException {
        target.write((int)character);
    }
}
