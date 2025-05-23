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

import java.util.Objects;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.metrics.Metrics;

public class FlinkFuncionTypeMetricsFactory implements FuncionTypeMetricsFactory {

  private final MetricGroup metricGroup;
  private final StatefulFunctionsConfig configuration;

  public FlinkFuncionTypeMetricsFactory(
      MetricGroup metricGroup, StatefulFunctionsConfig configuration) {
    this.metricGroup = Objects.requireNonNull(metricGroup);
    this.configuration = Objects.requireNonNull(configuration);
  }

  @Override
  public FunctionTypeMetrics forType(FunctionType functionType) {
    MetricGroup namespace =
        (configuration.getMetricFunctionNamespaceKey() != null)
            ? metricGroup.addGroup(
                configuration.getMetricFunctionNamespaceKey(), functionType.namespace())
            : metricGroup.addGroup(functionType.namespace());
    MetricGroup typeGroup =
        (configuration.getMetricFunctionTypeKey() != null)
            ? namespace.addGroup(configuration.getMetricFunctionTypeKey(), functionType.name())
            : namespace.addGroup(functionType.name());
    MetricGroup adapterGroup =
        MetricGroupAdapterFactoryUtil.getMetricGroupAdapterFactory(configuration)
            .createMetricGroupAdapter(typeGroup);
    Metrics functionTypeScopedMetrics = new FlinkUserMetrics(adapterGroup);
    return new FlinkFunctionTypeMetrics(adapterGroup, functionTypeScopedMetrics);
  }
}
