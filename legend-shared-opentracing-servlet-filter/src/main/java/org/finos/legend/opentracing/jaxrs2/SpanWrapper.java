// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.opentracing.jaxrs2;

import io.opentracing.Scope;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replaces io.opentracing.contrib.jaxrs2.internal.SpanWrapper which was
 * removed along with the opentracing-jaxrs2 dependency.
 */
public class SpanWrapper
{
    public static final String PROPERTY_NAME = SpanWrapper.class.getName() + ".activeSpanWrapper";

    private final Span span;
    private final Scope scope;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public SpanWrapper(Span span, Scope scope)
    {
        this.span = span;
        this.scope = scope;
    }

    public Span get()
    {
        return span;
    }

    public Scope getScope()
    {
        return scope;
    }

    public boolean isFinished()
    {
        return finished.get();
    }

    public void finish()
    {
        if (finished.compareAndSet(false, true))
        {
            span.finish();
        }
    }
}
