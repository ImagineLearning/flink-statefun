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
package org.apache.flink.statefun.flink.core;

import static org.apache.flink.configuration.description.TextElement.code;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.statefun.flink.core.message.MessageFactoryKey;
import org.apache.flink.statefun.flink.core.message.MessageFactoryType;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.InstantiationUtil;

/** Configuration that captures all stateful function related settings. */
@SuppressWarnings("WeakerAccess")
public class StatefulFunctionsConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String MODULE_CONFIG_PREFIX = "statefun.module.global-config.";

  // This configuration option exists for the documentation generator
  @SuppressWarnings("unused")
  public static final ConfigOption<String> MODULE_GLOBAL_DEFAULT =
      ConfigOptions.key(MODULE_CONFIG_PREFIX + "<KEY>")
          .stringType()
          .noDefaultValue()
          .withDescription(
              Description.builder()
                  .text(
                      "Adds the given key/value pair to the Stateful Functions global configuration.")
                  .text(
                      "These values will be available via the `globalConfigurations` parameter of StatefulFunctionModule#configure.")
                  .linebreak()
                  .text(
                      "Only the key <KEY> and value are added to the configuration. If the key/value pairs")
                  .list(
                      code(MODULE_CONFIG_PREFIX + "key1: value1"),
                      code(MODULE_CONFIG_PREFIX + "key2: value2"))
                  .text("are set, then the map")
                  .list(code("key1: value1"), code("key2: value2"))
                  .text("will be made available to your module at runtime.")
                  .build());

  public static final ConfigOption<MessageFactoryType> USER_MESSAGE_SERIALIZER =
      ConfigOptions.key("statefun.message.serializer")
          .enumType(MessageFactoryType.class)
          .defaultValue(MessageFactoryType.WITH_PROTOBUF_PAYLOADS)
          .withDescription("The serializer to use for on the wire messages.");

  public static final ConfigOption<String> USER_MESSAGE_CUSTOM_PAYLOAD_SERIALIZER_CLASS =
      ConfigOptions.key("statefun.message.custom-payload-serializer-class")
          .stringType()
          .noDefaultValue()
          .withDescription(
              "The custom payload serializer class to use with the WITH_CUSTOM_PAYLOADS serializer, which must implement MessagePayloadSerializer.");

  public static final ConfigOption<String> FLINK_JOB_NAME =
      ConfigOptions.key("statefun.flink-job-name")
          .stringType()
          .defaultValue("StatefulFunctions")
          .withDescription("The name to display at the Flink-UI");

  public static final ConfigOption<MemorySize> TOTAL_MEMORY_USED_FOR_FEEDBACK_CHECKPOINTING =
      ConfigOptions.key("statefun.feedback.memory.size")
          .memoryType()
          .defaultValue(MemorySize.ofMebiBytes(32))
          .withDescription(
              "The number of bytes to use for in memory buffering of the feedback channel, before spilling to disk.");

  public static final ConfigOption<Integer> ASYNC_MAX_OPERATIONS_PER_TASK =
      ConfigOptions.key("statefun.async.max-per-task")
          .intType()
          .defaultValue(32 * 1024)
          .withDescription(
              "The max number of async operations per task before backpressure is applied.");

  public static final ConfigOption<String> REMOTE_MODULE_NAME =
      ConfigOptions.key("statefun.remote.module-name")
          .stringType()
          .defaultValue("classpath:module.yaml")
          .withDescription(
              "The name of the remote module entity to look for. Also supported, file:///...");

  public static final ConfigOption<String> METRICS_FUNCTION_NAMESPACE_KEY =
      ConfigOptions.key("statefun.metrics.function.namespace.key")
          .stringType()
          .noDefaultValue()
          .withDescription(
              "Enable key/value metrics for functions using the supplied key in the 'namespace' MetricGroup");

  public static final ConfigOption<String> METRICS_FUNCTION_TYPE_KEY =
      ConfigOptions.key("statefun.metrics.function.type.key")
          .stringType()
          .noDefaultValue()
          .withDescription(
              "Enable key/value metrics for functions using the supplied key in the 'type' MetricGroup");

  public static final ConfigOption<String> METRIC_GROUP_ADAPTER_FACTORY_CLASS =
      ConfigOptions.key("statefun.metrics.group-adapter-factory-class")
          .stringType()
          .noDefaultValue()
          .withDescription(
              "The class name of a factory that creates a MetricGroupAdapter. The class must implement the MetricGroupAdapterFactory interface.");

  public static final ConfigOption<Boolean> EMBEDDED =
      ConfigOptions.key("statefun.embedded")
          .booleanType()
          .defaultValue(false)
          .withDescription(
              "True if Flink is running this job from an uber jar, rather than using statefun-specific docker images");

  /**
   * Creates a new {@link StatefulFunctionsConfig} based on the default configurations in the
   * current environment set via the {@code flink-conf.yaml}.
   */
  public static StatefulFunctionsConfig fromEnvironment(StreamExecutionEnvironment env) {
    Configuration configuration = FlinkConfigExtractor.reflectivelyExtractFromEnv(env);
    return new StatefulFunctionsConfig(configuration);
  }

  public static StatefulFunctionsConfig fromFlinkConfiguration(Configuration flinkConfiguration) {
    return new StatefulFunctionsConfig(flinkConfiguration);
  }

  private MessageFactoryType factoryType;

  private String customPayloadSerializerClassName;

  private String flinkJobName;

  private byte[] universeInitializerClassBytes;

  private MemorySize feedbackBufferSize;

  private int maxAsyncOperationsPerTask;

  private String remoteModuleName;

  private boolean embedded;

  private final Map<String, String> globalConfigurations = new HashMap<>();

  private String metricFunctionNamespaceKey;
  private String metricFunctionTypeKey;

  private String metricGroupAdapterFactoryClassName;

  /**
   * Create a new configuration object based on the values set in flink-conf.
   *
   * @param configuration a configuration to read the values from
   */
  private StatefulFunctionsConfig(Configuration configuration) {
    this.factoryType = configuration.get(USER_MESSAGE_SERIALIZER);
    this.customPayloadSerializerClassName =
        configuration.get(USER_MESSAGE_CUSTOM_PAYLOAD_SERIALIZER_CLASS);
    this.flinkJobName = configuration.get(FLINK_JOB_NAME);
    this.feedbackBufferSize = configuration.get(TOTAL_MEMORY_USED_FOR_FEEDBACK_CHECKPOINTING);
    this.maxAsyncOperationsPerTask = configuration.get(ASYNC_MAX_OPERATIONS_PER_TASK);
    this.remoteModuleName = configuration.get(REMOTE_MODULE_NAME);
    this.metricFunctionNamespaceKey = configuration.get(METRICS_FUNCTION_NAMESPACE_KEY);
    this.metricFunctionTypeKey = configuration.get(METRICS_FUNCTION_TYPE_KEY);
    this.metricGroupAdapterFactoryClassName = configuration.get(METRIC_GROUP_ADAPTER_FACTORY_CLASS);
    this.embedded = configuration.getBoolean(EMBEDDED);

    for (String key : configuration.keySet()) {
      if (key.startsWith(MODULE_CONFIG_PREFIX)) {
        String value = configuration.get(ConfigOptions.key(key).stringType().noDefaultValue());
        String userKey = key.substring(MODULE_CONFIG_PREFIX.length());
        globalConfigurations.put(userKey, value);
      }
    }
  }

  /** Returns the factory type used to serialize messages. */
  public MessageFactoryType getFactoryType() {
    return factoryType;
  }

  /**
   * Returns the custom payload serializer class name, when factory type is WITH_CUSTOM_PAYLOADS *
   */
  public String getCustomPayloadSerializerClassName() {
    return customPayloadSerializerClassName;
  }

  /** Returns the factory key * */
  public MessageFactoryKey getFactoryKey() {
    return MessageFactoryKey.forType(this.factoryType, this.customPayloadSerializerClassName);
  }

  /** Sets the factory type used to serialize messages. */
  public void setFactoryType(MessageFactoryType factoryType) {
    this.factoryType = Objects.requireNonNull(factoryType);
  }

  /** Sets the custom payload serializer class name * */
  public void setCustomPayloadSerializerClassName(String customPayloadSerializerClassName) {
    this.customPayloadSerializerClassName = customPayloadSerializerClassName;
  }

  /** Returns the Flink job name that appears in the Web UI. */
  public String getFlinkJobName() {
    return flinkJobName;
  }

  /** Set the Flink job name that appears in the Web UI. */
  public void setFlinkJobName(String flinkJobName) {
    this.flinkJobName = Objects.requireNonNull(flinkJobName);
  }

  /** Returns the number of bytes to use for in memory buffering of the feedback channel. */
  public MemorySize getFeedbackBufferSize() {
    return feedbackBufferSize;
  }

  /** Sets the number of bytes to use for in memory buffering of the feedback channel. */
  public void setFeedbackBufferSize(MemorySize size) {
    this.feedbackBufferSize = Objects.requireNonNull(size);
  }

  /** Returns the max async operations allowed per task. */
  public int getMaxAsyncOperationsPerTask() {
    return maxAsyncOperationsPerTask;
  }

  /** Sets the max async operations allowed per task. */
  public void setMaxAsyncOperationsPerTask(int maxAsyncOperationsPerTask) {
    this.maxAsyncOperationsPerTask = maxAsyncOperationsPerTask;
  }

  /** Returns the remote module name. */
  public String getRemoteModuleName() {
    return remoteModuleName;
  }

  /**
   * Sets a template for the remote module name.
   *
   * <p>By default the system will look for module.yaml in the classapth, to override that use
   * either a configuration parameter (see {@linkplain #REMOTE_MODULE_NAME}) or this getter.
   *
   * <p>The supported formats are either a file path, a file path prefixed with a {@code file:}
   * schema, or a name prefixed by {@code classpath:}
   */
  public void setRemoteModuleName(String remoteModuleName) {
    this.remoteModuleName = Objects.requireNonNull(remoteModuleName);
  }

  /** Returns whether the job was launched in embedded mode (see {@linkplain #EMBEDDED}). */
  public boolean isEmbedded() {
    return embedded;
  }

  /**
   * Sets the embedded mode. If true, disables certain validation steps. See documentation:
   * Configurations.
   */
  public void setEmbedded(boolean embedded) {
    this.embedded = embedded;
  }

  /**
   * Returns the key name to use for key/value metric groups at the function 'namespace' level of
   * the metric group hierarchy.
   */
  public String getMetricFunctionNamespaceKey() {
    return metricFunctionNamespaceKey;
  }

  public void setMetricFunctionNamespaceKey(String metricFunctionNamespaceKey) {
    this.metricFunctionNamespaceKey = metricFunctionNamespaceKey;
  }

  /**
   * Returns the key name to use for key/value metric groups at the function 'type' level of the
   * metric group hierarchy.
   */
  public String getMetricFunctionTypeKey() {
    return metricFunctionTypeKey;
  }

  public void setMetricFunctionTypeKey(String metricFunctionTypeKey) {
    this.metricFunctionTypeKey = metricFunctionTypeKey;
  }

  public String getMetricGroupAdapterFactoryClassName() {
    return metricGroupAdapterFactoryClassName;
  }

  public void setMetricGroupAdapterFactoryClassName(String metricGroupAdapterFactoryClassName) {
    this.metricGroupAdapterFactoryClassName = metricGroupAdapterFactoryClassName;
  }

  /**
   * Retrieves the universe provider for loading modules.
   *
   * @param cl The classloader on which the provider class is located.
   * @return A {@link StatefulFunctionsUniverseProvider}.
   */
  public StatefulFunctionsUniverseProvider getProvider(ClassLoader cl) {
    try {
      return InstantiationUtil.deserializeObject(universeInitializerClassBytes, cl);
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Unable to initialize.", e);
    }
  }

  /** Sets the universe provider used to load modules. */
  public void setProvider(StatefulFunctionsUniverseProvider provider) {
    try {
      universeInitializerClassBytes = InstantiationUtil.serializeObject(provider);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the global configurations passed to {@link
   * org.apache.flink.statefun.sdk.spi.StatefulFunctionModule#configure(Map,
   * StatefulFunctionModule.Binder)}.
   */
  public Map<String, String> getGlobalConfigurations() {
    return Collections.unmodifiableMap(globalConfigurations);
  }

  /** Adds all entries in this to the global configuration. */
  public void addAllGlobalConfigurations(Map<String, String> globalConfigurations) {
    this.globalConfigurations.putAll(globalConfigurations);
  }

  /**
   * Adds the given key/value pair to the global configuration.
   *
   * @param key the key of the key/value pair to be added
   * @param value the value of the key/value pair to be added
   */
  public void setGlobalConfiguration(String key, String value) {
    this.globalConfigurations.put(key, value);
  }
}
