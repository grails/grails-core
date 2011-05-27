/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * Default implementating of the UrlMappingData interface.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class DefaultUrlMappingData implements UrlMappingData {

    private static final String CAPTURED_WILDCARD = "(*)";
    private static final String CAPTURED_DOUBLE_WILDCARD = "(**)";
    private static final String QUESTION_MARK = "?";
    private static final String SLASH = "/";

    private final String urlPattern;
    private final String[] logicalUrls;
    private String[] tokens;

    private List<Boolean> optionalTokens = new ArrayList<Boolean>();

    public DefaultUrlMappingData(String urlPattern) {
        Assert.hasLength(urlPattern, "Argument [urlPattern] cannot be null or blank");
        Assert.isTrue(urlPattern.startsWith(SLASH), "Argument [urlPattern] is not a valid URL. It must start with '/' !");

        this.urlPattern = StringUtils.replace(urlPattern, "(*)**", CAPTURED_DOUBLE_WILDCARD); // remove starting /
        tokens = this.urlPattern.substring(1).split(SLASH);
        List<String> urls = new ArrayList<String>();
        parseUrls(urls);

        logicalUrls = urls.toArray(new String[urls.size()]);
    }

    private void parseUrls(List<String> urls) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();

            if (token.equals(SLASH)) continue;

            boolean isOptional = false;
            if (token.endsWith(QUESTION_MARK)) {
                urls.add(buf.toString());
                tokens[i] = token.substring(0, token.length()-1);
                buf.append(SLASH).append(tokens[i]);
                isOptional = true;
            }
            else {
                buf.append(SLASH).append(token);
            }
            if (CAPTURED_WILDCARD.equals(tokens[i])) {
                if (isOptional) {
                    optionalTokens.add(Boolean.TRUE);
                }
                else {
                    optionalTokens.add(Boolean.FALSE);
                }
            }
            if (CAPTURED_DOUBLE_WILDCARD.equals(tokens[i])) {
                optionalTokens.add(Boolean.TRUE);
            }
        }
        urls.add(buf.toString());
        Collections.reverse(urls);
    }

    public String[] getTokens() {
        return tokens;
    }

    public String[] getLogicalUrls() {
        return logicalUrls;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public boolean isOptional(int index) {
        if (index >= optionalTokens.size()) return true;
        return optionalTokens.get(index).equals(Boolean.TRUE);
    }
}
