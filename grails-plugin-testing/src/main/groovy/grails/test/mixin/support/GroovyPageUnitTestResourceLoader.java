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
package grails.test.mixin.support;

import grails.core.GrailsApplication;
import grails.core.support.GrailsApplicationAware;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import grails.util.BuildSettings;
import org.grails.io.support.GrailsResourceUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * A {@link org.springframework.core.io.ResourceLoader} implementation
 * that loads GSP views relative to the project base directory for unit tests.
 *
 * @since 2.0
 * @author Graeme Rocher
 */
public class GroovyPageUnitTestResourceLoader extends DefaultResourceLoader implements GrailsApplicationAware, InitializingBean {

    public static final String WEB_INF_PREFIX = "/WEB-INF/grails-app/views";
    private Map<String,String> groovyPages = new ConcurrentHashMap<String, String>();
    private String basePath;
    private GrailsApplication grailsApplication;

    public GroovyPageUnitTestResourceLoader(Map<String, String> groovyPages) {
        this.groovyPages = groovyPages;
    }

    @Override
    public Resource getResource(String location) {

        if (location.startsWith(WEB_INF_PREFIX)) {
            location = location.substring(WEB_INF_PREFIX.length());
        }
        if (groovyPages.containsKey(location)) {
            try {
                return new ByteArrayResource(groovyPages.get(location).getBytes("UTF-8"), location);
            } catch (UnsupportedEncodingException e) {
                // continue
            }
        }
        
        if(basePath == null) {
            String basedir = BuildSettings.BASE_DIR.getAbsolutePath();
            basePath = basedir + File.separatorChar + GrailsResourceUtils.VIEWS_DIR_PATH;
        }

        String path = basePath + location;
        path = makeCanonical(path);
        return new FileSystemResource(path);
    }

    private String makeCanonical(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(grailsApplication != null) {
            Map config = grailsApplication.getFlatConfig();
            Object viewDir = config.get("grails.gsp.view.dir");
            if(viewDir != null) {
                basePath = viewDir.toString();
            }
        }
    }
}
