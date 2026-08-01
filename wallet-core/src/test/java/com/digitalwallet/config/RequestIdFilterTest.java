package com.digitalwallet.config;

import com.digitalwallet.common.request.RequestIds;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void requestIdsGet_usesValidHeaderWhenAttributeIsMissing() {
        String requestId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, requestId);

        assertThat(RequestIds.get(request)).isEqualTo(requestId);
        assertThat(request.getAttribute(RequestIds.ATTRIBUTE)).isEqualTo(requestId);
    }

    @Test
    void requestIdsGet_ignoresInvalidAttributeAndFallsBackToValidHeader() {
        String requestId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestIds.ATTRIBUTE, "not-a-uuid");
        request.addHeader(RequestIds.HEADER, requestId);

        assertThat(RequestIds.get(request)).isEqualTo(requestId);
        assertThat(request.getAttribute(RequestIds.ATTRIBUTE)).isEqualTo(requestId);
    }

    @Test
    void requestIdFilter_runsAtHighestPrecedence() {
        Order order = RequestIdFilter.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void doFilterInternal_usesIncomingValidRequestId() throws Exception {
        String requestId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(RequestIds.ATTRIBUTE)).isEqualTo(requestId);
        assertThat(response.getHeader(RequestIds.HEADER)).isEqualTo(requestId);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_replacesInvalidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String requestId = response.getHeader(RequestIds.HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(UUID.fromString(requestId)).isNotNull();
        assertThat(request.getAttribute(RequestIds.ATTRIBUTE)).isEqualTo(requestId);
        verify(chain).doFilter(request, response);
    }
}