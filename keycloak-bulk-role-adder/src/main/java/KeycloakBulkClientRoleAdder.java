import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ProcessingException;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class KeycloakBulkClientRoleAdder {

    private static boolean SIMULATION_MODE = true;
    private static boolean CREATE_MISSING_USERS = false;
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {

        // 1. Parse + validate input
        CliConfig config = parseArgs(args);
        EnvConfig targetEnv = resolveEnvironment(config.env);

        validateClientSecret(targetEnv);

        // 2. Load input + setup logging
        List<UserRoleEntry> entries = loadUserRoleEntries(config.inputFilePath);
        setupLoggingToFile();

        printStartupSummary(targetEnv, entries.size());
        printEnvironmentBanner(targetEnv.url);

        // 3. Safety check
        confirmProductionRun(targetEnv);

        // 4. Connect + preflight
        Keycloak keycloak = authenticate(targetEnv);
        preflight(keycloak, config.realm, config.clientId);

        String clientUuid = requireClientUuid(keycloak, config.realm, config.clientId);

        Map<String, RoleRepresentation> clientRoleMap =
                loadClientRoleMap(keycloak, config.realm, clientUuid);

        preflightRoles(
                collectRequestedRoleNames(entries),
                clientRoleMap,
                config.clientId
        );

        // 5. Execute
        runBulkProcessing(keycloak, config.realm, clientUuid, clientRoleMap, entries);

        // 6. Shutdown
        keycloak.close();

        System.out.println("\nProcessed " + entries.size() + " entries in " + targetEnv.name() + ".");
        System.out.println("Done.");
    }

    static void validateClientSecret(EnvConfig env) {
        if (env.clientSecret == null || env.clientSecret.isEmpty()) {
            System.err.println(RED + "Missing client secret for environment " + env.name() + RESET);
            System.err.println("Expected env var name: " + CYAN + env.clientId + RESET);
            System.exit(1);
        }
    }

    record CliConfig(String inputFilePath, String env, String realm, String clientId) {
    }

    static CliConfig parseArgs(String[] args) {
        String inputFilePath = null;
        String envArg = null;
        String realm = null;
        String clientId = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--input") && i + 1 < args.length) {
                inputFilePath = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--env") && i + 1 < args.length) {
                envArg = args[i + 1].toUpperCase();
            }
            if (args[i].equalsIgnoreCase("--realm") && i + 1 < args.length) {
                realm = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--client") && i + 1 < args.length) {
                clientId = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("--real")) {
                SIMULATION_MODE = false;
            }
            if (args[i].equalsIgnoreCase("--create-missing-users")) {
                CREATE_MISSING_USERS = true;
            }
        }

        if (inputFilePath == null || envArg == null || realm == null || clientId == null) {
            System.err.println("Usage: ...");
            System.exit(1);
        }

        return new CliConfig(inputFilePath, envArg, realm, clientId);
    }

    static EnvConfig resolveEnvironment(String envArg) {
        try {
            return EnvConfig.valueOf(envArg);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid environment: " + envArg);
            System.exit(1);
            return null;
        }
    }

    static String requireClientUuid(Keycloak keycloak, String realm, String clientId) {
        String uuid = getClientUuid(keycloak, realm, clientId);
        if (uuid == null) {
            System.err.println(RED + "Client not found: " + clientId + RESET);
            System.exit(1);
        }
        return uuid;
    }

    static void runBulkProcessing(
            Keycloak keycloak,
            String realm,
            String clientUuid,
            Map<String, RoleRepresentation> clientRoleMap,
            List<UserRoleEntry> entries
    ) {
        int threads = Math.min(10, Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<?>> futures = new ArrayList<>();

        for (UserRoleEntry entry : entries) {
            futures.add(executor.submit(() -> {
                ProcessingContext ctx = new ProcessingContext(keycloak, realm, clientUuid, clientRoleMap);
                processUser(ctx, entry);
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }

    static void printStartupSummary(EnvConfig env, int count) {
        System.out.println("Running in " + (SIMULATION_MODE ? GREEN + "SIMULATION" : RED + "REAL") + RESET + " mode.");
        System.out.println("Target environment: " + CYAN + env.name() + RESET);
        System.out.println("Loaded " + count + " user/role entries.");
    }

    private static void confirmProductionRun(EnvConfig targetEnv) {
        if (!SIMULATION_MODE && targetEnv.isProd()) {
            System.out.print(RED + "You are running in PRODUCTION and REAL mode. Type 'I UNDERSTAND' to continue: " + RESET);
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("I UNDERSTAND")) {
                System.out.println("Aborted.");
                System.exit(1);
            }
        }
    }

    static void processUser(ProcessingContext ctx, UserRoleEntry entry) {
        try {
            UserRepresentation user = findOrCreateUser(ctx, entry);
            if (user == null) {
                flush(ctx.log);
                return;
            }

            ctx.log("\nUser: " + user.getUsername() + " (" + user.getId() + ")");

            UserResource userRes = ctx.keycloak.realm(ctx.realm).users().get(user.getId());

            Set<String> existingRoleNames = getExistingRoleNames(userRes, ctx.clientUuid);

            List<RoleRepresentation> rolesToAdd =
                    determineRolesToAdd(ctx, entry, existingRoleNames);

            applyRoles(ctx, rolesToAdd, user, userRes);

        } catch (Exception e) {
            ctx.log("Error processing user " + entry.username + ": " + e.getMessage());
        }

        flush(ctx.log);
    }

    private static void applyRoles(
            ProcessingContext ctx,
            List<RoleRepresentation> rolesToAdd,
            UserRepresentation user,
            UserResource userRes
    ) {
        if (rolesToAdd.isEmpty()) {
            ctx.log("No new roles to add.");
            return;
        }

        String roleNames = rolesToAdd.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.joining(", "));

        if (SIMULATION_MODE) {
            ctx.log("[SIMULATION] Would add roles " + roleNames + " to user " + user.getUsername());
        } else {
            userRes.roles().clientLevel(ctx.clientUuid).add(rolesToAdd);
            ctx.log("[REAL] Added roles " + roleNames + " to user " + user.getUsername());
        }
    }

    @Nonnull
    private static Set<String> getExistingRoleNames(UserResource userRes, String clientUuid) {
        List<RoleRepresentation> existingRoles = userRes.roles().clientLevel(clientUuid).listEffective();

        return existingRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

    private static List<RoleRepresentation> determineRolesToAdd(
            ProcessingContext ctx,
            UserRoleEntry entry,
            Set<String> existingRoleNames
    ) {
        List<RoleRepresentation> rolesToAdd = new ArrayList<>();

        for (String roleName : entry.roles) {
            RoleRepresentation role = ctx.clientRoleMap.get(roleName);

            if (role == null) {
                ctx.log("ERROR: Role not found in map: " + roleName);
                continue;
            }

            if (!existingRoleNames.contains(roleName)) {
                rolesToAdd.add(role);
            } else {
                ctx.log("Role already assigned: " + roleName);
            }
        }

        return rolesToAdd;
    }

    private static UserRepresentation findOrCreateUser(ProcessingContext ctx, UserRoleEntry entry) {
        List<UserRepresentation> found = ctx.keycloak.realm(ctx.realm).users().search(entry.username, true);

        UserRepresentation user = found.stream()
                .filter(u -> entry.username.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            if (!CREATE_MISSING_USERS) {
                ctx.log("User not found: " + entry.username);
                return null;
            }

            if (SIMULATION_MODE) {
                ctx.log("[SIMULATION] Would create user: " + entry.username);
                return null;
            }

            UserRepresentation newUser = new UserRepresentation();
            newUser.setUsername(entry.username);
            newUser.setEnabled(true);

            try (Response response = ctx.keycloak.realm(ctx.realm).users().create(newUser)) {

                if (response.getStatus() >= 300) {
                    ctx.log("ERROR creating user " + entry.username + ": HTTP " + response.getStatus());
                    return null;
                }

                ctx.log("[REAL] Created user: " + entry.username);

                List<UserRepresentation> created =
                        ctx.keycloak.realm(ctx.realm).users().search(entry.username, true);

                return created.stream()
                        .filter(u -> entry.username.equalsIgnoreCase(u.getUsername()))
                        .findFirst()
                        .orElse(null);

            } catch (Exception e) {
                ctx.log("ERROR creating user " + entry.username + ": " + e.getMessage());
                return null;
            }
        }

        return user;
    }

    static synchronized void flush(StringBuilder log) {
        System.out.print(log.toString());
    }

    // --- Preflight auth + perms ---
    static void preflight(Keycloak keycloak, String realm, String clientId) {
        System.out.println(CYAN + "Preflight: verifying credentials and permissions..." + RESET);
        try {
            // Force token retrieval to catch 401 immediately
            keycloak.tokenManager().grantToken();
        } catch (ProcessingException | NotAuthorizedException e) {
            System.err.println(RED + "Authentication failed (401 Unauthorized)." + RESET);
            System.err.println("Likely causes:");
            System.err.println(" - Wrong client secret");
            System.err.println(" - Client disabled or not in realm '" + realm + "'");
            System.err.println(" - Using the wrong server URL/realm for these credentials");
            throw e;
        }

        try {
            // Minimal permission probe: requires at least view-users (or query-users) on realm-management
            keycloak.realm(realm).users().count();
        } catch (ForbiddenException e) {
            System.err.println(RED + "Authenticated, but missing permissions (403 Forbidden) to list users." + RESET);
            System.err.println("Service account likely needs realm-management roles such as:");
            System.err.println(" - view-users (to search/count users)");
            System.err.println(" - manage-users (to assign roles)");
            System.err.println(" - view-clients or query-clients (to read client + roles)");
            throw e;
        }

        try {
            // Quick existence check for the target client; if missing permissions, this can also 403
            List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
            if (clients.isEmpty()) {
                System.err.println(RED + "Client with clientId '" + clientId + "' not found in realm '" + realm + "'." + RESET);
                System.err.println("Double-check the --client value, realm, and your account's client view permissions.");
                System.exit(1);
            }
        } catch (ForbiddenException e) {
            System.err.println(RED + "Authenticated, but missing permissions (403 Forbidden) to view clients." + RESET);
            System.err.println("Service account likely needs: view-clients or query-clients on realm-management.");
            throw e;
        }

        System.out.println(GREEN + "Preflight OK." + RESET);
    }

    static String getClientUuid(Keycloak keycloak, String realm, String clientId) {
        List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
        if (clients.isEmpty()) return null;
        return clients.get(0).getId();
    }

    static List<UserRoleEntry> loadUserRoleEntries(String path) {
        List<UserRoleEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = stripBOM(line).trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split(",", 2);
                if (parts.length < 2) continue;

                String username = parts[0].trim();
                String rolesPart = parts[1].replaceAll("^\"|\"$", "").trim();
                List<String> roles = Arrays.stream(rolesPart.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .collect(Collectors.toList());

                entries.add(new UserRoleEntry(username, roles));
            }
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + e.getMessage());
            System.exit(1);
        }
        return entries;
    }

    static String stripBOM(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\ufeff') {
            return s.substring(1);
        }
        return s;
    }

    static class UserRoleEntry {
        String username;
        List<String> roles;

        UserRoleEntry(String username, List<String> roles) {
            this.username = username;
            this.roles = roles;
        }
    }

    enum EnvConfig {
        DEV("https://common-logon-dev.hlth.gov.bc.ca/auth", "immsbc-bulk-load-serviceaccount-dev"),
        TEST("https://common-logon-test.hlth.gov.bc.ca/auth", "immsbc-bulk-load-serviceaccount-test"),
        PROD("https://common-logon.hlth.gov.bc.ca/auth", "immsbc-bulk-load-serviceaccount-prod");

        public final String url;
        public final String clientId;   // env var name
        public final String clientSecret;

        EnvConfig(String url, String clientId) {
            this.url = url;
            this.clientId = clientId;
            this.clientSecret = System.getenv(clientId);
        }

        public boolean isProd() {
            return this == PROD;
        }
    }

    static Keycloak authenticate(EnvConfig config) {
        return KeycloakBuilder.builder()
                .serverUrl(config.url)
                .realm("moh_applications")
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .grantType("client_credentials")
                .build();
    }

    public static void setupLoggingToFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String logFileName = "KeycloakBulkClientRoleAdder_" + timestamp + ".log";

        try {
            PrintStream originalOut = System.out;
            PrintStream fileOut = new PrintStream(new FileOutputStream(logFileName));

            PrintStream dualOut = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    originalOut.write(b);
                    fileOut.write(b);
                }

                @Override
                public void flush() {
                    originalOut.flush();
                    fileOut.flush();
                }
            }, true);

            System.setOut(dualOut);
            System.setErr(dualOut);
            System.out.println("Logging to " + logFileName);
        } catch (IOException e) {
            System.err.println("Failed to set up log file: " + e.getMessage());
        }
    }

    static void printEnvironmentBanner(String url) {
        String urlLower = url.toLowerCase();
        boolean isProd = !(urlLower.contains("dev") || urlLower.contains("test"));

        String banner = "\n==============================\n" +
                "  ENVIRONMENT: " + CYAN + url.toUpperCase() + RESET + "\n" +
                (isProd
                        ? RED + "  ⚠️ PRODUCTION ENVIRONMENT — BE CAREFUL! ⚠️" + RESET + "\n"
                        : GREEN + "  (Non-prod environment)" + RESET + "\n") +
                "==============================";

        if (isProd) {
            Toolkit.getDefaultToolkit().beep();
        }

        System.out.println(banner);
    }

    static Map<String, RoleRepresentation> loadClientRoleMap(Keycloak keycloak, String realm, String clientUuid) {
        List<RoleRepresentation> roleList = keycloak.realm(realm).clients().get(clientUuid).roles().list();
        Map<String, RoleRepresentation> map = new HashMap<>(roleList.size());
        for (RoleRepresentation rr : roleList) {
            map.put(rr.getName(), rr); // keep case sensitivity; KC role names are case-sensitive
        }
        return map;
    }

    static void preflightRoles(Set<String> requestedRoleNames, Map<String, RoleRepresentation> clientRoleMap, String clientId) {
        List<String> missing = requestedRoleNames.stream()
                .filter(r -> !clientRoleMap.containsKey(r))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            System.err.println(RED + "Missing roles on client '" + clientId + "':" + RESET);
            missing.forEach(r -> System.err.println("  - " + r));
            System.err.println("Create these roles on the client or fix the input file, then re-run.");
            System.exit(1);
        }
    }

    static Set<String> collectRequestedRoleNames(List<UserRoleEntry> entries) {
        return entries.stream()
                .flatMap(e -> e.roles.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static class ProcessingContext {
        final Keycloak keycloak;
        final String realm;
        final String clientUuid;
        final Map<String, RoleRepresentation> clientRoleMap;

        final StringBuilder log = new StringBuilder();

        ProcessingContext(Keycloak keycloak, String realm, String clientUuid,
                          Map<String, RoleRepresentation> clientRoleMap) {
            this.keycloak = keycloak;
            this.realm = realm;
            this.clientUuid = clientUuid;
            this.clientRoleMap = clientRoleMap;
        }

        void log(String msg) {
            log.append(msg).append("\n");
        }
    }
}
