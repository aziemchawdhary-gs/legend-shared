// Forked from
//      https://github.com/opentracing-contrib/java-jaxrs/blob/master/opentracing-jaxrs2/src/main/java/io/opentracing/contrib/jaxrs2/server/ServerTracingInterceptor.java
//
// This includes enhancements requested on issue:
//      https://github.com/opentracing-contrib/java-jaxrs/issues/147
//
// PR with contribution for solving issue:
//      https://github.com/opentracing-contrib/java-jaxrs/pull/148
//
// Once issue is solved, we should update artifact version and delete this fork

package org.finos.legend.opentracing.jaxrs2;

import io.opentracing.Tracer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.InterceptorContext;
import java.util.List;

public class ServerTracingInterceptor extends TracingInterceptor
{
    /**
     * Apache CFX does not seem to publish the PROPERTY_NAME into the Interceptor context.
     * Use the current HttpServletRequest to lookup the current span wrapper.
     */
    @Context
    private HttpServletRequest servletReq;

    public ServerTracingInterceptor(Tracer tracer, List<InterceptorSpanDecorator> spanDecorators)
    {
        super(tracer, spanDecorators);
    }

    @Override
    protected SpanWrapper findSpan(InterceptorContext context)
    {
        Object prop = context.getProperty(SpanWrapper.PROPERTY_NAME);
        SpanWrapper spanWrapper = prop instanceof SpanWrapper ? (SpanWrapper) prop : null;
        if (spanWrapper == null && servletReq != null)
        {
            Object attr = servletReq.getAttribute(SpanWrapper.PROPERTY_NAME);
            spanWrapper = attr instanceof SpanWrapper ? (SpanWrapper) attr : null;
        }
        return spanWrapper;
    }
}