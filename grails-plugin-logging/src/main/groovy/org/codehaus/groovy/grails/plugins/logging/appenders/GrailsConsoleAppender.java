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

package org.codehaus.groovy.grails.plugins.logging.appenders;

import grails.build.logging.GrailsConsole;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Log4j appender that appends to the GrailsConsole instance. Not for use in production/WAR deployed scenarios!
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsConsoleAppender extends AppenderSkeleton {
    GrailsConsole console = GrailsConsole.getInstance();

    @Override
    protected void append(LoggingEvent event) {
        Level level = event.getLevel();
        String message = buildMessage(event);
        if((level == Level.ERROR)||(level == Level.FATAL)) {
            console.error(message);
        }
        else {
            console.log(message);
        }
    }

    private String buildMessage(LoggingEvent event) {
        StringBuilder b = new StringBuilder(this.layout.format(event));

        String[] throwableStrRep = event.getThrowableStrRep();
        if(throwableStrRep != null) {
            b.append(Layout.LINE_SEP);
            for (String line : throwableStrRep) {
                b.append(line).append(Layout.LINE_SEP);
            }
        }
        return b.toString();
    }

    public void close() {
        // do nothing
    }

    public boolean requiresLayout() {
        return true;
    }
}
