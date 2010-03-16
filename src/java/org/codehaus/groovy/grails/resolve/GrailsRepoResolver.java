/* Copyright 2004-2005 the original author or authors.
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

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.xml.sax.SAXException;

/**
 * Overrides the default Ivy resolver to substitute the release tag in Grails'
 * repository format prior to a resolve
 *
 * @author Graeme Rocher
 * @since 1.3
 */
public class GrailsRepoResolver extends URLResolver{

    protected URL repositoryRoot;

    public GrailsRepoResolver(String name, URL repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
        setName(name);
    }

    public URL getRepositoryRoot() {
        return repositoryRoot;
    }

    @Override
    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        pattern = transformGrailsRepositoryPattern(mrid, pattern);
        return super.findResourceUsingPattern(mrid, pattern, artifact, rmdparser, date);    
    }

    public String transformGrailsRepositoryPattern(ModuleRevisionId mrid, String pattern) {
        final String revision = mrid.getRevision();
        String versionTag;
        if(revision.equals("latest.integration") || revision.equals("latest")) {
            versionTag = "LATEST_RELEASE";
        }
        else {
            versionTag = "RELEASE_" + revision.replace('.', '_');
        }
        return pattern.replace("RELEASE_*", versionTag);

    }

    /**
     * Obtains the XML representation of the plugin-list.xml held in a Grails compatible repository
     * @param localFile The local file to save to XML too
     * @return The GPathResult reperesenting the XML
     */
    public GPathResult getPluginList(File localFile) {

        try {
            final Repository repo = getRepository();
            List list = repo.list(repositoryRoot.toString());
            for (Object entry : list) {
                String url = entry.toString();
                if(url.contains(".plugin-meta")) {
                    List metaList = repo.list(url);
                    for (Object current : metaList) {
                        url = current.toString();
                        if(url.contains("plugins-list.xml")) {
                            repo.get(url, localFile);
                            return new XmlSlurper().parse(localFile);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            return null;
        }
        catch (SAXException e) {
            return null;
        }
        catch (ParserConfigurationException e) {
            return null;
        }
        return null;
    }
}
