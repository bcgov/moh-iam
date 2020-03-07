A customized copy of the [Keycloak OpenID Connect](https://www.drupal.org/project/keycloak)
Drupal module (version `8.x-1.3`). Adds role mapping.

# Configuration

Read [the wiki](https://github.com/bcgov/moh-iam/wiki/How-to-secure-a-Drupal-application-with-Keycloak).

# Roles

Enabling role mapping allows roles to be managed in Keycloak instead of Drupal. To use role mapping, you just need to
specify the role attribute name in the OpenID configuration. Then, when a user logs in, the MoH Keycloak module will
 get the user's claims (from OIDC UserInfo) and assign all roles it finds there to the Drupal user.

Role mapping has some behaviours you should be aware of:
* All of the user's current roles in Drupal are _replaced_ by the Keycloak roles. This means that the user's existing
roles are removed.
* An exception to the above is the Administrator role. If the user has the Administrator role, it will be not be
removed.
* Roles will be updated every time the user logs in.
* Roles must already exist in Drupal. If the Keycloak user has roles that Drupal doesn't recognize, the role will not
be created or assigned.

# Test environment

- Windows 10
- Docker Desktop - Community, version 2.1.0.5
- Drupal 8 using Docker image ricardoamaro/drupal8 (listed on [drupal.org](https://www.drupal.org/docs/develop/local-server-setup/docker-development-environments))
- [OpenID Connect Drupal module](https://www.drupal.org/project/openid_connect), version 8.x-1.0-beta5
- [Keycloak Integration Drupal module](https://www.drupal.org/project/keycloak), version 8.x-1.3
- Keycloak 8.0.1 on OpenJDK 11

Keycloak and Docker/Drupal were run locally on the same host. A client was configured in Keycloak.

# Justification

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
- The module does not support custom claim mapping. You can make the default OpenID claims to Drupal user attributes,
but the configuration form does not support custom mappings.

# Suggestions

- Try to update the Keycloak module source code to the latest version. I tried myself, but I had a dependency
conflict with the OpenID module and ran out of time to figure it out.
- Also try to update the OpenID module to the latest version. (I suspect using an old version caused the dependency conflict
mentioned above.)

Both of the above suggestions would gain us many features and bug fixes. I think it would fix the log out bug.

# Links

- [The base Keycloak module on Drupal.org](https://www.drupal.org/project/keycloak) with links to the OpenID module
dependency, source code, documentation, and issues.

# How to set-up a development environment

These instructions aren't complete, but here's what I remember:

- I used [ddev](https://www.ddev.com/) to set-up a local development environment.
    - It's Docker based, so you need to install Docker.
    - I installed ddev using the Chocolatey package manager on Windows, as recommended.
    - [ddev instructions are here](https://www.ddev.com/get-started/).
    - Frequently used `ddev start`, `ddev ssh` (then `drush cr`), `ddev restart`, and `ddev describe`.
- To to keep the project in our moh-iam Github, I sym-linked "C:\Dev\moh-iam\keycloak-drupal-module" to
"C:\Users\david.a.sharpe\my-drupal8-site\web\modules\custom\mohkeycloak".
- To get Drupal/Guzzle to trust Keycloak's self-signed cert, I made a change in
`OpenIDConnectClientBase.php`: in the `__contruct` method, I added
`$this->httpClient = new Client(['verify' => false]);` to override the HTTP client.
- I used the [devel](https://www.drupal.org/project/devel) module to quickly run cache rebuild and reinstall modules.
