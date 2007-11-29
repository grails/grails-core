/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.codehaus.groovy.grails.validation.routines;

/**
 * Groups tests and expected results.
 *
 * @version $Revision: 588091 $ $Date: 2007-10-25 04:17:42 +0400 (Чт, 25 окт 2007) $
 */
 public class ResultPair {
      public String item;
      public boolean valid;

      public ResultPair(String item, boolean valid) {
         this.item = item;
         this.valid = valid;  //Weather the individual part of url is valid.
      }
   }
