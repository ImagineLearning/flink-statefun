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
package org.apache.flink.statefun.flink.core.functions;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.accumulators.*;
import org.apache.flink.api.common.accumulators.Histogram;
import org.apache.flink.api.common.cache.DistributedCache;
import org.apache.flink.api.common.externalresource.ExternalResourceInfo;
import org.apache.flink.api.common.functions.BroadcastVariableInitializer;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.*;
import org.apache.flink.metrics.groups.OperatorMetricGroup;
import org.apache.flink.runtime.state.*;
import org.apache.flink.runtime.state.heap.HeapPriorityQueueElement;
import org.apache.flink.runtime.state.internal.InternalListState;
import org.apache.flink.shaded.guava31.com.google.common.util.concurrent.MoreExecutors;
import org.apache.flink.statefun.flink.core.StatefulFunctionsConfig;
import org.apache.flink.statefun.flink.core.StatefulFunctionsUniverse;
import org.apache.flink.statefun.flink.core.TestUtils;
import org.apache.flink.statefun.flink.core.backpressure.ThresholdBackPressureValve;
import org.apache.flink.statefun.flink.core.message.Message;
import org.apache.flink.statefun.flink.core.message.MessageFactoryKey;
import org.apache.flink.statefun.flink.core.message.MessageFactoryType;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.Triggerable;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.LatencyMarker;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.watermarkstatus.WatermarkStatus;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.function.BiConsumerWithException;
import org.junit.Test;

public class ReductionsTest {

  @Test
  public void testFactory() {
    Reductions reductions =
        Reductions.create(
            new ThresholdBackPressureValve(-1),
            new StatefulFunctionsUniverse(
                MessageFactoryKey.forType(MessageFactoryType.WITH_KRYO_PAYLOADS, null)),
            new FakeRuntimeContext(),
            new FakeKeyedStateBackend(),
            new FakeTimerServiceFactory(),
            new FakeInternalListState(),
            new FakeMapState<>(),
            new HashMap<>(),
            new FakeOutput(),
            TestUtils.ENVELOPE_FACTORY,
            MoreExecutors.directExecutor(),
            new FakeMetricGroup(),
            new FakeMapState<>(),
            StatefulFunctionsConfig.fromFlinkConfiguration(new Configuration()));

    assertThat(reductions, notNullValue());
  }

  @SuppressWarnings("deprecation")
  private static final class FakeRuntimeContext implements RuntimeContext {

    @Override
    public <T> ValueState<T> getState(ValueStateDescriptor<T> stateProperties) {
      return new ValueState<T>() {
        @Override
        public T value() {
          return null;
        }

        @Override
        public void update(T value) {}

        @Override
        public void clear() {}
      };
    }

    @Override
    public <UK, UV> MapState<UK, UV> getMapState(MapStateDescriptor<UK, UV> stateProperties) {
      return new MapState<UK, UV>() {
        @Override
        public UV get(UK key) {
          return null;
        }

        @Override
        public void put(UK key, UV value) {}

        @Override
        public void putAll(Map<UK, UV> map) {}

        @Override
        public void remove(UK key) {}

        @Override
        public boolean contains(UK key) {
          return false;
        }

        @Override
        public Iterable<Entry<UK, UV>> entries() {
          return null;
        }

        @Override
        public Iterable<UK> keys() {
          return null;
        }

        @Override
        public Iterable<UV> values() {
          return null;
        }

        @Override
        public Iterator<Entry<UK, UV>> iterator() {
          return null;
        }

        @Override
        public boolean isEmpty() throws Exception {
          return true;
        }

        @Override
        public void clear() {}
      };
    }

    @Override
    public ExecutionConfig getExecutionConfig() {
      return new ExecutionConfig();
    }

    // everything below this line would throw UnspportedOperationException()

    @Override
    public String getTaskName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public OperatorMetricGroup getMetricGroup() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getNumberOfParallelSubtasks() {
      return 0;
    }

    @Override
    public int getMaxNumberOfParallelSubtasks() {
      return 0;
    }

    @Override
    public int getIndexOfThisSubtask() {
      return 0;
    }

    @Override
    public int getAttemptNumber() {
      return 0;
    }

    @Override
    public String getTaskNameWithSubtasks() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getUserCodeClassLoader() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <V, A extends Serializable> void addAccumulator(
        String name, Accumulator<V, A> accumulator) {}

    @Override
    public <V, A extends Serializable> Accumulator<V, A> getAccumulator(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IntCounter getIntCounter(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LongCounter getLongCounter(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DoubleCounter getDoubleCounter(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Histogram getHistogram(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<ExternalResourceInfo> getExternalResourceInfos(String resourceName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasBroadcastVariable(String name) {
      return false;
    }

    @Override
    public <RT> List<RT> getBroadcastVariable(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T, C> C getBroadcastVariableWithInitializer(
        String name, BroadcastVariableInitializer<T, C> initializer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DistributedCache getDistributedCache() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> ListState<T> getListState(ListStateDescriptor<T> stateProperties) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> ReducingState<T> getReducingState(ReducingStateDescriptor<T> stateProperties) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <IN, ACC, OUT> AggregatingState<IN, OUT> getAggregatingState(
        AggregatingStateDescriptor<IN, ACC, OUT> stateProperties) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerUserCodeClassLoaderReleaseHookIfAbsent(String s, Runnable runnable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public JobID getJobId() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeKeyedStateBackend implements KeyedStateBackend<Object> {

    @Override
    public <N, S extends State, T> void applyToAllKeys(
        N namespace,
        TypeSerializer<N> namespaceSerializer,
        StateDescriptor<S, T> stateDescriptor,
        KeyedStateFunction<Object, S> function) {}

    @Override
    public <N> Stream<Object> getKeys(String state, N namespace) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <N, S extends State, T> S getOrCreateKeyedState(
        TypeSerializer<N> namespaceSerializer, StateDescriptor<S, T> stateDescriptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <N, S extends State> S getPartitionedState(
        N namespace, TypeSerializer<N> namespaceSerializer, StateDescriptor<S, ?> stateDescriptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {}

    @Override
    public void registerKeySelectionListener(KeySelectionListener<Object> listener) {}

    @Override
    public boolean deregisterKeySelectionListener(KeySelectionListener<Object> listener) {
      return false;
    }

    @Nonnull
    @Override
    public <N, SV, SEV, S extends State, IS extends S> IS createOrUpdateInternalState(
        @Nonnull TypeSerializer<N> typeSerializer,
        @Nonnull StateDescriptor<S, SV> stateDescriptor,
        @Nonnull
            StateSnapshotTransformer.StateSnapshotTransformFactory<SEV>
                stateSnapshotTransformFactory)
        throws Exception {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public <T extends HeapPriorityQueueElement & PriorityComparable<? super T> & Keyed<?>>
        KeyGroupedInternalPriorityQueue<T> create(
            @Nonnull String s, @Nonnull TypeSerializer<T> typeSerializer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object getCurrentKey() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentKey(Object newKey) {}

    @Override
    public TypeSerializer<Object> getKeySerializer() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <N> Stream<Tuple2<Object, N>> getKeysAndNamespaces(String state) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeTimerServiceFactory implements TimerServiceFactory {

    @Override
    public InternalTimerService<VoidNamespace> createTimerService(
        Triggerable<String, VoidNamespace> triggerable) {
      return new FakeTimerService();
    }
  }

  private static final class FakeTimerService implements InternalTimerService<VoidNamespace> {

    @Override
    public long currentProcessingTime() {
      return 0;
    }

    @Override
    public long currentWatermark() {
      return 0;
    }

    @Override
    public void registerEventTimeTimer(VoidNamespace namespace, long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerProcessingTimeTimer(VoidNamespace namespace, long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEventTimeTimer(VoidNamespace namespace, long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProcessingTimeTimer(VoidNamespace namespace, long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void forEachEventTimeTimer(
        BiConsumerWithException<VoidNamespace, Long, Exception> consumer) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void forEachProcessingTimeTimer(
        BiConsumerWithException<VoidNamespace, Long, Exception> consumer) throws Exception {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeInternalListState
      implements InternalListState<String, Long, Message> {

    @Override
    public void add(Message value) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(List<Message> values) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void update(List<Message> values) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void updateInternal(List<Message> valueToStore) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentNamespace(Long namespace) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getSerializedValue(
        byte[] serializedKeyAndNamespace,
        TypeSerializer<String> safeKeySerializer,
        TypeSerializer<Long> safeNamespaceSerializer,
        TypeSerializer<List<Message>> safeValueSerializer)
        throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Message> getInternal() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Message> get() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void mergeNamespaces(Long target, Collection<Long> sources) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public StateIncrementalVisitor<String, Long, List<Message>> getStateIncrementalVisitor(
        int recommendedMaxNumberOfReturnedRecords) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TypeSerializer<Long> getNamespaceSerializer() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TypeSerializer<String> getKeySerializer() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TypeSerializer<List<Message>> getValueSerializer() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeMapState<K, V> implements MapState<K, V> {

    @Override
    public V get(K key) throws Exception {
      return null;
    }

    @Override
    public void put(K key, V value) throws Exception {}

    @Override
    public void putAll(Map<K, V> map) throws Exception {}

    @Override
    public void remove(K key) throws Exception {}

    @Override
    public boolean contains(K key) throws Exception {
      return false;
    }

    @Override
    public Iterable<Entry<K, V>> entries() throws Exception {
      return null;
    }

    @Override
    public Iterable<K> keys() throws Exception {
      return null;
    }

    @Override
    public Iterable<V> values() throws Exception {
      return null;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() throws Exception {
      return null;
    }

    @Override
    public boolean isEmpty() throws Exception {
      return true;
    }

    @Override
    public void clear() {}
  }

  private static final class FakeOutput implements Output<StreamRecord<Message>> {

    @Override
    public void emitWatermark(Watermark mark) {}

    @Override
    public void emitWatermarkStatus(WatermarkStatus watermarkStatus) {}

    @Override
    public <X> void collect(OutputTag<X> outputTag, StreamRecord<X> record) {}

    @Override
    public void emitLatencyMarker(LatencyMarker latencyMarker) {}

    @Override
    public void collect(StreamRecord<Message> record) {}

    @Override
    public void close() {}
  }

  private static final class FakeMetricGroup implements MetricGroup {
    @Override
    public Counter counter(int i) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Counter counter(String s) {
      return new SimpleCounter();
    }

    @Override
    public <C extends Counter> C counter(int i, C c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <C extends Counter> C counter(String s, C c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T, G extends Gauge<T>> G gauge(int i, G g) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T, G extends Gauge<T>> G gauge(String s, G g) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <H extends org.apache.flink.metrics.Histogram> H histogram(String s, H h) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <H extends org.apache.flink.metrics.Histogram> H histogram(int i, H h) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Meter> M meter(String s, M m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Meter> M meter(int i, M m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MetricGroup addGroup(int i) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MetricGroup addGroup(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MetricGroup addGroup(String s, String s1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String[] getScopeComponents() {
      return new String[0];
    }

    @Override
    public Map<String, String> getAllVariables() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getMetricIdentifier(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getMetricIdentifier(String s, CharacterFilter characterFilter) {
      throw new UnsupportedOperationException();
    }
  }
}
