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
package org.codehaus.groovy.grails.resolve;

import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository cable of handling the Grails svn repo's repository patterns.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsRepository extends URLRepository {

    private static final String RELEASE_TOKEN = "/RELEASE_";
    private static final String END_TOKEN = "/";

    @Override
    public Resource getResource(String source) throws IOException {
        return super.getResource(convertSource(source, '.', '_'));
    }

    @Override
    public void get(String source, File destination) throws IOException {
        super.get(convertSource(source, '.', '_'), destination);
    }

    @Override
    public void put(File source, String destination, boolean overwrite) throws IOException {
        super.put(source, convertSource(destination, '.', '_'), overwrite);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List list(String parent) throws IOException {
        List lst = super.list(convertSource(parent, '.', '_'));
        if (lst == null) {
            return null;
        }

        List<String> newLst = new ArrayList<String>(lst.size());

        for (Object i : lst) {
            newLst.add(convertSource((String)i, '_', '.'));
        }

        return newLst;
    }

    private String convertSource(String source, char from, char to) {
        StringBuilder path = new StringBuilder(source);

        int startIndex = path.indexOf(RELEASE_TOKEN);
        if (startIndex == -1) {
           return source;
        }

        startIndex += RELEASE_TOKEN.length();
        int endIndex = path.indexOf(END_TOKEN, startIndex);

        String versionString = path.substring(startIndex, endIndex).replace(from, to);
        path.replace(startIndex, endIndex, versionString);

        return path.toString();
    }
}
