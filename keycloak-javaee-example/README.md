[![Build Status](https://travis-ci.org/bcgov/moh-iam.svg?branch=master)](https://travis-ci.org/bcgov/moh-iam)

This example shows how to add the Payara OpenID Connect adapter to an application. This example is a part of the guide [How to secure a Jakarta EE application deployed to Payara with Keycloak](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Jakarta-EE-application-deployed-to-Payara-with-Keycloak).

# Prerequisites

1. Keycloak needs to be installed and running somewhere.
2. An application "Client" needs to be configured in Keycloak, as [described in the wiki](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Jakarta-EE-application-deployed-to-Payara-with-Keycloak).

# How to deploy

After the prequisites are met, run `mvn package` and deploy the resulting WAR file to Payara.

# Overview

To secure a Jakarta EE application deployed to Payara with Keycloak you need two things:

1. A class annotated with @OpenIdAuthenticationDefinition, as shown in [KeycloakSecurityBean.java](https://github.com/bcgov/moh-iam/blob/master/keycloak-javaee-example/src/main/java/ca/bc/gov/health/security/KeycloakSecurityBean.java).
2. An unsecured HTTP GET resource, as shown in [CallbackServlet.java](https://github.com/bcgov/moh-iam/blob/master/keycloak-javaee-example/src/main/java/ca/bc/gov/health/security/CallbackServlet.java). This could be an unsecured page, a Servlet, a JAX-RS endpoint, etc.

# Tested on

* Payara 5.192 with Oracle JDK 8, jdk1.8.0_181 
* Payara 5.2020.4 with Adopt OpenJDK 11.0.5
* Windows 10
* Keycloak 8.0.1
