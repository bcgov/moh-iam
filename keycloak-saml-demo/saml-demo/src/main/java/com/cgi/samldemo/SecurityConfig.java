package com.cgi.samldemo;

import org.opensaml.security.x509.X509Support;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${saml2.ap.metadata.location}")
    private String metadataLocation;

    @Value("${saml2.rp.signing.cert-location}")
    private String rpSigningCertLocation;

    @Value("${saml2.rp.signing.key-location}")
    private String rpSigningKeyLocation;

    @Value("${saml2.ap.signing-cert}")
    private String apCertificate;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/").authenticated()
                        .requestMatchers("**").authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/")
                )
                .saml2Login(withDefaults())
                .saml2Logout(withDefaults())
        ;
        return http.build();
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrations() throws Exception {
        Resource signingCertResource = new ClassPathResource(this.rpSigningCertLocation);
        Resource signingKeyResource = new ClassPathResource(this.rpSigningKeyLocation);
        try (
                InputStream is = signingKeyResource.getInputStream();
                InputStream certIS = signingCertResource.getInputStream();
        ) {
            X509Certificate rpCertificate = X509Support.decodeCertificate(certIS.readAllBytes());
            RSAPrivateKey rpKey = RsaKeyConverters.pkcs8().convert(is);
            final Saml2X509Credential rpSigningCredentials = Saml2X509Credential.signing(rpKey, rpCertificate);

            X509Certificate apCert = X509Support.decodeCertificate(apCertificate);
            Saml2X509Credential apCredential = Saml2X509Credential.verification(apCert);

            RelyingPartyRegistration registration = RelyingPartyRegistrations
                    .fromMetadataLocation(metadataLocation)
                    .entityId("saml-demo")
                    .registrationId("saml-demo")
                    .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo")
                    .signingX509Credentials(c -> c.add(rpSigningCredentials))
                    .assertingPartyDetails(party -> party
                            .wantAuthnRequestsSigned(true)
                            .verificationX509Credentials(c -> c.add(apCredential))
                    )
                    .build();
            return new InMemoryRelyingPartyRegistrationRepository(registration);
        }
    }
}