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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import datadog.opentracing.DDTracer;
import io.opentracing.util.GlobalTracer;
import org.apache.druid.guice.JsonConfigProvider;
import org.apache.druid.guice.ManageLifecycle;
import org.apache.druid.initialization.DruidModule;
import org.apache.druid.java.util.emitter.core.Emitter;

import java.util.Collections;
import java.util.List;

public class OpenTracingEmitterModule implements DruidModule
{

  private static final String EMITTER_TYPE = "opentracing";

  @Override
  public List<? extends Module> getJacksonModules()
  {
    return Collections.emptyList();
  }

  @Override
  public void configure(Binder binder)
  {
    JsonConfigProvider.bind(binder, "druid.emitter." + EMITTER_TYPE, OpenTracingEmitterConfig.class);
  }

  @Provides
  @ManageLifecycle
  @Named(EMITTER_TYPE)
  public Emitter getEmitter(OpenTracingEmitterConfig config, ObjectMapper mapper)
  {
    // Note that while there are a lot of configs that we can use,
    // the datadog tracer picks up everything via sys properties/
    // env vars.
    // We should come back and add a proper config, though.
    // https://docs.datadoghq.com/tracing/manual_instrumentation/java/#setup
    // TODO move datadog behind a config
    DDTracer tracer = DDTracer.builder().serviceName("druid").build();
    GlobalTracer.registerIfAbsent(tracer);
    // register the same tracer with the Datadog API
    datadog.trace.api.GlobalTracer.registerIfAbsent(tracer);
    return new OpenTracingEmitter(tracer);
  }
}
