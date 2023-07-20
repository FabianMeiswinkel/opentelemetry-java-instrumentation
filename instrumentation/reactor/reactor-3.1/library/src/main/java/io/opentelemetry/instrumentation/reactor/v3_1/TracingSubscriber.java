/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Includes work from:
/*
 * Copyright 2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.opentelemetry.instrumentation.reactor.v3_1;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * Based on OpenTracing code.
 * https://github.com/opentracing-contrib/java-reactor/blob/master/src/main/java/io/opentracing/contrib/reactor/TracedSubscriber.java
 */
public class TracingSubscriber<T> implements CoreSubscriber<T> {
  private static final Class<?> fluxRetrySubscriberClass = getFluxRetrySubscriberClass();
  private static final Class<?> fluxRetryWhenSubscriberClass = getFluxRetryWhenSubscriberClass();
  private final io.opentelemetry.context.Context traceContext;

  private final io.opentelemetry.context.Context validTraceContextOrNull;
  private final Subscriber<? super T> subscriber;
  private final Context context;

  public TracingSubscriber(Subscriber<? super T> subscriber, Context ctx) {
    this(subscriber, ctx, io.opentelemetry.context.Context.current());
  }

  public TracingSubscriber(
      Subscriber<? super T> subscriber,
      Context ctx,
      io.opentelemetry.context.Context contextToPropagate) {
    this.subscriber = subscriber;
    this.context = ctx;
    this.traceContext = ContextPropagationOperator.getOpenTelemetryContext(ctx, contextToPropagate);
    boolean hasContextToPropagate =
        traceContext == null ? false : Span.fromContext(traceContext).getSpanContext().isValid();
    this.validTraceContextOrNull = hasContextToPropagate ? traceContext : null;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
      if (validTraceContextOrNull != io.opentelemetry.context.Context.current()) {
          withActiveSpan(() -> subscriber.onSubscribe(subscription));
      } else {
          subscriber.onSubscribe(subscription);
      }
  }

  @Override
  public void onNext(T o) {
      if (validTraceContextOrNull != io.opentelemetry.context.Context.current()) {
          withActiveSpan(() -> subscriber.onNext(o));
      } else {
          subscriber.onNext(o);
      }
  }

  @Override
  public void onError(Throwable throwable) {
    if (validTraceContextOrNull == null
        && (fluxRetrySubscriberClass == subscriber.getClass()
            || fluxRetryWhenSubscriberClass == subscriber.getClass())) {
      // clear context for retry to avoid having retried operations run with currently active
      // context as parent context
      withActiveSpan(io.opentelemetry.context.Context.root(), () -> subscriber.onError(throwable));
    } else {
        if (validTraceContextOrNull != io.opentelemetry.context.Context.current()) {
            withActiveSpan(() -> subscriber.onError(throwable));
        } else {
            subscriber.onError(throwable);
        }
    }
  }

  @Override
  public void onComplete() {
      if (validTraceContextOrNull != io.opentelemetry.context.Context.current()) {
          withActiveSpan(subscriber::onComplete);
      } else {
          subscriber.onComplete();
      }
  }

  @Override
  public Context currentContext() {
    return context;
  }

  private void withActiveSpan(Runnable runnable) {
    if (validTraceContextOrNull != io.opentelemetry.context.Context.current()) {
        withActiveSpan(validTraceContextOrNull, runnable);
    } else {
        runnable.run();
    }
  }

  private static void withActiveSpan(io.opentelemetry.context.Context context, Runnable runnable) {
      if (context != null && io.opentelemetry.context.Context.current() != context) {
          try (Scope ignored = context.makeCurrent()) {
              runnable.run();
          }
      } else {
          runnable.run();
      }
  }

  private static Class<?> getFluxRetrySubscriberClass() {
    try {
      return Class.forName("reactor.core.publisher.FluxRetry$RetrySubscriber");
    } catch (ClassNotFoundException exception) {
      return null;
    }
  }

  private static Class<?> getFluxRetryWhenSubscriberClass() {
    try {
      return Class.forName("reactor.core.publisher.FluxRetryWhen$RetryWhenMainSubscriber");
    } catch (ClassNotFoundException exception) {
      return null;
    }
  }
}
