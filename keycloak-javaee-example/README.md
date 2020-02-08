This example shows how to add the Payara OpenID Connect adapter to an application.

Prerequisites:

1. Keycloak.
2. An application "Client" needs to be configured in Keycloak, as [described in the wiki](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Jakarta-EE-application-deployed-to-Payara-with-Keycloak).

To secure a Jakarta EE application deployed to Payara with Keycloak you need two things:

1. A class annotated with @OpenIdAuthenticationDefinition, as shown in [KeycloakSecurityBean.java](https://github.com/bcgov/moh-iam/blob/master/keycloak-javaee-example/src/main/java/ca/bc/gov/health/security/KeycloakSecurityBean.java).
2. An unsecured HTTP GET resource, as shown in [CallbackServlet.java](https://github.com/bcgov/moh-iam/blob/master/keycloak-javaee-example/src/main/java/ca/bc/gov/health/security/CallbackServlet.java). This could be an unsecured page, a Servlet, a JAX-RS endpoint, etc.
