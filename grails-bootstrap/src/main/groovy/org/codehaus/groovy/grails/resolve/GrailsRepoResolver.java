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
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.plugins.resolver.util.ResourceMDParser;
import org.apache.ivy.util.url.IvyAuthenticator;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overrides the default Ivy resolver to substitute the release tag in Grails'
 * repository format prior to a resolve.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
public class GrailsRepoResolver extends URLResolver{

    protected URL repositoryRoot;

    private Map<File,GPathResult> parsedXmlCache = new ConcurrentHashMap<File,GPathResult>();

    public GrailsRepoResolver(String name, URL repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
        setName(name);
    }

    public URL getRepositoryRoot() {
        return repositoryRoot;
    }

    @Override
    protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date) {
        installIvyAuth();
        pattern = transformGrailsRepositoryPattern(mrid, pattern);
        return super.findResourceUsingPattern(mrid, pattern, artifact, rmdparser, date);
    }

    private void installIvyAuth() {
        IvyAuthenticator.install();
    }

    public String transformGrailsRepositoryPattern(ModuleRevisionId mrid, String pattern) {
        final String revision = mrid.getRevision();
        String versionTag;
        if (revision.equals("latest.integration") || revision.equals("latest")) {
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
    @SuppressWarnings("rawtypes")
    public GPathResult getPluginList(File localFile) {
        GPathResult parsedXml = parsedXmlCache.get(localFile);
        if (parsedXml == null) {
            installIvyAuth();
            try {
                final Repository repo = getRepository();
                List list = repo.list(repositoryRoot.toString());
                for (Object entry : list) {
                    String url = entry.toString();
                    if (url.contains(".plugin-meta")) {
                        List metaList = repo.list(url);
                        for (Object current : metaList) {
                            url = current.toString();
                            if (url.contains("plugins-list.xml")) {
                                Resource remoteFile = repo.getResource(url);
                                if (localFile.lastModified() < remoteFile.getLastModified()) {
                                    repo.get(url, localFile);
                                }
                                parsedXml = new XmlSlurper().parse(localFile);
                                parsedXmlCache.put(localFile, parsedXml);
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
        }
        return parsedXml;
    }
}
