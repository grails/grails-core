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
package org.slf4j.impl;

import org.apache.log4j.MDC;
import org.slf4j.spi.MDCAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsLog4jMDCAdapter implements MDCAdapter {
  public void clear() {
    Map map = org.apache.log4j.MDC.getContext();
    if (map != null) {
      map.clear();
    }
  }

  public String get(String key) {
    return (String) org.apache.log4j.MDC.get(key);
  }

  public void put(String key, String val) {
    org.apache.log4j.MDC.put(key, val);
  }

  public void remove(String key) {
    org.apache.log4j.MDC.remove(key);
  }

  public Map getCopyOfContextMap() {
    Map old = org.apache.log4j.MDC.getContext();
    if(old != null) {
      return new HashMap(old);
    } else {
      return null;
    }
  }

  public void setContextMap(Map contextMap) {
    Map old = org.apache.log4j.MDC.getContext();
    if(old == null) {
        for (Object o : contextMap.entrySet()) {
            Map.Entry mapEntry = (Map.Entry) o;
            MDC.put((String) mapEntry.getKey(), mapEntry.getValue());
        }
    } else {
      old.clear();
      old.putAll(contextMap);
    }
  }
}
