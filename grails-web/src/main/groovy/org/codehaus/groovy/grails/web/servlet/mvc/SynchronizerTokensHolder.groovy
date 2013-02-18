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
package org.codehaus.groovy.grails.web.servlet.mvc

import javax.servlet.http.HttpSession
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A token used to handle double-submits.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class SynchronizerTokensHolder implements Serializable {

    public static final String HOLDER = "org.codehaus.groovy.grails.SYNCHRONIZER_TOKENS_HOLDER"

    public static final String TOKEN_KEY = "org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN"
    public static final String TOKEN_URI = "org.codehaus.groovy.grails.SYNCHRONIZER_URI"

    Map<String, Set<UUID>> currentTokens= [:].withDefault { new CopyOnWriteArraySet<UUID>() };

    SynchronizerTokensHolder() {
    }

    boolean isValid(String url, String token) {
        final uuid = UUID.fromString(token)
        currentTokens[url]?.contains(uuid)
    }

    String generateToken(String url) {
        final uuid = UUID.randomUUID()
        currentTokens[url].add(uuid)
        return uuid
    }

    void resetToken(String url) {
        currentTokens.remove(url)
    }

    void resetToken(String url, String token) {
        if (url && token) {
            final set = currentTokens[url]
            set.remove(UUID.fromString(token))
            if (set.isEmpty()) {
                currentTokens.remove(url)
            }
        }
    }

    boolean isEmpty() {
        return currentTokens.isEmpty() || currentTokens.every { String url, Set<UUID> uuids -> uuids.isEmpty() }
    }

    static SynchronizerTokensHolder store(HttpSession session) {
        SynchronizerTokensHolder tokensHolder = session.getAttribute(HOLDER) ?: new SynchronizerTokensHolder()
        session.setAttribute(HOLDER, tokensHolder)
        return tokensHolder
    }
}
