# Overview
This directory consists of three themes which are used to style the **moh_idp** and **moh_applications** Keycloak realms. These themes are based on Keycloak 9.0 and may need to be updated for use with different versions of Keycloak (themes are still fine as of Keycloak 18.0.2). For further instructions regarding creating or updating themes see the reference link.

## moh-app-realm
This theme package contains themes for both the Login and Account pages. The Login theme has been designed to only show a list of identity providers and explicitly excludes any IDPs that start with "bcsc". The Account page removes the options for users to view and manage password, authentication, and applications. 

In both cases the CSS has also been updated to better align with Ministry of Health Standards.

### Login Theme Notes
The top level heading (Ministry of Health) is set by the Realm HTML Display Name which can be configured in the Keycloak Admin console.

The second level heading (application name) is pulled from the Client Name which can be configured in the Keycloak Admin Console.

The identity providers shown are dynamically configured by setting url query parameters. By default, all IDPs will show in the list, except for those that start with "bcsc". In order to only show specific IDPs, the user browser redirect from the client application should include `idps_to_show=` as a query parameter with a comma seperated of idp-alias names as values (e.g. `idps_to_show=moh_idp,idir` will only show Keycloak and IDIR).

When this parameter is set any idp-alias not in the list will be hidden.

### Account Theme Notes
The top level header is set in the template.ftl template file. 

## moh-app-realm-bcsc-idp
This theme package is a copy of the [moh-app-realm](https://github.com/bcgov/moh-iam/tree/master/keycloak-themes#moh-app-realm) theme above with the one change being that it does not explicitly exclude any IDPs that start with "bcsc". It can be applied on a per client basis for those clients that require BCSC integration but do not have a custom login page (e.g. HCIMWeb).

## moh-idp
This theme package contains a theme for the moh-idp realm Login page.

The top level heading (Ministry of Health) is set by the Realm HTML Display Name which can be configured in Keycloak's Admin console.

The second level heading is set in the messages.properties file in the theme.

# How to Deploy

In order to add the themes to a Keycloak installation: 

1. Copy the root directory of a theme to the themes directory in the Keycloak installation.
2. Restart Keycloak.

The theme will be available for selection in the Keycloak Admin Console.

The settings to apply the account theme, or a login theme for the whole realm, are found at:
`Realm Settings -> Themes -> Login Theme or Account Theme`

The settings to apply the the login theme only to a specific client are found at:
`Client -> Client Name -> Settings -> Login Theme.`

## Clearing Theme Cache
During a deploy, some theme resource files such as scripts may remain cached after restarting Keycloak. This cache can be cleared with the following:

As the `gfadmin` user, remove the cache folder for a specific theme found under `/data/gfadmin/software/keycloak/domain/servers/server-<one|two>/tmp/kc-gzip-cache/?????/` (where `?????` can be found from inspecting a theme resource file from your browser's **Developer Tools -> Network** tab).
> **Note:** Dev `?????=m465f`, Test `?????=4nox8`, and Prod `?????=jo96m` but this could change in the future.

e.g. To clear the cache for the moh-app-realm login theme in Keycloak Dev, run the following command as `gfadmin`:
```
rm -rf /data/gfadmin/software/keycloak/domain/servers/server-one/tmp/kc-gzip-cache/m465f/login/moh-app-realm/
```

# IDP Outage notification
In case of a rare event of an IDP outage, the login page can be edited, to inform users about the issue. To display the outage notification, a developer needs to append the word `outage` to the Display Name attribute of the damaged Identity Provider in the corresponding realm. For example, if IDIR IDP is not working, changing Display Name from `IDIR` to `IDIR outage` in the `moh_applications` realm will result in displaying the following message: `Login with IDIR is currently unavailable. IDP team is working on restoring the service.` No server restart is needed. To delete the notification, the Display Name must be set to the previous value.

# Tested on
* Keycloak 9.0.2, 15.0.2, and 18.0.2

# References
* Keycloak Documentation: [Creating, Updating, and Configuring Themes](https://www.keycloak.org/docs/latest/server_development/#_themes).
