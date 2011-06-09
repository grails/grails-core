/* Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.config;

import grails.build.logging.GrailsConsole;
import groovy.lang.Closure;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
abstract class AbstractDependenciesConfigurer extends AbstractDependencyManagementConfigurer {

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("([a-zA-Z0-9\\-/\\._+=]*?):([a-zA-Z0-9\\-/\\._+=]+?):([a-zA-Z0-9\\-/\\._+=]+)");

    public AbstractDependenciesConfigurer(DependencyConfigurationContext context) {
        super(context);
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        if (args == null) {
            GrailsConsole.getInstance().error("WARNING: Configurational method [" + name + "] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..");
            return null;
        }

        List<Object> argsList = Arrays.asList((Object[])args);
        if (argsList.size() == 0) {
            GrailsConsole.getInstance().error("WARNING: Configurational method [" + name + "] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..");
            return null;
        }

        if (isOnlyStrings(argsList)) {
            addDependencyStrings(name, argsList, null, null);

        } else if (isProperties(argsList)) {
            addDependencyMaps(name, argsList, null);

        } else if (isStringsAndConfigurer(argsList)) {
            addDependencyStrings(name, argsList.subList(0, argsList.size() - 1), null, (Closure<?>)argsList.get(argsList.size() - 1));

        } else if (isPropertiesAndConfigurer(argsList)) {
            addDependencyMaps(name, argsList.subList(0, argsList.size() - 1), (Closure<?>)argsList.get(argsList.size() - 1));
        } else if (isStringsAndProperties(argsList)) {
            addDependencyStrings(name, argsList.subList(0, argsList.size() - 1), (Map<Object, Object>)argsList.get(argsList.size() - 1), null);

        } else {
            GrailsConsole.getInstance().error("WARNING: Configurational method [" + name + "] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring..");
        }

        return null;
    }

    private boolean isOnlyStrings(List<Object> args) {
        for (Object arg : args) {
            if (!(arg instanceof CharSequence)) {
                return false;
            }
        }
        return true;
    }

    private boolean isStringsAndConfigurer(List<Object> args) {
        if (args.size() == 1) {
            return false;
        }
        return isOnlyStrings(args.subList(0, args.size() - 1)) && args.get(args.size() - 1) instanceof Closure;
    }

    private boolean isStringsAndProperties(List<Object> args) {
        if (args.size() == 1) {
            return false;
        }
        return isOnlyStrings(args.subList(0, args.size() - 1)) && args.get(args.size() - 1) instanceof Map;
    }

    private boolean isProperties(List<Object> args) {
        for (Object arg : args) {
            if (!(arg instanceof Map)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPropertiesAndConfigurer(List<Object> args) {
        if (args.size() == 1) {
            return false;
        }
        return isProperties(args.subList(0, args.size() - 1)) && args.get(args.size() - 1) instanceof Closure;
    }

    private Map<Object, Object> extractDependencyProperties(String scope, String dependency) {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(dependency);
        if (matcher.matches()) {
            Map<Object, Object> properties = new HashMap<Object, Object>(3);
            properties.put("name", matcher.group(2));
            properties.put("group", matcher.group(1));
            properties.put("version", matcher.group(3));
            return properties;
        }
        GrailsConsole.getInstance().error("WARNING: Specified dependency definition " + scope + "(" + dependency + ") is invalid! Skipping..");
        return null;
    }

    private void addDependencyStrings(String scope, List<Object> dependencies, Map<Object, Object> overrides, Closure<?> configurer) {
        for (Object dependency : dependencies) {
            Map<Object, Object> dependencyProperties = extractDependencyProperties(scope, dependency.toString());
            if (dependencyProperties == null) {
                continue;
            }

            if (overrides != null) {
                for (Map.Entry<Object, Object> override : overrides.entrySet()) {
                    dependencyProperties.put(override.getKey().toString(), override.getValue().toString());
                }
            }

            addDependency(scope, dependencyProperties, configurer);
        }
    }

    private void addDependencyMaps(String scope, List<Object> dependencies, Closure<?> configurer) {
        for (Object dependency : dependencies) {
            addDependency(scope, (Map<Object, Object>)dependency, configurer);
        }
    }

    private String nullSafeToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private void addDependency(String scope, Map<Object, Object> dependency, Closure<?> configurer) {
        preprocessDependencyProperties(dependency);

        String name = nullSafeToString(dependency.get("name"));
        String group = nullSafeToString(dependency.get("group"));
        String version = nullSafeToString(dependency.get("version"));
        String classifier = nullSafeToString(dependency.get("classifier"));
        String branch = nullSafeToString(dependency.get("branch"));

        boolean transitive = getBooleanValueOrDefault(dependency, "transitive", true);
        Boolean export = getExportSetting(dependency);

        boolean isExcluded = context.pluginName != null ?
                context.dependencyManager.isExcludedFromPlugin(context.pluginName, name) :
                context.dependencyManager.isExcluded(name);

        if (isExcluded) {
            return;
        }

        Map<String, String> attrs = null;
        if (classifier != null) {
            attrs = new HashMap<String, String>(1);
            attrs.put("m:classifier", classifier);
        }

        ModuleRevisionId mrid;
        if (branch != null) {
            mrid = ModuleRevisionId.newInstance(group, name, branch, version, attrs);
        } else {
            mrid = ModuleRevisionId.newInstance(group, name, version, attrs);
        }

        EnhancedDefaultDependencyDescriptor dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, transitive, scope);
        handleExport(dependencyDescriptor,export);

        boolean inherited = context.inherited || context.dependencyManager.getInheritsAll() || context.pluginName != null;
        dependencyDescriptor.setInherited(inherited);

        if (context.pluginName != null) {
            dependencyDescriptor.setPlugin(context.pluginName);
        }

        if (configurer != null) {
            dependencyDescriptor.configure(configurer);
        }

        addDependency(scope, dependencyDescriptor);
    }

    protected Boolean getExportSetting(Map<Object, Object> dependency) {
        return dependency.containsKey("export") ? Boolean.valueOf(dependency.get("export").toString()) : null;
    }

    private boolean getBooleanValueOrDefault(Map<Object, Object> properties, String propertyName, boolean defaultValue) {
        return properties.containsKey(propertyName) ? Boolean.valueOf(properties.get(propertyName).toString()) : defaultValue;
    }

    protected void preprocessDependencyProperties(@SuppressWarnings("unused") Map<Object, Object> dependency) {
        // used in plugin subclass to populate default group id
    }

    abstract protected void addDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor);

    abstract protected void handleExport(EnhancedDefaultDependencyDescriptor descriptor, Boolean export);
}
