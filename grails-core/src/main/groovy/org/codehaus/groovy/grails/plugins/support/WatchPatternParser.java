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
package org.codehaus.groovy.grails.plugins.support;

import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Grails plugin's watchedResources property value into a list of
 *
 * @since 1.4
 * @author Graeme Rocher
 */
public class WatchPatternParser {

    public static final String WILD_CARD = "*";

    public List<WatchPattern> getWatchPatterns(List<String> patterns) {
       List<WatchPattern> watchPatterns = new ArrayList<WatchPattern>();

        for (String pattern : patterns) {
            WatchPattern watchPattern = new WatchPattern();
            watchPattern.setPattern(pattern);
            if (pattern.startsWith("file:")) {
                pattern = pattern.substring(5);
            }

            if (pattern.contains(WILD_CARD)) {
                watchPattern.setDirectory(new File(pattern.substring(0, pattern.indexOf(WILD_CARD))));
                setExtension(pattern, watchPattern);
                watchPatterns.add(watchPattern);
            }
            else {
                setExtension(pattern, watchPattern);

                watchPattern.setFile(new File(pattern));
                watchPatterns.add(watchPattern);
            }
        }

       return watchPatterns;
    }

    private void setExtension(String pattern, WatchPattern watchPattern) {
        String ext = StringUtils.getFilenameExtension(pattern);
        if (ext != null) {
            watchPattern.setExtension(ext);
        }
    }
}
