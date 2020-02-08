package ca.bc.gov.health.security;

import fish.payara.security.annotations.ClaimsDefinition;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;

/**
 * Configures an OIDC Client.
 *
 * You can find the Provider URI (also called “auth-server-uri”), client ID
 * (also called "resource"), and client secret on the Keycloak Admin Console at
 * Clients: [Your Client]: Installation: [Any Format].
 *
 * The "callerGroupsClaim" must match the value you set for the User Client Role
 * mapping when you created the client in Keycloak.
 */
@OpenIdAuthenticationDefinition(
        providerURI = "https://localhost:8444/auth/realms/moh-users-realm/",
        clientId = "KeycloakIntegrationExample",
        clientSecret = "b09d7655-6315-44ae-b739-d32289de5b96",
        redirectURI = "https://localhost:8181/KeycloakIntegrationExample/callback",
        scope = {"openid"},
        claimsDefinition = @ClaimsDefinition(callerGroupsClaim = "fmdb_role")
)
public class KeycloakSecurityBean {
    /*
     * This class really can be blank as the annotation doesn't affect the
     * behaviour of the class itself. In fact, you should be able to apply this
     * annotation to any class in the project, but it's probably clearest to
     * annotate an empty class.
     */
}
