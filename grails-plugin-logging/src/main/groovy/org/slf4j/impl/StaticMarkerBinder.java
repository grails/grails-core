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

import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;

/**
 * Implementation of the StaticMarkerBinder for slf4j
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class StaticMarkerBinder {
  /**
   * The unique instance of this class.
   */
  public static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

  final IMarkerFactory markerFactory = new BasicMarkerFactory();

  private StaticMarkerBinder() {
  }

  /**
   * Currently this method always returns an instance of
   * {@link BasicMarkerFactory}.
   */
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  /**
   * Currently, this method returns the class name of
   * {@link BasicMarkerFactory}.
   */
  public String getMarkerFactoryClassStr() {
    return BasicMarkerFactory.class.getName();
  }
}
