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
package org.codehaus.groovy.grails.web.pages;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * GSP tag information holder
 *
 * @author Ivo Houbrechts
 */
public class GspTagInfo {
    private static final String GRAILS_APP_TAG_LIB = "grails-app" + File.separatorChar + "taglib" + File.separatorChar;
    private static final String GSP_TAG_LIB = "GspTagLib";

    private String text;
    private String tagName;
    private String tagLibName;
    private String packageName;
    private String filePath;
    private String gspEncoding = "UTF-8";

    public GspTagInfo(String tagName, String packageName, String text) {
        this.tagName = tagName;
        this.packageName = packageName;
        tagLibName = "_" + Character.toUpperCase(tagName.charAt(0)) + tagName.substring(1) + GSP_TAG_LIB;
        filePath = packageName.replace('.', File.separatorChar) + File.separatorChar + getTagLibFileName();
        this.text = text;
    }

    public GspTagInfo(File file) {
        try {
            String path = file.getCanonicalPath();
            if (path.contains(GRAILS_APP_TAG_LIB)) {
                path = path.substring(path.indexOf(GRAILS_APP_TAG_LIB) + GRAILS_APP_TAG_LIB.length());
            }
            if (path.lastIndexOf(".gsp") > -1) {
                path = path.substring(0, path.lastIndexOf(".gsp"));
            }
            int lastDot = path.lastIndexOf(File.separatorChar);
            if (lastDot > -1) {
                packageName = path.substring(0, lastDot).replace(File.separatorChar, '.');
                tagName = path.substring(lastDot + 1);
            } else {
                packageName = "";
                tagName = path;
            }
            tagLibName = "_" + Character.toUpperCase(tagName.charAt(0)) + tagName.substring(1) + GSP_TAG_LIB;
            text = read(file);
            filePath = path + File.separatorChar + getTagLibFileName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTagLibFQCN() {
        return packageName + '.' + tagLibName;
    }

    public String getTagLibFileName() {
        return getTagLibName() + ".groovy";
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTagLibName() {
        return tagLibName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getText() {
        return text;
    }

    private String read(File file) {
        try {
            return IOUtils.toString(new FileInputStream(file), gspEncoding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getGspEncoding() {
        return gspEncoding;
    }

    public void setGspEncoding(String gspEncoding) {
        this.gspEncoding = gspEncoding;
    }
}
