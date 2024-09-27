/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins.codecs

import org.grails.encoder.impl.URLCodecFactory
import org.springframework.web.context.request.RequestContextHolder

/**
 * A codec that encodes and decodes Objects to and from URL encoded strings.
 *
 * @author Marc Palmer
 * @since 0.5
 */
class URLCodec extends URLCodecFactory {
    protected String resolveEncoding() {
        RequestContextHolder.getRequestAttributes()?.request?.characterEncoding ?: 'UTF-8'
    }
}
