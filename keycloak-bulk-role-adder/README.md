# KeycloakBulkClientRoleAdder

A small standalone Java utility for safely adding **client roles** to existing Keycloak users in bulk.

This replaces the older, more complex *Keycloak Data Loader* tool.
It’s focused, auditable, and built for admins who need to batch-assign roles without touching anything else.

---

## What it does

* Reads a CSV file of users and roles.
* Looks up each user by **exact username** in a given realm.
* Adds the listed **client roles** for a specified Keycloak client.
* Skips roles the user already has.
* Runs in **simulation mode by default** — nothing is modified unless `--real` is specified.
* Logs all console output to a timestamped log file.

No user creation, no deletions, no overwriting of existing roles.

---

## Example input file

```csv
david.sharpe@phsa.ca@phsa,"role1,role2"
jane.doe@phsa.ca@phsa,"report_viewer,admin_portal_access"
```

Each line:

1. A Keycloak username (must match exactly).
2. A comma-separated list of roles, quoted if it contains commas.

---

## Usage

```bash
java KeycloakBulkClientRoleAdder \
  --input users_roles.csv \
  --env DEV|TEST|PROD \
  --realm REALM_NAME \
  --client CLIENT_ID \
  [--real]
```

### Example

```bash
java KeycloakBulkClientRoleAdder \
  --input input.csv \
  --env TEST \
  --realm moh_applications \
  --client ICY
```

By default, the tool runs in **SIMULATION** mode.
Use `--real` to perform actual updates.

---

## Environment configuration

The tool authenticates using a Keycloak **service account**.
Each environment uses a different service client and secret, provided via environment variables.

| Env  | Keycloak URL                                                                                   | Env var (clientId)         |
| ---- | ---------------------------------------------------------------------------------------------- | -------------------------- |
| DEV  | [https://common-logon-dev.hlth.gov.bc.ca/auth](https://common-logon-dev.hlth.gov.bc.ca/auth)   | `admin-safety-toggle-dev`  |
| TEST | [https://common-logon-test.hlth.gov.bc.ca/auth](https://common-logon-test.hlth.gov.bc.ca/auth) | `admin-safety-toggle-test` |
| PROD | [https://common-logon.hlth.gov.bc.ca/auth](https://common-logon.hlth.gov.bc.ca/auth)           | `admin-safety-toggle-prod` |

Before running, set the appropriate environment variable:

```bash
export admin-safety-toggle-test='<client-secret>'
```

If the secret is missing, the program exits before making any API calls.

---

## Safety features

* **Simulation mode:** shows exactly what would change without performing it.
* **PROD confirmation:** in real mode, requires typing `I UNDERSTAND` before proceeding.
* **Preflight checks:** verifies credentials, permissions, and client existence before continuing.
* **Role validation:** confirms that every role in the CSV actually exists on the target client before touching any users.
* **Logging:** all console output is mirrored to a timestamped `.log` file.

---

## Output

Example simulation:

```
Running in SIMULATION mode.
Target environment: TEST
Loaded 12 user/role entries.

Preflight OK.

User: david.sharpe@phsa.ca@phsa (bfacdd98-271e-4bed-8152-be16dcc920a4)
[SIMULATION] Would add roles role1, role2 to user david.sharpe@phsa.ca@phsa

Processed 12 entries in TEST.
Done.
```

---

## Expected permissions

The service account must have these roles (via `realm-management`):

* `view-users`
* `manage-users`
* `view-clients` or `query-clients`

Without `view-clients`, Keycloak may silently hide the target client and fail the preflight with a “client not found” message.

---

## Notes

* Usernames must match exactly; partial or case-insensitive matches are not performed.
* Roles are **case-sensitive** in Keycloak.
* The program only adds roles; it never removes or replaces existing ones.
* Input files created in Excel may include a UTF-8 BOM; the program strips it automatically.
* If a role in the CSV does not exist on the client, the run halts before making changes.
