# Overview
This directory consists of two themes which are used to style the **moh_idp** and **moh_applications** Keycloak realms. These themes are based on Keycloak 9.0 and may need to be updated for use with different versions of Keycloak. For further instructions regarding creating or updating themes see the reference link.

## moh-app-realm
This theme package contains themes for both the Login and Account pages. The Login theme has been designed to only show a list of identity providers. The Account page removes the options for users to view and manage password, authentication, and applications. 

In both cases the CSS has also been updated to better align with Ministry of Health Standards.

### Login Theme Notes
The top level heading (Ministry of Health) is set by the Realm Display Name which can be configured in the Keycloak Admin console.

The second level heading (application name) is pulled from the Client Name which can be configured in the Keycloak Admin Console.

The identity providers shown are dynamically configured by setting url query parameters. By default all IDPs will show in the list. In order to only show specific IDPs, the user browser redirect from the client application should include `idps_to_show=` as a query parameter with a comma seperated of idp-alias names as values. (e.g. `idps_to_show=moh_idp,moh_collector_idir` will only show LDAP and IDIR)

When this parameter is set any idp-alias not in the list will be hidden.

### Account Theme Notes
The top level header is set in the template.ftl template file. 

## moh-idp
This theme package contains a theme for the moh-idp realm Login page.

The top level heading (Ministry of Health) is set by the Realm Display Name which can be configured in Keycloak's Admin console.

The second level heading is set in the messages.properties file in the theme.

# How to deploy

In order to add the themes to a Keycloak installation: 

1. Copy the root directory of a theme to the themes directory in the Keycloak installation.
2. Restart Keycloak.

The theme will be available for selection in the Keycloak Admin Console.

The settings to apply the account theme, or a login theme for the whole realm, are found at:
`Realm Settings -> Themes -> Login Theme or Account Theme`

The settings to apply the the login theme only to a specific client are found at:
`Client -> Client Name -> Settings -> Login Theme.`

# Tested on
* Keycloak 9.0.2

# References
* Keycloak Documentation: [Creating, Updating, and Configuring Themes](https://www.keycloak.org/docs/latest/server_development/#_themes).
