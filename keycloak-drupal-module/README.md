A customized copy of the [Keycloak OpenID Connect](https://www.drupal.org/project/keycloak)
Drupal module (version `8.x-1.3`). Adds role mapping.

# Packaging and installation

1. Package the entire project inside a tar.gz with file structure gz > tar > `mohkeycloak` folder. (I used 7-zip for this. Create the tar first, then gz it.)
2. Use the Drupal Admin UI to install the module. (Supposedly the Admin UI also supports zip files, but I had an error when I attempted to use zip packaging).

The module depends on the [OpenID Connect Drupal module](https://www.drupal.org/project/openid_connect), version 8.x-1.0-beta5.

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

I made several changes to the base Keycloak module source code:

1. Added the "Role Mapping" attribute to the OpenID configuration from in `MohKeycloak.php`, and to the configuration
and schema files.
2. Implemented the `mohkeycloak_openid_connect_post_authorize` hook in `mohkeycloak.module` for role mapping.
3. Added MohKeycloakRouteSubscriber to delegate logout to our own controller.
4. Added `sign_out_endpoint_kc` form field to OpenID Connect configuration in `MohKeycloak.php`.

I wasn't happy duplicating the Keycloak module source code, so initially I tried to extend it, but this did not work. Here are 
some reasons why:

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

- The module does not support custom claim mapping. You can make the default OpenID claims to Drupal user attributes,
but the configuration form does not support custom mappings.
- [OpenID Connect issue catalog on Drupal.org](https://www.drupal.org/project/issues/openid_connect?categories=All)
- [Keycloak issue catalog on Drupal.org](https://www.drupal.org/project/issues/keycloak?status=All&categories=All)

# Software versions used in development environment

- Windows 10
- Docker Desktop - Community, version 2.1.0.5
- Drupal 8 using ddev version v1.13.0
- [OpenID Connect Drupal module](https://www.drupal.org/project/openid_connect), version 8.x-1.0-beta5
- Keycloak 8.0.1 on OpenJDK 11

# How to set-up a development environment

These instructions aren't complete, but here's what I remember:

- I used [ddev](https://www.ddev.com/) to set-up a local development environment.
    - It's Docker based, so you need to install Docker.
    - I installed ddev using the Chocolatey package manager on Windows, as recommended.
    - [ddev instructions start here](https://www.ddev.com/get-started/), and these instructions describe how to [create a Drupal project](https://ddev.readthedocs.io/en/latest/users/cli-usage/#drupal-8-quickstart).
        - After following these instructions I encountered an error when attempting to view the site: "The website encountered an unexpected error. Please try again later." I resolved the error by running `ddev ssh`, then `drush cron` (more errors), then `drush cr`. Debugging this is outside the scope of this project.
    - Frequently used `ddev start`, `ddev ssh` (then `drush cr`), `ddev restart`, and `ddev describe`.
- To to keep the project in our moh-iam Github, I sym-linked "C:\Dev\moh-iam\keycloak-drupal-module" to
"C:\Users\david.a.sharpe\my-drupal8-site\web\modules\custom\mohkeycloak":`mklink /J keycloak-drupal-module/ /c/Users/david.a.sharpe/my-drupal8-site/web/modules/custom/mohkeycloak/`.
- To get Drupal/Guzzle to trust Keycloak's self-signed cert, I made a change in
`OpenIDConnectClientBase.php`: in the `__contruct` method, I added
`$this->httpClient = new Client(['verify' => false]);` to override the HTTP client. See below if that's not clear. [1]
- I used the [devel](https://www.drupal.org/project/devel) module to quickly run cache rebuild and reinstall modules.
- I had trouble installing the dev version of the OpenID module using composer (error "fatal: failed to read object c2d54a2...: Operation not permitted"), so I used downloaded the tar.gz from Drupal.org and extracted into the `web/modules/contrib` directory.

[1] Disable SSL certificate verification in `OpenIDConnectClientBase.php`:
```php
  use GuzzleHttp\Client;
  
  // ...

  public function __construct(
      array $configuration,
      $plugin_id,
      $plugin_definition,
      RequestStack $request_stack,
      ClientInterface $http_client,
      LoggerChannelFactoryInterface $logger_factory
  ) {
    parent::__construct(
      $configuration,
      $plugin_id,
      $plugin_definition
    );

    $this->requestStack = $request_stack;
    // $this->httpClient = $http_client;
    $this->httpClient = new Client(['verify' => false]);
    $this->loggerFactory = $logger_factory;
  }
```
