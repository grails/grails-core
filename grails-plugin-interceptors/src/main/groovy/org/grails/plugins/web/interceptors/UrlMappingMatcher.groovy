/*
 * Copyright 2014 original authors
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
package org.grails.plugins.web.interceptors

import grails.artefact.Interceptor
import grails.interceptors.Matcher
import grails.util.Environment
import grails.web.mapping.UrlMappingInfo
import groovy.transform.CompileStatic
import org.springframework.util.AntPathMatcher

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern



/**
 * Used to match {@link UrlMappingInfo} instance by {@link grails.artefact.Interceptor} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class UrlMappingMatcher implements Matcher {

    public static Pattern WILD_CARD_PATTERN = ~/.*/
    private static final Map<Integer, Boolean> CACHED_MATCHES = new ConcurrentHashMap<>()

    protected Pattern controllerRegex = WILD_CARD_PATTERN
    protected Pattern actionRegex = WILD_CARD_PATTERN
    protected Pattern namespaceRegex = WILD_CARD_PATTERN
    protected Pattern methodRegex = WILD_CARD_PATTERN

    protected List<Exclude> excludes = []
    protected Interceptor interceptor
    protected List<String> uriPatterns = []
    protected List<String> uriExcludePatterns = []
    protected AntPathMatcher pathMatcher = new AntPathMatcher()
    protected boolean matchAll = false

    UrlMappingMatcher(Interceptor interceptor) {
        this.interceptor = interceptor
    }

    boolean doesMatch(String uri, UrlMappingInfo info) {
        boolean hasUriPatterns = !uriPatterns.isEmpty()

        boolean isNotExcluded = !isExcluded(uri, info)
        if(matchAll && isNotExcluded) return true

        if(hasUriPatterns) {
            uri = uri.replace(';', '')
            for(pattern in uriPatterns) {
                if( pathMatcher.match(pattern, uri)  && isNotExcluded) {
                    return true
                }
            }
        }

        if(info) {
            def infoCode = info.hashCode()
            Boolean matched = CACHED_MATCHES.get(infoCode)
            if(matched != null) return matched

            if(doesMatchInternal(info)
                && isNotExcluded) {
                if(Environment.current == Environment.PRODUCTION) {
                    CACHED_MATCHES.put(infoCode, Boolean.TRUE)
                }
                return true
            }
        }
        return false
    }

    protected boolean isExcluded(String uri, UrlMappingInfo info) {
        if( uriExcludePatterns.any() { String p -> pathMatcher.match(p, uri)} ) {
            return true
        }
        if(info) {
            for(exclude in excludes) {
                if(exclude.isExcluded(info)) {
                    return true
                }
            }
        }
        false
    }

    protected boolean doesMatchInternal(UrlMappingInfo info) {
        (info != null &&
            ((info.controllerName ?: '') ==~ controllerRegex) &&
            ((info.actionName ?: '') ==~ actionRegex) &&
            ((info.namespace ?: '') ==~ namespaceRegex) &&
            ((info.httpMethod ?: '') ==~ methodRegex))
    }

    @Override
    Matcher matchAll() {
        matchAll = true
        return this
    }

    @Override
    Matcher matches(Map arguments) {
        if(arguments.uri) {
            uriPatterns << arguments.uri.toString()
        }
        else {
            controllerRegex = regexMatch( arguments, "controller")
            actionRegex = regexMatch( arguments, "action")
            namespaceRegex = regexMatch( arguments, "namespace")
            methodRegex = regexMatch( arguments, "method")
        }
        return this
    }

    @Override
    Matcher excludes(Map arguments) {
        if(arguments.uri) {
            uriExcludePatterns << arguments.uri.toString()
        }
        else {
            def exclude = new MapExclude()
            exclude.controllerExcludesRegex = regexMatch( arguments, "controller", null)
            exclude.actionExcludesRegex = regexMatch( arguments, "action", null)
            exclude.namespaceExcludesRegex = regexMatch( arguments, "namespace", null)
            exclude.methodExcludesRegex = regexMatch( arguments, "method", null)
            excludes << exclude
        }
        return this
    }

    @Override
    Matcher except(Map arguments) {
        excludes(arguments)
    }

    @Override
    Matcher excludes(Closure<Boolean> condition) {
        excludes << new ClosureExclude(interceptor, condition)
        return this
    }

    private Pattern regexMatch(Map arguments, String type, Pattern defaultPattern = WILD_CARD_PATTERN) {
        def value = arguments.get(type)
        if(!value) return defaultPattern
        if(value instanceof Pattern) {
            return (Pattern)value
        }
        else {
            def str = value.toString()
            if(str == '*') return defaultPattern
            else {
                return Pattern.compile(str)
            }
        }
    }

    static interface Exclude {
        boolean isExcluded(UrlMappingInfo info)
    }

    static class ClosureExclude implements Exclude {
        Interceptor interceptor
        Closure<Boolean> callable

        ClosureExclude(Interceptor interceptor, Closure<Boolean> callable) {
            this.interceptor = interceptor
            this.callable = callable
        }

        @Override
        boolean isExcluded(UrlMappingInfo info) {
            if(callable) {
                callable.delegate = interceptor
                return callable.call()
            }
            return false
        }
    }

    static class MapExclude implements Exclude {
        Pattern controllerExcludesRegex
        Pattern actionExcludesRegex
        Pattern namespaceExcludesRegex
        Pattern methodExcludesRegex
        @Override
        boolean isExcluded(UrlMappingInfo info) {
            (controllerExcludesRegex != null && ((info.controllerName ?: '') ==~ controllerExcludesRegex)) ||
            (actionExcludesRegex != null && ((info.actionName ?: '') ==~ actionExcludesRegex)) ||
            (namespaceExcludesRegex != null && ((info.namespace ?: '') ==~ namespaceExcludesRegex)) ||
            (methodExcludesRegex != null && ((info.httpMethod ?: '') ==~ methodExcludesRegex))
        }
    }
}
