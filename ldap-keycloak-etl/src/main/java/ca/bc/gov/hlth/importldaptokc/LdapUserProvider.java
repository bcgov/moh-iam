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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * This program queries an LDAP server for users.
 */
public class LdapUserProvider {

    private final String ldapUrl;
    private final String ldapCredentials;
    private final String objectClass;
    private final String userAttribute;
    
    private static final Logger LOG = Logger.getLogger(LdapUserProvider.class.getName());

    LdapUserProvider(String ldapUrl, String ldapCredentials, String objectClass, String userAttribute) {
        this.ldapUrl = ldapUrl;
        this.ldapCredentials = ldapCredentials;
        this.objectClass = objectClass;
        this.userAttribute = userAttribute;
    }

    // Retrieve the list of users and associated role from LDAP that map to the input object class and attribute
    List<LdapUser> queryUsers() throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,o=HNET,st=BC,c=CA");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_CREDENTIALS, ldapCredentials);

        NamingEnumeration<?> namingEnum = null;
        LdapContext ctx = null;
        List<LdapUser> users = new ArrayList<>();

        try {
            ctx = new InitialLdapContext(env, null);
            ctx.setRequestControls(null);
            namingEnum = ctx.search("o=hnet,st=bc,c=ca", "(objectClass=" + objectClass + ")", getSimpleSearchControls());
            while (namingEnum.hasMore()) {
                SearchResult result = (SearchResult) namingEnum.next();
                Attributes attrs = result.getAttributes();

                users.add(new LdapUser(attrs.get("uid").get().toString(), attrs.get(userAttribute).get().toString()));
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            if (namingEnum != null) {
                namingEnum.close();
            }
        }
        LOG.info(String.format("Found %s matching users on LDAP.", users.size()));
        return users;
    }

    // Set the search Controls for LDAP Query
    private static SearchControls getSimpleSearchControls() {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"uid", "fmdbuserrole"});
        searchControls.setTimeLimit(30000);
        return searchControls;
    }

}
