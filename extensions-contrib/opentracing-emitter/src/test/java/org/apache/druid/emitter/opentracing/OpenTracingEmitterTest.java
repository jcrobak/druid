/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.emitter.opentracing;

import com.google.common.collect.ImmutableMap;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.apache.druid.java.util.common.DateTimes;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class OpenTracingEmitterTest
{

  @Test
  public void testCreateBrokerSpan()
  {
    String traceKey = "traceid";
    String spanKey = "spanid";
    // 2020-06-01T13:22:16,412 INFO [qtp956448191-127] org.apache.druid.emitter.opentracing.OpenTracingEmitter - Emit called for event {feed=metrics,map={feed=metrics, timestamp=2020-06-01T13:22:16.412Z, service=druid/broker, host=localhost:8082, version=0.17.0-incubating-SNAPSHOT, metric=query/time, value=3845, context={api.view=cloud, priority=2, queryId=7717c080-86ed-483e-9a00-979b34cd5d86, skipEmptyBuckets=true, sortByDimsFirst=true, user.superuser=true}, dataSource=telemetry_sandbox, duration=PT700000S, hasFilters=true, id=7717c080-86ed-483e-9a00-979b34cd5d86, interval=[2019-10-16T16:30:20.000Z/2019-10-24T18:57:00.000Z], numComplexMetrics=0, numDimensions=1, numMetrics=1, remoteAddress=127.0.0.1, success=true, type=groupBy}}
    String id = "b3dgccb3-c0c9-475g-c9e5-03362g0b10b8";
    long traceId = 4077920609876211111L;
    ServiceMetricEvent testEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "data-source")
        .setDimension("type", "groupBy")
        .setDimension("interval", "2013/2015")
        .setDimension("some_random_dim1", "random_dim_value1")
        .setDimension("some_random_dim2", "random_dim_value2")
        .setDimension("hasFilters", "no")
        .setDimension("duration", "P1D")
        .setDimension("remoteAddress", "194.0.90.2")
        .setDimension("id", id)
        .setDimension("context", ImmutableMap.of(traceKey, traceId))
        .build(DateTimes.utc(123 + 456), "query/time", 456)
        .build("broker", "brokerHost1");

    MockTracer tracer = new MockTracer();
    OpenTracingEmitter emitter = new OpenTracingEmitter(tracer);

    emitter.emit(testEvent);

    List<MockSpan> spans = tracer.finishedSpans();

    Assert.assertTrue(
        "Found multiple span but only expected one. Spans=[" + spans + "]",
        spans.size() == 1
    );

    MockSpan span = spans.get(0);
    Assert.assertEquals("Operation name", "query", span.operationName());
    Assert.assertEquals("Start time", 123L, span.startMicros());
    Assert.assertEquals("End time", 123L + 456L, span.finishMicros());

    Assert.assertEquals("tags",
                        ImmutableMap.of("span.kind", "server", "component", "broker"), span.tags()
    );
    //Assert.assertEquals("Parent id", OpenTracingEmitter.buildSpanId("router", id), span.parentId());
    Assert.assertEquals(traceId, span.context().traceId());
    // TODO it doesn't seem possible to override the span id, unfortunately.
    //Assert.assertEquals(OpenTracingEmitter.buildSpanId("broker", id), span.context().spanId());
  }
}
