# Keycloak IDP Unlinker

This utility is used to fix a specific identity conflict in Keycloak where users attempting to sign in with a Health Authority ID (via the PHSA Entra ID tenant) were incorrectly registered with email-based usernames instead of UPNs.

These accounts must have their `phsa_aad` identity provider link removed so that on next login, Keycloak can correctly link the user to their UPN-based account — or create a new one as needed.

## Why this is needed

Keycloak was previously misconfigured to use email as the username for PHSA_AAD logins. This caused duplicate accounts and sign-in failures for Panorama users. Updating the username mapper alone causes a conflict unless these old identity links are removed first.

## Usage

```bash
java KeycloakIdpUnlinker --input bad_user_ids.txt --env TEST
java KeycloakIdpUnlinker --input bad_user_ids.txt --env PROD --real
````

## Configuration

The script includes built-in configuration for each environment, including its Keycloak base URL and service account client ID:

```java
DEV("https://common-logon-dev.hlth.gov.bc.ca/auth", "svc-keycloak-cli-RFC-20250811"),
TEST("https://common-logon-test.hlth.gov.bc.ca/auth", "..."),
PROD("https://common-logon.hlth.gov.bc.ca/auth", "...")
```

The service account must have the following realm-level roles in the `moh_applications` realm:

- manage-users
- view-users

The client secret for each environment must be set as an environment variable, using the client ID as the variable name. For example:

```bash
export svc-keycloak-cli-RFC-20250811="your-client-secret"
```