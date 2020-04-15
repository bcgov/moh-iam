# Custom Keycloak Admin Console

This application is a proof of concept. It demonstrates how a Javascript front-end can use Keycloak's REST APIs to administer users. 

It's based off of Keycloak’s [Basic JavaScript Example]( https://github.com/keycloak/keycloak/tree/master/examples/js-console), which uses [Keycloak’s Javascript adapter](https://www.keycloak.org/docs/latest/securing_apps/index.html#_javascript_adapter). The official example is excellent by itself, but we made some changes to highlight and demonstrate required functionality:

* We configured the Keycloak Javascript adapter to require login on page load, and display a blank page if the user is not authenticated.
* We also configured the adapter to work with Keycloak’s default `security-admin-console` client in [keycloak.json]( https://github.com/bcgov/moh-iam/blob/master/keycloak-js-example/src/main/webapp/keycloak.json) to allow the application to call Keycloak’s REST API.
* We added a “Search Users” field to demonstrate how to call the Keycloak REST APIs This could be expanded into complete user management.
* We applied basic Ministry of Health page styling.

# How to deploy

* The application is a static site and can be deployed to any web server.
* You need a Keycloak server , and you need to configure a "Client" in Keycloak which is explained below.
* Edit `keycloak.json` to point at your Keycloak server.

**TODO** 

If you really want to deploy this version, you also need to change these URLs to point to your Keycloak server. We will parameterize this in a later commit.

* `<script src="http://localhost:8081/auth/js/keycloak.js" defer></script>` is hardcoded in `index.html`.
* `http://localhost:8081/auth...` is harcoded in app.js.

# Client configuration

* The client should be `public` because there is no way to secure a "secret" on the client side.
* You need to configure Valid Redirect URIs and Web Origins. Setting them to `*` could suffice in a development environment, but that's insecure.
* We reused Keycloak's default `security-admin-console` for this example. That's the same client used by Keycloak's Admin Console.

![image](https://user-images.githubusercontent.com/1767127/79283978-448fd280-7e6e-11ea-834e-27782e13fe47.png)

## New client configuration

As explained above, we reused the `security-admin-console` client. If you want to use a different client, you just need to configure the same options listed above: public access, Valid Redirect URIs, and Web Origins. In order for the new client use Keycloak's REST APIs to administer users, add the `realm-management` client roles to the client's scope mappings as shown in this example:

![image](https://user-images.githubusercontent.com/1767127/79284448-84a38500-7e6f-11ea-971d-6aa6b24cac3d.png)

Note that this isn't exactly authorizing the _client_ to do "realm-management"; it just means that if a _user_ has "realm-management" permissions, those permissions will be included in the user's access token, which will then allow the user to use Keycloak's REST API to administer users.

You could also just leave the default setting of "Full Scope Allowed", but it's more secure to  limit the scope.

# Appendix

* [Documentation on Keycloak’s JavaScript adapter]( https://www.keycloak.org/docs/latest/securing_apps/index.html#_javascript_adapter).
* [Keycloak’s Basic JavaScript Example]( https://github.com/keycloak/keycloak/tree/master/examples/js-console).
* [Documentation on Keycloak's Role Scope Mappings](https://www.keycloak.org/docs/latest/server_admin/#_role_scope_mappings).
