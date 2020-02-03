package ca.bc.gov.health.security;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;

@OpenIdAuthenticationDefinition(
       providerURI = "https://localhost:8444/auth/realms/moh-users-realm/",
       clientId = "KeycloakIntegrationExample",
       clientSecret = "b09d7655-6315-44ae-b739-d32289de5b96",
       redirectURI = "https://localhost:8181/KeycloakIntegrationExample/callback",
       scope = { "openid" }

)
public class KeycloakSecurityBean {
    
}
