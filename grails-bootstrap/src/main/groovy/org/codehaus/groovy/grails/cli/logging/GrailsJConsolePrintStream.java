package org.codehaus.groovy.grails.cli.logging;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 03/06/2011
 * Time: 09:05
 * To change this template use File | Settings | File Templates.
 */
class GrailsJConsolePrintStream extends PrintStream {


    GrailsJConsolePrintStream(OutputStream out) {
        super(out);
    }

    public OutputStream getTargetOut() {
        return out;
    }

    @Override
    public void print(Object o) {
        if(o != null)
            GrailsConsole.getInstance().updateStatus(o.toString());
    }

    @Override
    public void print(String s) {
        GrailsConsole.getInstance().updateStatus(s);
    }

    @Override
    public void println(String s) {
        GrailsConsole.getInstance().log(s);
    }

    @Override
    public void println(Object o) {
        if(o != null)
            GrailsConsole.getInstance().updateStatus(o.toString());
    }


}
