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

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.java.util.emitter.core.Emitter;
import org.apache.druid.java.util.emitter.core.Event;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenTracingEmitter implements Emitter
{
  private static final Logger log = new Logger(OpenTracingEmitter.class);
  private final Tracer tracer;

  public OpenTracingEmitter(Tracer tracer)
  {
    this.tracer = tracer;
  }

  @Override
  public void start()
  {
    log.info("Starting OpenTracingEmitter...");
  }

  @Override
  public void emit(Event e)
  {
    log.trace("Emit called for event %s", toString(e));

    if (!(e instanceof ServiceMetricEvent)) {
      return;
    }
    ServiceMetricEvent event = (ServiceMetricEvent) e;
    // We generate spans for the following types of events:
    // query/time
    if (!event.getMetric().equals("query/time")) {
      return;
    }
    String operationName = event.getService() + "/query";

    DateTime endTime = event.getCreatedTime();
    DateTime startTime = event.getCreatedTime().minusMillis(event.getValue().intValue());

    SpanContext parentSpan = tracer.extract(Format.Builtin.TEXT_MAP_EXTRACT,
            new TextMapExtractAdapter(getContextAsString(event)));

    Tracer.SpanBuilder spanBuilder = tracer.buildSpan("druid").asChildOf(parentSpan)
                                           .withStartTimestamp(startTime.getMillis() * 1000)
                                           .withTag(Tags.COMPONENT.getKey(), event.getService())
                                           .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
    Span span = spanBuilder.start();

    // TODO ideally we'd have a way to propogate the context of the current span,
    // so that we could associated a causal relationship between the router/broker/etc.

    log.info("Traceid: %s, Spanid: %s, start: %d, end: %d, %s", span.context().toTraceId(),
            span.context().toSpanId(), startTime.getMillis(), endTime.getMillis(), span.toString());
    span.setOperationName(operationName);
    addTags(span, event);
    span.finish(endTime.getMillis() * 1000);
  }

  private static String toString(Event e)
  {
    return "{feed=" + e.getFeed() + ",map=" + e.toMap() + "}";
  }

  private static void addTags(Span span, ServiceMetricEvent event)
  {
    getContext(event).forEach((String key, Object value) -> {
      // TODO filter out all the things that are consumed by the extractor.
      span.setTag(key, value.toString());
    });
  }

  private static Map<String, String> getContextAsString(ServiceMetricEvent event)
  {
    return getContext(event).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
  }

  private static Map<String, Object> getContext(ServiceMetricEvent event)
  {
    Object context = event.getUserDims().get("context");
    if (!(context instanceof Map)) {
      return Collections.emptyMap();
    }
    return (Map<String, Object>) context;
  }


  @Override
  public void flush() throws IOException
  {
  }

  @Override
  public void close() throws IOException
  {
    tracer.close();
  }
}
