package ca.bc.gov.health.security;

import fish.payara.security.annotations.ClaimsDefinition;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;

/**
 * Configures an OIDC Client.
 */
@OpenIdAuthenticationDefinition(
        // All configuration properties can be found on the Keycloak Admin Console        
        // providerUri: See Clients > [Your Client] > Installation > [Any Format] 
        // to get the base auth-server-url, then append /realms/[your-realm]/
        providerURI = "http://localhost:8081/auth/realms/moh-users-realm/",
        // clientId: See Clients for the clientId
        clientId = "KeycloakIntegrationExample",
        // clientSecret: See Clients > [Your Client] > Credentials tab
        // In this example we use a Payara password alias.
        clientSecret = "${ALIAS=keycloak-client-secret}",
        // redirectUri: This must be an application page, but it must also be a
        // "valid redirect URI", see Clients > [Your Client] > Valid Redirect
        // URIs field
        redirectURI = "https://localhost:8181/keycloak-javaee-example/callback",
        // scope: Must openid.
        scope = {"openid"},
        // Value must match the "User Client Role" mapper specified in Clients >
        // [Your Client] > Mappers > [the User Client Role mapper].
        claimsDefinition = @ClaimsDefinition(callerGroupsClaim = "groups")
)
public class KeycloakSecurityBean {
    /*
     * This class really can be blank as the annotation doesn't affect the
     * behaviour of the class itself. In fact, you should be able to apply this
     * annotation to any class in the project, but it's probably clearest to
     * annotate an empty class.
     */
}
