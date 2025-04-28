/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.statefun.flink.core.metrics;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for obtaining the MetricGroupAdapterFactory. */
public class MetricGroupAdapterFactoryUtil {

  private static final Logger LOG = LoggerFactory.getLogger(MetricGroupAdapterFactoryUtil.class);

  private static MetricGroupAdapterFactory factory = null;

  public static MetricGroupAdapterFactory getMetricGroupAdapterFactory(
      StatefulFunctionsConfig config) {

    if (factory != null) {
      return factory;
    }

    if (StringUtils.isNotEmpty(config.getMetricGroupAdapterFactoryClassName())) {
      try {
        Class<?> clazz = Class.forName(config.getMetricGroupAdapterFactoryClassName());
        if (MetricGroupAdapterFactory.class.isAssignableFrom(clazz)) {
          factory = (MetricGroupAdapterFactory) clazz.getDeclaredConstructor().newInstance();
          return factory;
        } else {
          LOG.warn(
              "Class {} is not a MetricGroupAdapterFactory.",
              config.getMetricGroupAdapterFactoryClassName());
        }
      } catch (Exception e) {
        LOG.warn(
            "Failed to create MetricGroupAdapterFactory from class: {}.",
            config.getMetricGroupAdapterFactoryClassName(),
            e);
      }
    }
    LOG.info("Using default MetricGroupAdapterFactory.");
    factory = adapted -> adapted;
    return factory;
  }
}
