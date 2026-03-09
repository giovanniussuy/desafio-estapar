package com.estapar.parking.interfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void shouldGenerateTraceIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
            assertNotNull(traceId);
            assertEquals(traceId, request.getAttribute(TraceIdFilter.TRACE_ID_KEY));
        });

        assertNotNull(response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }

    @Test
    void shouldReuseHeaderTraceIdWhenProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-from-header");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
            assertEquals("trace-from-header", MDC.get(TraceIdFilter.TRACE_ID_KEY))
        );

        assertEquals("trace-from-header", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }

    @Test
    void shouldGenerateTraceIdWhenHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
            assertNotNull(traceId);
            assertEquals(traceId, request.getAttribute(TraceIdFilter.TRACE_ID_KEY));
        });

        String header = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(header);
        assertEquals(36, header.length());
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }

    @Test
    void shouldClearMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-with-error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }
}
