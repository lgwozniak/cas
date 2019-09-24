package org.apereo.cas.adaptors.x509.authentication.principal;

import org.apereo.cas.AbstractCentralAuthenticationServiceTests;
import org.apereo.cas.adaptors.x509.authentication.CasX509Certificate;
import org.apereo.cas.config.CasCoreConfiguration;

import lombok.ToString;
import org.springframework.context.annotation.Import;

/**
 * @author Marvin S. Addison
 * @since 3.0.0
 */
@ToString
@Import(CasCoreConfiguration.class)
public abstract class AbstractX509CertificateTests extends AbstractCentralAuthenticationServiceTests {
    public static final CasX509Certificate VALID_CERTIFICATE = new CasX509Certificate(true);
}
