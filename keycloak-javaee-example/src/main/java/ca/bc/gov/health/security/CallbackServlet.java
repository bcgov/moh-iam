package ca.bc.gov.health.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("index.xhtml");
    }

}
