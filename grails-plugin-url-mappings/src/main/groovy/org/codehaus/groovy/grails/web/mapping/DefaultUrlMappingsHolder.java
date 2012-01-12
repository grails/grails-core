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

import grails.util.CollectionUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.style.ToStringCreator;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weigher;

/**
 * Default implementation of the UrlMappingsHolder interface that takes a list of mappings and
 * then sorts them according to their precedence rules as defined in the implementation of Comparable.
 *
 * @see org.codehaus.groovy.grails.web.mapping.UrlMapping
 * @see Comparable
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@SuppressWarnings("rawtypes")
public class DefaultUrlMappingsHolder implements UrlMappingsHolder {

    private static final transient Log LOG = LogFactory.getLog(DefaultUrlMappingsHolder.class);
    private static final int DEFAULT_MAX_WEIGHTED_CAPACITY = 5000;

    private int maxWeightedCacheCapacity = DEFAULT_MAX_WEIGHTED_CAPACITY;
    private Map<String, UrlMappingInfo> cachedMatches;
    private Map<String, List<UrlMappingInfo>> cachedListMatches;
    private enum CustomListWeigher implements Weigher<List<UrlMappingInfo>> {
        INSTANCE;
        public int weightOf(List<UrlMappingInfo> values) {
            return values.size() + 1;
        }
    }

    private List<UrlMapping> urlMappings = new ArrayList<UrlMapping>();
    private UrlMapping[] mappings;
    private List excludePatterns;
    private Map<UrlMappingKey, UrlMapping> mappingsLookup = new HashMap<UrlMappingKey, UrlMapping>();
    private Map<String, UrlMapping> namedMappings = new HashMap<String, UrlMapping>();
    private UrlMappingsList mappingsListLookup = new UrlMappingsList();
    private Set<String> DEFAULT_CONTROLLER_PARAMS = CollectionUtils.newSet(
          UrlMapping.CONTROLLER, UrlMapping.ACTION);
    private Set<String> DEFAULT_ACTION_PARAMS = CollectionUtils.newSet(UrlMapping.ACTION);
    private UrlCreatorCache urlCreatorCache;
    // capacity of the UrlCreatoreCache is the estimated number of char's stored in cached objects
    private int urlCreatorMaxWeightedCacheCapacity = 160000;

    public DefaultUrlMappingsHolder(List<UrlMapping> mappings) {
        this(mappings, null, false);
    }

    public DefaultUrlMappingsHolder(List<UrlMapping> mappings, List excludePatterns) {
        this(mappings, excludePatterns, false);
    }

    public DefaultUrlMappingsHolder(List<UrlMapping> mappings, List excludePatterns, boolean doNotCallInit) {
        urlMappings = mappings;
        this.excludePatterns = excludePatterns;
        if (!doNotCallInit) {
            initialize();
        }
    }

    public void initialize() {
        sortMappings();

        cachedMatches = new ConcurrentLinkedHashMap.Builder<String, UrlMappingInfo>()
            .maximumWeightedCapacity(maxWeightedCacheCapacity)
            .build();
        cachedListMatches = new ConcurrentLinkedHashMap.Builder<String, List<UrlMappingInfo>>()
            .maximumWeightedCapacity(maxWeightedCacheCapacity)
            .weigher(CustomListWeigher.INSTANCE)
            .build();
        if (urlCreatorMaxWeightedCacheCapacity > 0) {
            urlCreatorCache = new UrlCreatorCache(urlCreatorMaxWeightedCacheCapacity);
        }

        mappings = urlMappings.toArray(new UrlMapping[urlMappings.size()]);

        for (UrlMapping mapping : mappings) {
            String mappingName = mapping.getMappingName();
            if (mappingName != null) {
                namedMappings.put(mappingName, mapping);
            }
            String controllerName = mapping.getControllerName() instanceof String ? mapping.getControllerName().toString() : null;
            String actionName = mapping.getActionName() instanceof String ? mapping.getActionName().toString() : null;

            ConstrainedProperty[] params = mapping.getConstraints();
            Set<String> requiredParams = new HashSet<String>();
            int optionalIndex = -1;
            for (int j = 0; j < params.length; j++) {
                ConstrainedProperty param = params[j];
                if (!param.isNullable()) {
                    requiredParams.add(param.getPropertyName());
                }
                else {
                    optionalIndex = j;
                    break;
                }
            }
            UrlMappingKey key = new UrlMappingKey(controllerName, actionName, requiredParams);
            mappingsLookup.put(key, mapping);

            UrlMappingsListKey listKey = new UrlMappingsListKey(controllerName, actionName);
            mappingsListLookup.put(listKey, key);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Reverse mapping: " + key + " -> " + mapping);
            }
            Set<String> requiredParamsAndOptionals = new HashSet<String>(requiredParams);
            if (optionalIndex > -1) {
                for (int j = optionalIndex; j < params.length; j++) {
                    ConstrainedProperty param = params[j];
                    requiredParamsAndOptionals.add(param.getPropertyName());
                    key = new UrlMappingKey(controllerName, actionName, new HashSet<String>(requiredParamsAndOptionals));
                    mappingsLookup.put(key, mapping);

                    listKey = new UrlMappingsListKey(controllerName, actionName);
                    mappingsListLookup.put(listKey, key);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Reverse mapping: " + key + " -> " + mapping);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sortMappings() {
        List<ResponseCodeUrlMapping> responseCodeUrlMappings = new ArrayList<ResponseCodeUrlMapping>();
        Iterator<UrlMapping> iter = urlMappings.iterator();
        while (iter.hasNext()) {
            UrlMapping mapping = iter.next();
            if (mapping instanceof ResponseCodeUrlMapping) {
                responseCodeUrlMappings.add((ResponseCodeUrlMapping)mapping);
                iter.remove();
            }
        }

        Collections.sort(urlMappings);
        urlMappings.addAll(responseCodeUrlMappings);
        Collections.reverse(urlMappings);
    }

    public UrlMapping[] getUrlMappings() {
        return mappings;
    }

    public List getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * @see UrlMappingsHolder#getReverseMapping(String, String, java.util.Map)
     */
    public UrlCreator getReverseMapping(final String controller, final String action, Map params) {
        if (params == null) params = Collections.EMPTY_MAP;

        if (urlCreatorCache != null) {
            UrlCreatorCache.ReverseMappingKey key=urlCreatorCache.createKey(controller, action, params);
            UrlCreator creator=urlCreatorCache.lookup(key);
            if (creator==null) {
                creator=resolveUrlCreator(controller, action, params, true);
                creator=urlCreatorCache.putAndDecorate(key, creator);
            }
            // preserve previous side-effect, remove mappingName from params
            params.remove("mappingName");
            return creator;
        }
        // cache is disabled
        return resolveUrlCreator(controller, action, params, true);
    }

    public UrlCreator getReverseMappingNoDefault(String controller, String action, Map params) {
        if (params == null) params = Collections.EMPTY_MAP;

        if (urlCreatorCache != null) {
            UrlCreatorCache.ReverseMappingKey key=urlCreatorCache.createKey(controller, action, params);
            UrlCreator creator=urlCreatorCache.lookup(key);
            if (creator==null) {
                creator=resolveUrlCreator(controller, action, params, false);
                if(creator != null)
                    creator=urlCreatorCache.putAndDecorate(key, creator);
            }
            // preserve previous side-effect, remove mappingName from params
            params.remove("mappingName");
            return creator;
        }
        // cache is disabled
        return resolveUrlCreator(controller, action, params, true);
    }

    @SuppressWarnings("unchecked")
    private UrlCreator resolveUrlCreator(final String controller,
                                         final String action, Map params, boolean useDefault) {
        UrlMapping mapping = null;

        mapping = namedMappings.get(params.remove("mappingName"));
        if (mapping == null) {
            mapping = lookupMapping(controller, action, params);
        }
        if (mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            mapping = mappingsLookup.get(new UrlMappingKey(controller, action, Collections.EMPTY_SET));
        }
        if (mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            Set<String> lookupParams = new HashSet<String>(DEFAULT_ACTION_PARAMS);
            Set<String> paramKeys = new HashSet<String>(params.keySet());
            paramKeys.removeAll(lookupParams);
            lookupParams.addAll(paramKeys);
            mapping = mappingsLookup.get(new UrlMappingKey(controller, null, lookupParams));
            if (mapping == null) {
                lookupParams.removeAll(paramKeys);
                mapping = mappingsLookup.get(new UrlMappingKey(controller, null, lookupParams));
            }
        }
        if (mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            Set<String> lookupParams = new HashSet<String>(DEFAULT_CONTROLLER_PARAMS);
            Set<String> paramKeys = new HashSet<String>(params.keySet());
            paramKeys.removeAll(lookupParams);

            lookupParams.addAll(paramKeys);
            mapping = mappingsLookup.get(new UrlMappingKey(null, null, lookupParams));
            if (mapping == null) {
                lookupParams.removeAll(paramKeys);
                mapping = mappingsLookup.get(new UrlMappingKey(null, null, lookupParams));
            }
        }
        UrlCreator creator = null;
        if (mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            if(useDefault)
                creator=new DefaultUrlCreator(controller, action);
        } else {
            creator=mapping;
        }
        return creator;
    }

    /**
     * Performs a match uses reverse mappings to looks up a mapping from the
     * controller, action and params. This is refactored to use a list of mappings
     * identified by only controller and action and then matches the mapping to select
     * the mapping that best matches the params (most possible matches).
     *
     * @param controller The controller name
     * @param action The action name
     * @param params The params
     * @return A UrlMapping instance or null
     */
    @SuppressWarnings("unchecked")
    protected UrlMapping lookupMapping(String controller, String action, Map params) {
        final UrlMappingsListKey lookupKey = new UrlMappingsListKey(controller, action);
        SortedSet mappingKeysSet = mappingsListLookup.get(lookupKey);

        final String actionName = lookupKey.action;
        boolean secondAttempt = false;
        final boolean isIndexAction = GrailsControllerClass.INDEX_ACTION.equals(actionName);
        if (null == mappingKeysSet && actionName != null) {
            lookupKey.action=null;
            mappingKeysSet = mappingsListLookup.get(lookupKey);
            secondAttempt = true;
        }
        if (null == mappingKeysSet) return null;
        
        Set<String> lookupParams = new HashSet<String>(params.keySet());
        if (secondAttempt) {
            lookupParams.removeAll(DEFAULT_ACTION_PARAMS);
            lookupParams.addAll(DEFAULT_ACTION_PARAMS);
        }

        UrlMappingKey[] mappingKeys = (UrlMappingKey[]) mappingKeysSet.toArray(new UrlMappingKey[mappingKeysSet.size()]);
        for (int i = mappingKeys.length; i > 0; i--) {
            UrlMappingKey mappingKey = mappingKeys[i - 1];
            if (lookupParams.containsAll(mappingKey.paramNames)) {
                final UrlMapping mapping = mappingsLookup.get(mappingKey);
                if (canInferAction(actionName, secondAttempt, isIndexAction, mapping)) {
                    return mapping;
                }
                if (!secondAttempt) {
                    return mapping;
                }
            }
        }
        return null;
    }

    private boolean canInferAction(String actionName, boolean secondAttempt, boolean indexAction, UrlMapping mapping) {
        return secondAttempt && (indexAction || mapping.hasRuntimeVariable(GrailsControllerClass.ACTION) || (mapping.isRestfulMapping() && UrlMappingEvaluator.DEFAULT_REST_MAPPING.containsValue(actionName)));
    }

    /**
     * @see org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder#match(String)
     */
    public UrlMappingInfo match(String uri) {
        UrlMappingInfo info = null;
        if (cachedMatches.containsKey(uri)) {
            return cachedMatches.get(uri);
        }

        for (UrlMapping mapping : mappings) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempting to match URI [" + uri + "] with pattern [" + mapping.getUrlData().getUrlPattern() + "]");
            }

            info = mapping.match(uri);
            if (info != null) {
                cachedMatches.put(uri, info);
                break;
            }
        }

        return info;
    }

    public UrlMappingInfo[] matchAll(String uri) {
        List<UrlMappingInfo> matchingUrls = new ArrayList<UrlMappingInfo>();
        if (cachedListMatches.containsKey(uri)) {
            matchingUrls = cachedListMatches.get(uri);
        }
        else {
            for (UrlMapping mapping : mappings) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Attempting to match URI [" + uri + "] with pattern [" + mapping.getUrlData().getUrlPattern() + "]");
                }

                UrlMappingInfo current = mapping.match(uri);
                if (current != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Matched URI [" + uri + "] with pattern [" + mapping.getUrlData().getUrlPattern() + "], adding to posibilities");
                    }

                    matchingUrls.add(current);
                }
            }
            cachedListMatches.put(uri, matchingUrls);
        }
        return matchingUrls.toArray(new UrlMappingInfo[matchingUrls.size()]);
    }

    public UrlMappingInfo[] matchAll(String uri, String httpMethod) {
        return matchAll(uri);
    }

    public UrlMappingInfo matchStatusCode(int responseCode) {
        for (UrlMapping mapping : mappings) {
            if (mapping instanceof ResponseCodeUrlMapping) {
                ResponseCodeUrlMapping responseCodeUrlMapping = (ResponseCodeUrlMapping) mapping;
                if (responseCodeUrlMapping.getExceptionType() != null) continue;
                final UrlMappingInfo current = responseCodeUrlMapping.match(responseCode);
                if (current != null) return current;
            }
        }

        return null;
    }

    public UrlMappingInfo matchStatusCode(int responseCode, Throwable e) {
        for (UrlMapping mapping : mappings) {
            if (mapping instanceof ResponseCodeUrlMapping) {
                ResponseCodeUrlMapping responseCodeUrlMapping = (ResponseCodeUrlMapping) mapping;
                final UrlMappingInfo current = responseCodeUrlMapping.match(responseCode);
                if (current != null) {
                    if (responseCodeUrlMapping.getExceptionType() != null &&
                            responseCodeUrlMapping.getExceptionType().isInstance(e)) {
                        return current;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("URL Mappings");
        pw.println("------------");
        for (UrlMapping mapping : mappings) {
            pw.println(mapping);
        }
        pw.flush();
        return sw.toString();
    }

    /**
     * A class used as a key to lookup a UrlMapping based on controller, action and parameter names
     */
    @SuppressWarnings("unchecked")
    class UrlMappingKey implements Comparable {
        String controller;
        String action;
        Set<String> paramNames = Collections.EMPTY_SET;

        public UrlMappingKey(String controller, String action, Set<String> paramNames) {
            this.controller = controller;
            this.action = action;
            this.paramNames = paramNames;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UrlMappingKey that = (UrlMappingKey) o;

            if (action != null && !action.equals(that.action)) return false;
            if (controller != null && !controller.equals(that.controller)) return false;
            if (!paramNames.equals(that.paramNames)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (controller != null ? controller.hashCode() : 0);
            result = 31 * result + (action != null ? action.hashCode() : 0);
            result = 31 * result + paramNames.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("controller", controller)
                                            .append("action",action)
                                            .append("params", paramNames)
                                            .toString();
        }

        public int compareTo(Object o) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            //this optimization is usually worthwhile, and can always be added
            if (this == o) return EQUAL;

            final UrlMappingKey other = (UrlMappingKey)o;

            if (paramNames.size() < other.paramNames.size()) return BEFORE;
            if (paramNames.size() > other.paramNames.size()) return AFTER;

            int comparison = controller != null ? controller.compareTo(other.controller) : EQUAL;
            if (comparison != EQUAL) return comparison;

            comparison = action != null ? action.compareTo(other.action) : EQUAL;
            if (comparison != EQUAL) return comparison;

            return EQUAL;
        }
    }

    /**
     * A class used as a key to lookup a all UrlMappings based on only controller and action.
     */
    class UrlMappingsListKey {
        String controller;
        String action;

        public UrlMappingsListKey(String controller, String action) {
            this.controller = controller;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UrlMappingsListKey that = (UrlMappingsListKey) o;

            if (action != null && !action.equals(that.action)) return false;
            if (controller != null && !controller.equals(that.controller)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (controller != null ? controller.hashCode() : 0);
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this).append("controller", controller)
                                            .append("action",action)
                                            .toString();
        }
    }

    class UrlMappingsList {
        // A map from a UrlMappingsListKey to a list of UrlMappingKeys
        private Map<UrlMappingsListKey, SortedSet<UrlMappingKey>> lookup =
            new HashMap<UrlMappingsListKey, SortedSet<UrlMappingKey>>();

        public void put(UrlMappingsListKey key, UrlMappingKey mapping) {
            SortedSet<UrlMappingKey> mappingsList = lookup.get(key);
            if (null == mappingsList) {
                mappingsList = new TreeSet<UrlMappingKey>();
                lookup.put(key, mappingsList);
            }
            mappingsList.add(mapping);
        }

        public SortedSet<UrlMappingKey> get(UrlMappingsListKey key) {
            return lookup.get(key);
        }
    }

    public void setMaxWeightedCacheCapacity(int maxWeightedCacheCapacity) {
        this.maxWeightedCacheCapacity = maxWeightedCacheCapacity;
    }

    public void setUrlCreatorMaxWeightedCacheCapacity(int urlCreatorMaxWeightedCacheCapacity) {
        this.urlCreatorMaxWeightedCacheCapacity = urlCreatorMaxWeightedCacheCapacity;
    }
}
