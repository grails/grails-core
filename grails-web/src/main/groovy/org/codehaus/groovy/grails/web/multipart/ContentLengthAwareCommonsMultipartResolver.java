/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.multipart;

import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * Safari includes the multipart packet inside an HTTP redirect without the Content-Length header. This causes
 * CommonsMultipartResolver to blow up. We extend it here to catch this exception, printing a warning.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class ContentLengthAwareCommonsMultipartResolver extends CommonsMultipartResolver {

    private static final LinkedMultiValueMap<String, MultipartFile> EMPTY_MULTI_VALUE_MAP = new LinkedMultiValueMap<String, MultipartFile>();

    private static final Log LOG = LogFactory.getLog(ContentLengthAwareCommonsMultipartResolver.class);

    @SuppressWarnings("deprecation")
    @Override
    protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
        try {
            return super.parseRequest(request);
        } catch (MultipartException e) {
            if (e.getCause() != null && e.getCause().getClass().equals(FileUploadBase.UnknownSizeException.class)) {
                LOG.warn(e.getMessage());
                Map<String, String[]> emptyMpParams = Collections.emptyMap();
                Map<String, String> emptyMpParamContentTypes = Collections.emptyMap();
                return new MultipartParsingResult(EMPTY_MULTI_VALUE_MAP, emptyMpParams, emptyMpParamContentTypes);
            }
            throw e;
        }
    }
}
