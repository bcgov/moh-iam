A customized copy of the [Keycloak OpenID Connect](https://www.drupal.org/project/keycloak)
Drupal module (version `8.x-1.3`). Adds role mapping.

# Prerequisites

1. Keycloak needs to be installed and running somewhere.
2. An application "Client" needs to be configured in Keycloak, as [described in the wiki](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Jakarta-EE-application-deployed-to-Payara-with-Keycloak).

# Configuration

Read [the wiki](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Drupal-application-with-Keycloak).

# Test environment

- Windows 10
- Docker Desktop - Community, version 2.1.0.5
- Drupal 8 using Docker image ricardoamaro/drupal8 (listed on [drupal.org](https://www.drupal.org/docs/develop/local-server-setup/docker-development-environments))
- [OpenID Connect Drupal module](https://www.drupal.org/project/openid_connect), version 8.x-1.0-beta5
- [Keycloak Integration Drupal module](https://www.drupal.org/project/keycloak), version 8.x-1.3
- Keycloak 8.0.1 on OpenJDK 11

Keycloak and Docker/Drupal were run locally on the same host. A client was configured in Keycloak.

# History

The base Keycloak module does not support role mapping. Using the base module, you can authenticate with Keycloak, but
Keycloak roles are not used. The MoH Keycloak module as role mapping.

# Implementation notes

I made two changes to the base module source code:

1. I added the "Role Mapping" attribute to the OpenID configuration from in `MoHKeycloak.php`, and to the configuration
and schema files.
2. I implemented the `mohkeycloak_openid_connect_post_authorize` hook in `mohkeycloak.module`.

I wasn't happy duplicating the Keycloak module source code, so initially I tried to extend it, but this did not work
well. Here are some reasons why:

- The configuration properties file must be duplicated in its entirety as there is no mechanism to "extend" a
configuration file.
- If you duplicate the configuration file, you have created a fragile dependency on the properties. If the base
Keycloak module changes properties (add, remove, rename), it will break your subclass.
- The same applies to the schema file.
- The Keycloak module uses an annotation to register itself (see the `OpenIDConnectClient` annotation in
`Keycloak.php`). If you extend the module, you cannot stop it from registering itself, so it will appear in the list of
available OpenID Providers alongside your subclass.
- The Keycloak module is not designed to be extended. The module author has no reason to not make breaking changes to
the API.
- The module author has not updated the module in two years. If future updates are doubtful, why bother extending?

# Issues

- Log out is not working. If a user logs out in Drupal, they are not logged out in Keycloak. This means that when they
click the Log In button again, they will be automatically authenticated without reentering their credentials.

# Suggestions

- Try to update the Keycloak module source code to the latest version. I tried myself, but I had a dependency
conflict with the OpenID module and ran out of time to figure it out.
- Also try to update the OpenID module to the latest version. (I suspect using an old version caused the dependency conflict
mentioned above.)

Both of the above suggestions would gain us many features and bug fixes. I think it would fix the log out bug.

# Links

- [The base Keycloak module on Drupal.org](https://www.drupal.org/project/keycloak) with links to the OpenID module
dependency, source code, documentation, and issues.
