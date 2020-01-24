/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.hlth.importldaptokc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import javax.naming.NamingException;

/**
 * This program assigns roles to Keycloak users based on their LDAP role.
 * 
 * Steps:
 * <ol>
 * <li>Query LDAP for uid and role.</li>
 * <li>Add the LDAP roles to a Keycloak client.</li>
 * <li>Add roles to Keycloak users.</li>
 * </ol>
 */
public class ImportLdapToKc {

    private static String configPath;
    
    private static String ldapUrl;
    private static String ldapCredentials;
    private static String objectClass;
    private static String userAttribute;
    private static String kcClientName;
    private static String kcAdminUser;
    private static String kcAdminPass;
    private static String kcBaseUrl;
    private static String kcRealmName;

    private static LdapUserProvider ldap;
    private static KeycloakRoleLoader keycloak;

    private static final Logger LOG = Logger.getLogger(ImportLdapToKc.class.getName());
    
    public static void main(String[] args) throws NamingException, InterruptedException, IOException {
        if (args != null && args.length != 0) {
            configPath = args[0];
        } else {
            configPath = "/configuration.properties";
        }
        LOG.info(String.format("Configuration file expected at '%s'.", configPath));
        initialize();
        List<LdapUser> ldapUsers = ldap.queryUsers();
        keycloak.loadRoles(ldapUsers);
    }

    private static void initialize() throws IOException {
        Properties configProperties = new Properties();
        File file = new File(configPath);
        InputStream inputStream;
        if (file.exists()) {
            inputStream = new FileInputStream(file);
        } else {
            inputStream = ImportLdapToKc.class.getResourceAsStream(configPath);
        }
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));

        configProperties.load(inputStream);

        ldapUrl = configProperties.getProperty("ldap_url");
        checkMandatory(ldapUrl);
        ldapCredentials = configProperties.getProperty("ldap_credentials");
        checkMandatory(ldapCredentials);
        objectClass = configProperties.getProperty("object_class");
        checkMandatory(objectClass);
        userAttribute = configProperties.getProperty("user_attribute");
        checkMandatory(userAttribute);
        kcClientName = configProperties.getProperty("kc_client");
        checkMandatory(kcClientName);
        kcAdminUser = configProperties.getProperty("kc_admin_user");
        checkMandatory(kcAdminUser);
        kcAdminPass = configProperties.getProperty("kc_admin_pass");
        checkMandatory(kcAdminPass);
        kcBaseUrl = configProperties.getProperty("kc_base_url");
        checkMandatory(kcBaseUrl);
        kcRealmName = configProperties.getProperty("kc_realm_name");
        checkMandatory(kcRealmName);

        ldap = new LdapUserProvider(ldapUrl, ldapCredentials, objectClass, userAttribute);
        keycloak = new KeycloakRoleLoader(kcClientName, kcAdminUser, kcAdminPass, kcBaseUrl, kcRealmName);
    }

    private static void checkMandatory(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(String.format("Value is mandatory but was '%s'.", value));
        }
    }
}
