package ca.bc.gov.health.security;

import fish.payara.security.openid.api.OpenIdContext;

import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

/**
 * This is an example unsecured HTTP resource to be used as an OpenID
 * Connect redirect_uri, as configured on your OpenID Provider (e.g. Keycloak)
 * and in the adapter, in this case {@link KeycloakSecurityBean}.
 *
 * Note that you may not need this Servlet. I think it's only needed if you
 * don't already have some unsecured resource in your application because the
 * adapter seems to require an unsecured channel to exchange information. (By
 * "unsecured" I mean no authentication is required, not that it doesn't use
 * https.)
 */
@WebServlet("/callback")
public class CallbackServlet extends HttpServlet {

    @Inject
    OpenIdContext context;

    @Inject
    SecurityContext securityContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // If you want to inspect the tokens, UNCOMMENT this block, and COMMENT OUT the redirect.
//        try {
//            debugOIDC(request, response);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        response.sendRedirect("secure.xhtml");

    }

    private void debugOIDC(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Principal userPrincipal = request.getUserPrincipal();

        PrintWriter out = response.getWriter();

        out.println("\nprincipal: " + userPrincipal.toString());
        out.println("\nprincipal name: " + userPrincipal.getName());

        out.println("\ncaller name: " + context.getCallerName());
        out.println("\ncaller groups: " + context.getCallerGroups());
        out.println("\nsubject: " + context.getSubject());
        out.println("\naccess token: " + context.getAccessToken());
        out.println("\nidentity token: " + context.getIdentityToken());
        out.println("\nuser claims: " + context.getClaimsJson());

        out.println("\ncaller principal name: " + securityContext.getCallerPrincipal().getName());
        out.println("\nisCallerInRole? PSDADMIN: " + securityContext.isCallerInRole("PSDADMIN"));
        out.println("\nisCallerInRole? FAKEROLE: " + securityContext.isCallerInRole("FAKEROLE"));
        out.println("\nisCallerInRole? view-profile: " + securityContext.isCallerInRole("view-profile"));
        out.println("\nisCallerInRole? manage-account-links: " + securityContext.isCallerInRole("manage-account-links"));
    }

}
