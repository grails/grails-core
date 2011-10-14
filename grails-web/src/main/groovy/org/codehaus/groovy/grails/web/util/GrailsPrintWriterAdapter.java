package org.codehaus.groovy.grails.web.util;

import groovy.lang.Writable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.io.output.NullWriter;

public class GrailsPrintWriterAdapter extends PrintWriter implements GrailsWrappedWriter {
    protected GrailsPrintWriter target;
    
    public GrailsPrintWriterAdapter(Writer wrapped) {
        super(new NullWriter());
        if(!(target instanceof GrailsPrintWriter)) {
            this.target = new GrailsPrintWriter(wrapped);
        } else {
            this.target = ((GrailsPrintWriter)target);            
        }
    }
    
    public boolean isAllowUnwrappingOut() {
        return target.isAllowUnwrappingOut();
    }

    public GrailsPrintWriter getTarget() {
        return target;
    }
    
    public Writer getOut() {
        return target.getOut();
    }
    
    public Writer unwrap() {
        if(isAllowUnwrappingOut()) {
            return getOut();
        } else {
            return this;
        }
    }    

    public GrailsPrintWriter leftShift(Object value) throws IOException {
        return target.leftShift(value);
    }

    public GrailsPrintWriter plus(Object value) throws IOException {
        return target.plus(value);
    }

    public boolean checkError() {
        return target.checkError();
    }

    public void setError() {
        target.setError();
    }

    public void flush() {
        target.flush();
    }

    public void print(Object obj) {
        target.print(obj);
    }

    public void print(String s) {
        target.print(s);
    }

    public void write(String s) {
        target.write(s);
    }

    public void write(int c) {
        target.write(c);
    }

    public void write(char[] buf, int off, int len) {
        target.write(buf, off, len);
    }

    public void write(String s, int off, int len) {
        target.write(s, off, len);
    }

    public void write(char[] buf) {
        target.write(buf);
    }

    public void print(boolean b) {
        target.print(b);
    }

    public void print(char c) {
        target.print(c);
    }

    public void print(int i) {
        target.print(i);
    }

    public void print(long l) {
        target.print(l);
    }

    public void print(float f) {
        target.print(f);
    }

    public void print(double d) {
        target.print(d);
    }

    public void print(char[] s) {
        target.print(s);
    }

    public void println() {
        target.println();
    }

    public void println(boolean b) {
        target.println(b);
    }

    public void println(char c) {
        target.println(c);
    }

    public void println(int i) {
        target.println(i);
    }

    public void println(long l) {
        target.println(l);
    }

    public void println(float f) {
        target.println(f);
    }

    public void println(double d) {
        target.println(d);
    }

    public void println(char[] c) {
        target.println(c);
    }

    public void println(String s) {
        target.println(s);
    }

    public void println(Object o) {
        target.println(o);
    }

    public PrintWriter append(char c) {
        target.append(c);
        return this;
    }

    public PrintWriter append(CharSequence csq, int start, int end) {
        target.append(csq, start, end);
        return this;
    }

    public PrintWriter append(CharSequence csq) {
        target.append(csq);
        return this;
    }

    public PrintWriter append(Object obj) {
        target.append(obj);
        return this;
    }

    public void write(StreamCharBuffer otherBuffer) {
        target.write(otherBuffer);
    }

    public void print(StreamCharBuffer otherBuffer) {
        target.print(otherBuffer);
    }

    public void append(StreamCharBuffer otherBuffer) {
        target.append(otherBuffer);
    }

    public void println(StreamCharBuffer otherBuffer) {
        target.println(otherBuffer);
    }

    public GrailsPrintWriter leftShift(StreamCharBuffer otherBuffer) {
        return target.leftShift(otherBuffer);
    }

    public void write(Writable writable) {
        target.write(writable);
    }

    public void print(Writable writable) {
        target.print(writable);
    }

    public GrailsPrintWriter leftShift(Writable writable) {
        return target.leftShift(writable);
    }

    public boolean isUsed() {
        return target.isUsed();
    }

    public void setUsed(boolean newUsed) {
        target.setUsed(newUsed);
    }

    public boolean resetUsed() {
        return target.resetUsed();
    }

    public void close() {
        target.close();
    }

    public void markUsed() {
        target.markUsed();
    }
    
    protected boolean isTrouble() {
        return target.isTrouble();
    }
    
    protected void handleIOException(IOException e) {
        target.handleIOException(e);
    }
}
