/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.logging;

import grails.build.logging.GrailsConsole;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Used to replace default System.out with one that routes calls through GrailsConsole
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsConsolePrintStream extends PrintStream {


    public GrailsConsolePrintStream(OutputStream out) {
        super(out);
    }

    public OutputStream getTargetOut() {
        return out;
    }

    @Override
    public void print(Object o) {
        if(o != null)
            GrailsConsole.getInstance().log(o.toString());
    }

    @Override
    public void print(String s) {
        GrailsConsole.getInstance().log(s);
    }

    @Override
    public void println(String s) {
        GrailsConsole.getInstance().log(s);
    }

    @Override
    public void println(Object o) {
        if(o != null)
            GrailsConsole.getInstance().log(o.toString());
    }


}
