/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.resolve.maven;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorBuilder;
import org.apache.ivy.plugins.parser.m2.PomReader;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;

/**
 * A POM module descriptor builder that is aware of Grails packaging types
 *
 * @author Graeme Rocher
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GrailsPackagingAwarePomModuleDescriptorBuilder extends PomModuleDescriptorBuilder {
    private static final String DEPENDENCY_MANAGEMENT = "m:dependency.management";
    private static final String EXTRA_INFO_DELIMITER = "__";
    private static final String WRONG_NUMBER_OF_PARTS_MSG = "what seemed to be a dependency "
            + "management extra info exclusion had the wrong number of parts (should have 2) ";

    private static final Collection/*<String>*/ JAR_PACKAGINGS = Arrays.asList(
            new String[] {"ejb", "bundle", "maven-plugin"});

    static final Map MAVEN2_CONF_MAPPING = new HashMap();

    static {
        try {
            Field field = PomModuleDescriptorBuilder.class.getDeclaredField("MAVEN2_CONF_MAPPING");
            field.setAccessible(true);
            Map m = (Map) field.get(PomModuleDescriptorBuilder.class);
            MAVEN2_CONF_MAPPING.putAll(m);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot obtain reference to ivy module descriptor to configure POM data correctly. Message: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot obtain reference to ivy module descriptor to configure POM data correctly. Message: " + e.getMessage());
        }
    }

    public static final String GRAILS_PLUGIN_PACKAGING = "grails-plugin";
    public static final String GRAILS_BINARY_PLUGIN_PACKAGING = "grails-binary-plugin";
    public static final String GRAILS_APP_PACKAGING = "grails-app";

    public GrailsPackagingAwarePomModuleDescriptorBuilder(ModuleDescriptorParser parser, Resource res, ParserSettings ivySettings) {
        super(parser, res, ivySettings);
    }

    @Override
    public void addMainArtifact(String artifactId, String packaging) {
        super.addMainArtifact(artifactId, getPackagingForGrailsType(packaging));
    }

    private String getPackagingForGrailsType(String packaging) {
        if (GRAILS_PLUGIN_PACKAGING.equals(packaging)) {
            packaging = "zip";
        }
        else if (GRAILS_BINARY_PLUGIN_PACKAGING.equals(packaging)) {
            packaging = "jar";
        } else if (GRAILS_APP_PACKAGING.equals(packaging)) {
            packaging = "war";
        }
        return packaging;
    }

    @Override
    public void addDependency(Resource res, PomReader.PomDependencyData dep) {
        String scope = dep.getScope();
        if ((scope != null) && (scope.length() > 0) && !MAVEN2_CONF_MAPPING.containsKey(scope)) {
            // unknown scope, defaulting to 'compile'
            scope = "compile";
        }

        String version = dep.getVersion();
        version = (version == null || version.length() == 0) ? getDefaultVersion(dep) : version;
        ModuleRevisionId moduleRevId = ModuleRevisionId.newInstance(dep.getGroupId(), dep
                .getArtifactId(), version);

        if (moduleRevId.getName().equals("grails-dependencies")) return;

        // Some POMs depend on theirselfves, don't add this dependency: Ivy doesn't allow this!
        // Example: http://repo2.maven.org/maven2/net/jini/jsk-platform/2.1/jsk-platform-2.1.pom
        DefaultModuleDescriptor ivyModuleDescriptor = getIvyModuleDescriptor();
        ModuleRevisionId mRevId = ivyModuleDescriptor.getModuleRevisionId();
        if ((mRevId != null) && mRevId.getModuleId().equals(moduleRevId.getModuleId())) {
            return;
        }

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(ivyModuleDescriptor,
                moduleRevId, true, false, true);
        scope = (scope == null || scope.length() == 0) ? getDefaultScope(dep) : scope;
        Object mapping = MAVEN2_CONF_MAPPING.get(scope);
        MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(mapping.getClass());
        metaClass.invokeMethod(mapping, "addMappingConfs", new Object[] { dd, dep.isOptional()} );

        Map extraAtt = new HashMap();
        if ((dep.getClassifier() != null) || ((dep.getType() != null) && !"jar".equals(dep.getType()))) {
            String type = "jar";
            if (dep.getType() != null) {
                type = dep.getType();
            }
            type = getPackagingForGrailsType(type);
            String ext = type;

            // if type is 'test-jar', the extension is 'jar' and the classifier is 'tests'
            // Cfr. http://maven.apache.org/guides/mini/guide-attached-tests.html
            if ("test-jar".equals(type)) {
                ext = "jar";
                extraAtt.put("m:classifier", "tests");
            } else if (JAR_PACKAGINGS.contains(type)) {
                ext = "jar";
            }

            ext = getPackagingForGrailsType(type);

            // we deal with classifiers by setting an extra attribute and forcing the
            // dependency to assume such an artifact is published
            if (dep.getClassifier() != null) {
                extraAtt.put("m:classifier", dep.getClassifier());
            }
            DefaultDependencyArtifactDescriptor depArtifact =
                    new DefaultDependencyArtifactDescriptor(dd, dd.getDependencyId().getName(),
                            type, ext, null, extraAtt);
            // here we have to assume a type and ext for the artifact, so this is a limitation
            // compared to how m2 behave with classifiers
            String optionalizedScope = dep.isOptional() ? "optional" : scope;
            dd.addDependencyArtifact(optionalizedScope, depArtifact);
        }

        // experimentation shows the following, excluded modules are
        // inherited from parent POMs if either of the following is true:
        // the <exclusions> element is missing or the <exclusions> element
        // is present, but empty.
        List /*<ModuleId>*/ excluded = dep.getExcludedModules();
        if (excluded.isEmpty()) {
            excluded = getDependencyMgtExclusions(ivyModuleDescriptor, dep.getGroupId(), dep.getArtifactId());
        }
        for (Iterator itExcl = excluded.iterator(); itExcl.hasNext();) {
            ModuleId excludedModule = (ModuleId) itExcl.next();
            String[] confs = dd.getModuleConfigurations();
            for (int k = 0; k < confs.length; k++) {
                dd.addExcludeRule(confs[k], new DefaultExcludeRule(new ArtifactId(
                        excludedModule, PatternMatcher.ANY_EXPRESSION,
                        PatternMatcher.ANY_EXPRESSION,
                        PatternMatcher.ANY_EXPRESSION),
                        ExactPatternMatcher.INSTANCE, null));
            }
        }

        ivyModuleDescriptor.addDependency(dd);
    }

    private DefaultModuleDescriptor getIvyModuleDescriptor() {

        try {
            Field field = PomModuleDescriptorBuilder.class.getDeclaredField("ivyModuleDescriptor");
            field.setAccessible(true);
            return (DefaultModuleDescriptor) field.get(this);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot obtain reference to ivy module descriptor to configure POM data correctly. Message: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot obtain reference to ivy module descriptor to configure POM data correctly. Message: " + e.getMessage());
        }
    }

    private static List /*<ModuleId>*/ getDependencyMgtExclusions(
            ModuleDescriptor descriptor,
            String groupId,
            String artifactId) {
        String exclusionPrefix = getDependencyMgtExtraInfoPrefixForExclusion(
                groupId, artifactId);
        List /*<ModuleId>*/ exclusionIds = new LinkedList /*<ModuleId>*/ ();
        Map /*<String,String>*/ extras = descriptor.getExtraInfo();
        for (final Iterator entIter = extras.entrySet().iterator(); entIter.hasNext();) {
            Map.Entry /*<String,String>*/ ent = (Map.Entry) entIter.next();
            String key = (String) ent.getKey();
            if (key.startsWith(exclusionPrefix)) {
                String fullExclusion = (String) ent.getValue();
                String[] exclusionParts = fullExclusion.split(EXTRA_INFO_DELIMITER);
                if (exclusionParts.length != 2) {
                    Message.error(WRONG_NUMBER_OF_PARTS_MSG + exclusionParts.length + " : "
                            + fullExclusion);
                    continue;
                }
                exclusionIds.add(ModuleId.newInstance(exclusionParts[0], exclusionParts[1]));
            }
        }

        return exclusionIds;
    }

    private static String getDependencyMgtExtraInfoPrefixForExclusion(
            String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId + EXTRA_INFO_DELIMITER + "exclusion_";
    }

    private static String getDependencyMgtExtraInfoKeyForScope(String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId + EXTRA_INFO_DELIMITER + "scope";
    }

    private static String getDependencyMgtExtraInfoKeyForVersion(
            String groupId, String artifaceId) {
        return DEPENDENCY_MANAGEMENT + EXTRA_INFO_DELIMITER + groupId
                + EXTRA_INFO_DELIMITER + artifaceId + EXTRA_INFO_DELIMITER + "version";
    }

    private String getDefaultScope(PomReader.PomDependencyData dep) {
        String key = getDependencyMgtExtraInfoKeyForScope(dep.getGroupId(), dep.getArtifactId());
        String result = (String) getIvyModuleDescriptor().getExtraInfo().get(key);
        if ((result == null) || !MAVEN2_CONF_MAPPING.containsKey(result)) {
            result = "compile";
        }
        return result;
    }

    private String getDefaultVersion(PomReader.PomDependencyData dep) {
        String key = getDependencyMgtExtraInfoKeyForVersion(dep.getGroupId(), dep.getArtifactId());
        return (String) getIvyModuleDescriptor().getExtraInfo().get(key);
    }
}
