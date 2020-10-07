package ca.bc.gov.health.security;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * Request scoped bean with a method to log the user out of their current
 * Keycloak and application sessions
 */
@RequestScoped
@Named("LogoutBean")
public class LogoutBean implements Serializable {

    /* Invalidate the Payara session and logout of Keycloak */
    public void logout() throws UnsupportedEncodingException, IOException {

        // Call the keycloak realm end-session endpoint to delete the session from keycloak
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("https://localhost:8543/auth/realms/moh-users-realm/protocol/openid-connect/logout?redirect_uri="
                        + URLEncoder.encode("https://localhost:8181/keycloak-javaee-example/callback", "UTF-8"));

        // Invalidate the user browser session
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
    }

    /* Invalidate the Payara session, logout of Keycloak and SiteMinder (for IDIR and BCeID) */
    public void logoutKcAndSiteMinder() throws IOException {

        /* Currently Keycloak does not support logging out of SiteMinder IDP's automatically
        so we set the Keycloak Logout redirect_uri= paramter to be the SiteMinder logout
        and we set the Siteminder returl= parameter to be application which chains both logouts for full Single Sign Out.
        https://github.com/bcgov/ocp-sso/issues/4
        
        The production siteminder url is different in production (logon7.gov.bc.ca) so this value should be configurable.
        */
        
        String localAppUrl = "https://localhost:8181/keycloak-javaee-example/callback";
        String siteMinderLogoutUrl = "https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi" + "?retnow=1&returl=" + localAppUrl;
        String keycloakLogoutUrl = "https://localhost:8543/auth/realms/moh-users-realm/protocol/openid-connect/logout?redirect_uri=" + URLEncoder.encode(siteMinderLogoutUrl, "UTF-8");

        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect(keycloakLogoutUrl);
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
    }
}
