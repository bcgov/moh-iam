# KeycloakFailoverTester

A simple Java program for testing **Keycloak failover behavior**, **load balancer routing**, and **token request performance** in a clustered environment.

This tool sends concurrent client credential token requests to a Keycloak endpoint, tracks routing via the `ROUTEID` cookie, and reports metrics such as success/failure rates, response times, and instance distribution.

## 🔧 Features

- Sends a configurable number of concurrent `client_credentials` token requests
- Optionally forces routing to a specific Keycloak instance (`ROUTEID`)
- Tracks:
  - Success and failure counts
  - Average response time
  - Instance routing based on ROUTEID
  - Total runtime of the test

## 📦 Requirements

- Java 11 or higher
- A Keycloak client configured for `client_credentials` grant type
- Environment variable containing the client secret

## 🚀 Usage

### 1. Set the client secret as an environment variable

```bash
export {CLIENT_ID}=your_client_secret_here
```

Replace `{CLIENT_ID}` with the client ID of the client used for client credential authentication.

### 2. Compile and run the program

This program is run in a Java IDE, which handles compilation and execution automatically.

## ⚙️ Configuration (in code)

Modify the following in `main()` as needed:

| Parameter         | Description                                 |
|------------------|---------------------------------------------|
| `clientId`       | Client ID for client credentials            |
| `totalRequests`  | Total number of requests to send            |
| `threads`        | Number of parallel threads                  |
| `forcedRoute`    | Use `.1` or `.2` to target a specific node  |

To observe natural load balancing, you can remove or randomize the `ROUTEID` value in the request header.

## 🧪 Sample Output

```text
✅ Request 42 | 200 OK | Time: 124ms | ROUTEID: 1
❌ Request 58 | Error 503 | Time: 89ms | ROUTEID: 1

===== 📊 Test Summary =====
✅ Success Count: 995
❌ Failure Count: 5
📈 Avg Response Time: 137.6ms
🖥️ Requests to ROUTEID 1 (tegu): 997
🖥️ Requests to ROUTEID 2 (skink): 3
⏳ Total Runtime: 9.7 seconds (9732ms)
===========================
```

## 📝 Notes

- This tool is meant for test environments only — it does not store or reuse tokens.
- ROUTEID values:
  - `.1` typically routes to instance **tegu**
  - `.2` typically routes to instance **skink**
- If you don't control the load balancer, sticky sessions may prevent true round-robin distribution.
- You can test failover behavior by disabling one Keycloak node during the run and observing routing changes and failures.