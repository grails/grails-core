/*
 * Copyright 2011 the original author or authors.
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
package org.grails.web.pages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds GSP Sitemesh integration directly to compiled GSP.
 *
 * head, meta, title, body and content tags are replaced with <sitemesh:capture*>...</sitemesh:capture*> taglibs
 *
 * The taglib is used to capture the content of each tag. This prevents the need to parse the content output like Sitemesh normally does.
 *
 * @author <a href="mailto:lari.hotari@sagire.fi">Lari Hotari, Sagire Software Oy</a>
 */
public class SitemeshPreprocessor {

    Pattern parameterPattern = Pattern.compile("<parameter(\\s+name[^>]+?)(/*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern metaPattern = Pattern.compile("<meta(\\s[^>]+?)(/*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern titlePattern = Pattern.compile("<title(\\s[^>]*)?>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern headPattern = Pattern.compile("<head(\\s[^>]*)?>(.*?)</head>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern bodyPattern = Pattern.compile("<body(\\s[^>]*)?>(.*?)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern contentPattern = Pattern.compile("<content(\\s+tag[^>]+)>(.*?)</content>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final String XML_CLOSING_FOR_EMPTY_TAG_ATTRIBUTE_NAME = "gsp_sm_xmlClosingForEmptyTag";

    public String addGspSitemeshCapturing(String gspSource) {
        StringBuffer sb = addHeadCapturing(gspSource);
        sb = addBodyCapturing(sb);
        sb = addContentCapturing(sb);
        return sb.toString();
    }

    StringBuffer addHeadCapturing(String gspSource) {
        StringBuffer sb = new StringBuffer((int)(gspSource.length() * 1.2));
        Matcher m = headPattern.matcher(gspSource);
        if (m.find()) {
            m.appendReplacement(sb, "");
            sb.append("<sitemesh:captureHead");
            if (m.group(1) != null) {
                sb.append(m.group(1));
            }
            sb.append(">");
            sb.append(addMetaCapturing(addTitleCapturing(m.group(2))));
            sb.append("</sitemesh:captureHead>");
            m.appendTail(sb);
        }
        else if (!bodyPattern.matcher(gspSource).find()) {
            // no body either, so replace meta & title in the entire gsp source
            // fix title in sub-template -problem
            sb.append(addMetaCapturing(addTitleCapturing(gspSource)));
        }
        else {
            sb.append(gspSource);
        }
        return sb;
    }

    String addMetaCapturing(String headContent) {
        Matcher m = metaPattern.matcher(headContent);
        final String result = parameterPattern.matcher(
                m.replaceAll("<sitemesh:captureMeta " + XML_CLOSING_FOR_EMPTY_TAG_ATTRIBUTE_NAME + "=\"$2\"$1/>")
        ).replaceAll("<sitemesh:parameter$1/>");
        return result;
    }

    String addTitleCapturing(String headContent) {
        Matcher m = titlePattern.matcher(headContent);
        return m.replaceAll("<sitemesh:wrapTitleTag><sitemesh:captureTitle$1>$2</sitemesh:captureTitle></sitemesh:wrapTitleTag>");
    }

    StringBuffer addBodyCapturing(StringBuffer sb) {
        Matcher m = bodyPattern.matcher(sb);
        return new StringBuffer(m.replaceAll("<sitemesh:captureBody$1>$2</sitemesh:captureBody>"));
    }

    StringBuffer addContentCapturing(StringBuffer sb) {
        Matcher m = contentPattern.matcher(sb);
        return new StringBuffer(m.replaceAll("<sitemesh:captureContent$1>$2</sitemesh:captureContent>"));
    }
}
