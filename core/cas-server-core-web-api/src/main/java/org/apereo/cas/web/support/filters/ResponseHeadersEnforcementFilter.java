package org.apereo.cas.web.support.filters;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * Allows users to easily inject the default security headers to assist in protecting the application.
 * The default for is to include the following headers:
 * <pre>
 * Cache-Control: no-cache, no-store, max-age=0, must-revalidate
 * Pragma: no-cache
 * Expires: 0
 * X-Content-Type-Options: nosniff
 * Strict-Transport-Security: max-age=15768000 ; includeSubDomains
 * X-Frame-Options: DENY
 * X-XSS-Protection: 1; mode=block
 * </pre>
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@Setter
@Getter
public class ResponseHeadersEnforcementFilter extends AbstractSecurityFilter implements Filter {
    /**
     * Enable CACHE_CONTROL.
     */
    public static final String INIT_PARAM_ENABLE_CACHE_CONTROL = "enableCacheControl";
    /**
     * Enable XCONTENT_OPTIONS.
     */
    public static final String INIT_PARAM_ENABLE_XCONTENT_OPTIONS = "enableXContentTypeOptions";
    /**
     * Enable STRICT_TRANSPORT_SECURITY.
     */
    public static final String INIT_PARAM_ENABLE_STRICT_TRANSPORT_SECURITY = "enableStrictTransportSecurity";

    /**
     * Enable STRICT_XFRAME_OPTIONS.
     */
    public static final String INIT_PARAM_ENABLE_STRICT_XFRAME_OPTIONS = "enableXFrameOptions";
    /**
     * The constant INIT_PARAM_STRICT_XFRAME_OPTIONS.
     */
    public static final String INIT_PARAM_STRICT_XFRAME_OPTIONS = "XFrameOptions";

    /**
     * Enable XSS_PROTECTION.
     */
    public static final String INIT_PARAM_ENABLE_XSS_PROTECTION = "enableXSSProtection";
    /**
     * XSS protection value.
     */
    public static final String INIT_PARAM_XSS_PROTECTION = "XSSProtection";

    /**
     * Consent security policy.
     */
    public static final String INIT_PARAM_CONTENT_SECURITY_POLICY = "contentSecurityPolicy";

    private boolean enableCacheControl;

    private String cacheControlHeader = "no-cache, no-store, max-age=0, must-revalidate";

    private boolean enableXContentTypeOptions;

    private String xContentTypeOptionsHeader = "nosniff";

    private boolean enableStrictTransportSecurity;

    /**
     * Allow for 6 months; value is in seconds.
     */
    private String strictTransportSecurityHeader = "max-age=15768000 ; includeSubDomains";

    private boolean enableXFrameOptions;
    private String xframeOptions = "DENY";

    private boolean enableXSSProtection;

    private String xssProtection = "1; mode=block";

    private String contentSecurityPolicy;

    /**
     * Examines the Filter init parameter names and throws ServletException if they contain an unrecognized
     * init parameter name.
     * <p>
     * This is a stateless static method.
     * <p>
     * This method is an implementation detail and is not exposed API.
     * This method is only non-private to allow JUnit testing.
     *
     * @param initParamNames init param names, in practice as read from the FilterConfig.
     */
    private static void throwIfUnrecognizedParamName(final Enumeration initParamNames) {
        val recognizedParameterNames = new HashSet<String>();
        recognizedParameterNames.add(INIT_PARAM_ENABLE_CACHE_CONTROL);
        recognizedParameterNames.add(INIT_PARAM_ENABLE_XCONTENT_OPTIONS);
        recognizedParameterNames.add(INIT_PARAM_ENABLE_STRICT_TRANSPORT_SECURITY);
        recognizedParameterNames.add(INIT_PARAM_ENABLE_STRICT_XFRAME_OPTIONS);
        recognizedParameterNames.add(INIT_PARAM_STRICT_XFRAME_OPTIONS);
        recognizedParameterNames.add(INIT_PARAM_CONTENT_SECURITY_POLICY);
        recognizedParameterNames.add(INIT_PARAM_ENABLE_XSS_PROTECTION);
        recognizedParameterNames.add(INIT_PARAM_XSS_PROTECTION);

        while (initParamNames.hasMoreElements()) {
            val initParamName = (String) initParamNames.nextElement();
            if (!recognizedParameterNames.contains(initParamName)) {
                logException(new ServletException("Unrecognized init parameter [" + initParamName + ']'));
            }
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        val failSafeParam = filterConfig.getInitParameter(FAIL_SAFE);
        if (null != failSafeParam) {
            throwOnErrors = Boolean.parseBoolean(failSafeParam);
        }

        val initParamNames = filterConfig.getInitParameterNames();
        throwIfUnrecognizedParamName(initParamNames);

        val enableCacheControl = filterConfig.getInitParameter(INIT_PARAM_ENABLE_CACHE_CONTROL);
        val enableXContentTypeOptions = filterConfig.getInitParameter(INIT_PARAM_ENABLE_XCONTENT_OPTIONS);
        val enableStrictTransportSecurity = filterConfig.getInitParameter(INIT_PARAM_ENABLE_STRICT_TRANSPORT_SECURITY);
        val enableXFrameOptions = filterConfig.getInitParameter(INIT_PARAM_ENABLE_STRICT_XFRAME_OPTIONS);
        val enableXSSProtection = filterConfig.getInitParameter(INIT_PARAM_ENABLE_XSS_PROTECTION);

        try {
            this.enableCacheControl = Boolean.parseBoolean(enableCacheControl);
        } catch (final Exception e) {
            logException(new ServletException("Error parsing parameter [" + INIT_PARAM_ENABLE_CACHE_CONTROL
                + "] with value [" + enableCacheControl + ']', e));
        }

        try {
            this.enableXContentTypeOptions = Boolean.parseBoolean(enableXContentTypeOptions);
        } catch (final Exception e) {
            logException(new ServletException("Error parsing parameter [" + INIT_PARAM_ENABLE_XCONTENT_OPTIONS
                + "] with value [" + enableXContentTypeOptions + ']', e));
        }

        try {
            this.enableStrictTransportSecurity = Boolean.parseBoolean(enableStrictTransportSecurity);
        } catch (final Exception e) {
            logException(new ServletException("Error parsing parameter [" + INIT_PARAM_ENABLE_STRICT_TRANSPORT_SECURITY
                + "] with value [" + enableStrictTransportSecurity + ']', e));
        }

        try {
            this.enableXFrameOptions = Boolean.parseBoolean(enableXFrameOptions);
            this.xframeOptions = filterConfig.getInitParameter(INIT_PARAM_STRICT_XFRAME_OPTIONS);
            if (this.xframeOptions == null || this.xframeOptions.isEmpty()) {
                this.xframeOptions = "DENY";
            }
        } catch (final Exception e) {
            logException(new ServletException("Error parsing parameter [" + INIT_PARAM_ENABLE_STRICT_XFRAME_OPTIONS
                + "] with value [" + enableXFrameOptions + ']', e));
        }

        try {
            this.enableXSSProtection = Boolean.parseBoolean(enableXSSProtection);
            this.xssProtection = filterConfig.getInitParameter(INIT_PARAM_XSS_PROTECTION);
            if (this.xssProtection == null || this.xssProtection.isEmpty()) {
                this.xssProtection = "1; mode=block";
            }
        } catch (final Exception e) {
            logException(new ServletException("Error parsing parameter [" + INIT_PARAM_ENABLE_XSS_PROTECTION
                + "] with value [" + enableXSSProtection + ']', e));
        }

        this.contentSecurityPolicy = filterConfig.getInitParameter(INIT_PARAM_CONTENT_SECURITY_POLICY);
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        try {
            if (servletResponse instanceof HttpServletResponse) {
                val httpServletResponse = (HttpServletResponse) servletResponse;
                val httpServletRequest = (HttpServletRequest) servletRequest;

                decideInsertCacheControlHeader(httpServletResponse, httpServletRequest);
                decideInsertStrictTransportSecurityHeader(httpServletResponse, httpServletRequest);
                decideInsertXContentTypeOptionsHeader(httpServletResponse, httpServletRequest);
                decideInsertXFrameOptionsHeader(httpServletResponse, httpServletRequest);
                decideInsertXSSProtectionHeader(httpServletResponse, httpServletRequest);
                decideInsertContentSecurityPolicyHeader(httpServletResponse, httpServletRequest);
            }
        } catch (final Exception e) {
            logException(new ServletException(getClass().getSimpleName()
                + " is blocking this request. Examine the cause in this stack trace to understand why.", e));
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    protected void decideInsertContentSecurityPolicyHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (this.contentSecurityPolicy == null) {
            return;
        }
        insertContentSecurityPolicyHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertContentSecurityPolicyHeader(final HttpServletResponse httpServletResponse,
                                                     final HttpServletRequest httpServletRequest) {
        this.insertContentSecurityPolicyHeader(httpServletResponse, httpServletRequest, this.contentSecurityPolicy);
    }

    protected void insertContentSecurityPolicyHeader(final HttpServletResponse httpServletResponse,
                                                     final HttpServletRequest httpServletRequest,
                                                     final String contentSecurityPolicy) {
        val uri = httpServletRequest.getRequestURI();
        httpServletResponse.addHeader("Content-Security-Policy", contentSecurityPolicy);
        LOGGER.trace("Adding Content-Security-Policy response header [{}] for [{}]", contentSecurityPolicy, uri);
    }

    protected void decideInsertXSSProtectionHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (!this.enableXSSProtection) {
            return;
        }
        insertXSSProtectionHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertXSSProtectionHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        insertXSSProtectionHeader(httpServletResponse, httpServletRequest, this.xssProtection);
    }

    protected void insertXSSProtectionHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest,
                                             final String value) {
        val uri = httpServletRequest.getRequestURI();
        httpServletResponse.addHeader("X-XSS-Protection", value);
        LOGGER.trace("Adding X-XSS Protection [{}] response headers for [{}]", value, uri);
    }

    protected void decideInsertXFrameOptionsHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (!this.enableXFrameOptions) {
            return;
        }
        insertXFrameOptionsHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertXFrameOptionsHeader(final HttpServletResponse httpServletResponse,
                                             final HttpServletRequest httpServletRequest) {
        insertXFrameOptionsHeader(httpServletResponse, httpServletRequest, this.xframeOptions);
    }

    protected void insertXFrameOptionsHeader(final HttpServletResponse httpServletResponse,
                                             final HttpServletRequest httpServletRequest,
                                             final String value) {
        val uri = httpServletRequest.getRequestURI();
        httpServletResponse.addHeader("X-Frame-Options", value);
        LOGGER.trace("Adding X-Frame Options [{}] response headers for [{}]", value, uri);
    }

    protected void decideInsertXContentTypeOptionsHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (!this.enableXContentTypeOptions) {
            return;
        }
        insertXContentTypeOptionsHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertXContentTypeOptionsHeader(final HttpServletResponse httpServletResponse,
                                                   final HttpServletRequest httpServletRequest) {
        insertXContentTypeOptionsHeader(httpServletResponse, httpServletRequest, this.xContentTypeOptionsHeader);
    }

    protected void insertXContentTypeOptionsHeader(final HttpServletResponse httpServletResponse,
                                                   final HttpServletRequest httpServletRequest,
                                                   final String value) {
        val uri = httpServletRequest.getRequestURI();
        httpServletResponse.addHeader("X-Content-Type-Options", value);
        LOGGER.trace("Adding X-Content Type response headers [{}] for [{}]", value, uri);
    }

    protected void decideInsertCacheControlHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (!this.enableCacheControl) {
            return;
        }
        insertCacheControlHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertCacheControlHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        insertCacheControlHeader(httpServletResponse, httpServletRequest, this.cacheControlHeader);
    }

    protected void insertCacheControlHeader(final HttpServletResponse httpServletResponse,
                                            final HttpServletRequest httpServletRequest,
                                            final String value) {

        val uri = httpServletRequest.getRequestURI();
        if (!uri.endsWith(".css")
            && !uri.endsWith(".js")
            && !uri.endsWith(".png")
            && !uri.endsWith(".txt")
            && !uri.endsWith(".jpg")
            && !uri.endsWith(".ico")
            && !uri.endsWith(".jpeg")
            && !uri.endsWith(".bmp")
            && !uri.endsWith(".gif")) {
            httpServletResponse.addHeader("Cache-Control", value);
            httpServletResponse.addHeader("Pragma", "no-cache");
            httpServletResponse.addIntHeader("Expires", 0);
            LOGGER.trace("Adding Cache Control response headers for [{}}", uri);
        }
    }

    protected void decideInsertStrictTransportSecurityHeader(final HttpServletResponse httpServletResponse, final HttpServletRequest httpServletRequest) {
        if (!this.enableStrictTransportSecurity) {
            return;
        }
        insertStrictTransportSecurityHeader(httpServletResponse, httpServletRequest);
    }

    protected void insertStrictTransportSecurityHeader(final HttpServletResponse httpServletResponse,
                                                       final HttpServletRequest httpServletRequest) {
        insertStrictTransportSecurityHeader(httpServletResponse, httpServletRequest, this.strictTransportSecurityHeader);
    }

    protected void insertStrictTransportSecurityHeader(final HttpServletResponse httpServletResponse,
                                                       final HttpServletRequest httpServletRequest,
                                                       final String strictTransportSecurityHeader) {
        if (httpServletRequest.isSecure()) {
            val uri = httpServletRequest.getRequestURI();

            httpServletResponse.addHeader("Strict-Transport-Security", strictTransportSecurityHeader);
            LOGGER.trace("Adding HSTS response headers for [{}]", uri);
        }
    }

    @Override
    public void destroy() {
    }
}
