# Identify and Fix Users with Invalid Characters in Name Attributes

---

Keycloak uses a validator to check if a value is a valid person name, as an added safeguard against attacks such as script injection.

The validation relies on a default RegEx pattern that blocks characters not commonly found in person names. This script removes all parentheses from users' first and last names.

## User Identification

Run the SQL query located in the `userList.sql` file and save the result to a _pipe-delimited_ file.

### Example

```text
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa|moh_applications
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa|moh_applications
```

## Fix

### Update `config.properties`

Before running the script, update the `config.properties` file with the required values:

| Property           | Description                         | Example Value |
|--------------------|-------------------------------------|----------------|
| `INPUT_FILE_PATH`  | Full path to the input file         | `C:/path/to/your/input.txt` |
| `KEYCLOAK_URL`     | URL of the Keycloak instance        | `https://your-keycloak-instance/auth` |
| `CLIENT_ID`        | ID of the Keycloak client           | `my-client-id` |
| `CLIENT_SECRET`    | Secret for the Keycloak client      | `my-client-secret` |
| `REALM`            | Realm containing Keycloak client    | `master` |
| `SIMULATION_MODE`  | `true` to simulate only, `false` to apply changes | `true` |

> ⚠️ **Note**: Set `SIMULATION_MODE = true` to run the tool in read-only mode. This prevents any real changes from being made.

### Service Account Requirements

The values for `CLIENT_ID`, `CLIENT_SECRET`, and `REALM` refer to a **Keycloak client configured as a service account** with the necessary permissions to modify users.

- The service account must have at least the following client roles:
    - `view-users`
    - `manage-users`

- If the script is intended to modify users across **multiple realms**, the service account client must be created in the `master` realm.

- If the script targets **only a single realm**, the service account client can be created directly within that realm instead.

Make sure that the service account has sufficient access to the target realm(s) to allow user updates.

### Steps

1. Set `SIMULATION_MODE = true` for the first run.
2. Run the `main` method in the `KeycloakFirstLastNameFixer` class.
3. Validate the output in the console.
4. If the results are correct, set `SIMULATION_MODE = false` and repeat step 2 to apply the changes.
