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
        providerURI = "https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/",
        // clientId: See Clients for the clientId
        clientId = "FMDB",
        // clientSecret: See Clients > [Your Client] > Credentials tab
        // In this example we use a Payara password alias.
        clientSecret = "${ALIAS=keycloak-client-secret}",
        // redirectUri: This must be an application page, but it must also be a
        // "valid redirect URI", see Clients > [Your Client] > Valid Redirect
        // URIs field
        redirectURI = "http://localhost:8080/KeycloakIntegrationExample/callback",
        // scope: Must openid.
        scope = {"openid"},
        // Value must match the "User Client Role" mapper specified in Clients >
        // [Your Client] > Mappers > [the User Client Role mapper].
        claimsDefinition = @ClaimsDefinition( callerNameClaim="preferred_username", callerGroupsClaim="fmdb_role" ),
        //Extra parameters added to the request url in the user browser
        //Examples:
        //Keycloak specific: kc_idp_hint - directs users to a specific idp login
        //MoH Keycloak specific: idps_to_show - defines the list of idps to show on the common logon page
        extraParameters = {
            "idps_to_show=all"
        }
)
public class KeycloakSecurityBean {
    /*
     * This class really can be blank as the annotation doesn't affect the
     * behaviour of the class itself. In fact, you should be able to apply this
     * annotation to any class in the project, but it's probably clearest to
     * annotate an empty class.
     */
}
