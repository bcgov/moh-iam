# Overview
This directory consists of two theme's which are used to style the **moh_idp** and **moh_applications** Keycloak realms. For further instructions regarding creating or updating themes see the reference link.

## moh-app-realm
This theme has been designed to only show a list of identity providers. 

The top level heading (Ministry of Health) is set by the Realm Display Name which can be configured in Keycloak's Admin console

The second level heading (application name) is pulled from the Client ID

The list of identity providers is dynamically configured by setting url query parameters. By default no IDP's will show in the list. In order to show an IDP the client application should include `idps_to_show=idp-alias` as a query parameter in the user browser redirect to Keycloak.

## moh-idp
This theme serves as a login page for the moh_idp realm.  

The top level heading (Ministry of Health) is set by the Realm Display Name which can be configured in Keycloak's Admin console

The second level heading is set in the messages.properties file in the theme

# How to deploy

In order to add the themes to a Keycloak installation: 
* copy the root directory of a theme to the themes directory in the keycloak installation.
* restart Keycloak

The theme will be available for selection in the Keycloak Admin Console under:
Realm Settings -> Themes -> Login Theme and/or Client -> Client Name -> Settings -> Login Theme

# Tested on
* Keycloak 8.0.1

# References
* Keycloak Documentation: [Creating, Updating, and Configuring Themes](https://www.keycloak.org/docs/latest/server_development/#_themes).