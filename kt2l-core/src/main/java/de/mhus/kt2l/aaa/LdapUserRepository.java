package de.mhus.kt2l.aaa;

import de.mhus.kt2l.aaa.oauth2.AuthProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(value = "kt2l.aaa.ldapUserRepository", havingValue = "true")
public class LdapUserRepository extends AbstractUserRepository {

    private DirContext context;
    private String group;

    @Override
    protected void internalCreateUser(AaaUser user) {
        initLdapContext();

    }

    private synchronized void initLdapContext() {
        if (context == null) {
            try {
                var env = new Hashtable<String, String>();
                System.getenv().forEach((k, v) -> {
                    if (k.startsWith("LDAP_ENV_")) {
                        k = k.substring(9).toLowerCase().replace('_', '.').toLowerCase();
                    }
                });
                context = new InitialDirContext(env);
                group = System.getenv("LDAP_GROUP_CONTEXT");
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void internalUpdateUser(AaaUser updatedUser) {
        initLdapContext();

    }

    @Override
    public void deleteUser(String userId) {
        initLdapContext();

    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        initLdapContext();

    }

    @Override
    public boolean userExists(String userId) {
        initLdapContext();
        return false;
    }

    @Override
    public Optional<AaaUser> getUserByUserId(String userId) {
        initLdapContext();
        var searchControls = getSearchControls();
        try {
            NamingEnumeration<SearchResult> answer = context.search(group, System.getenv("LDAP_USER_ID") + "=" + userId, searchControls);
            if (answer.hasMore()) {
                return Optional.of(toAaaUser(answer.next().getAttributes()));
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private AaaUser toAaaUser(Attributes attributes) {
        try {
            return AaaUser.builder()
                    .userId(attributes.get(System.getenv("LDAP_USER_ID")).get().toString())
                    .displayName(attributes.get(System.getenv("LDAP_USER_NAME")).get().toString())
                    .email(attributes.get(System.getenv("LDAP_USER_EMAIL")).get().toString())
                    .roles(List.of(attributes.get(System.getenv("LDAP_USER_ROLES")).get().toString().split(",")))
                    .encodedPassword(attributes.get(System.getenv("LDAP_USER_PASSWORD")).get().toString()) //xxx
                    .providerId(attributes.get(System.getenv("LDAP_USER_ID") + "@local").get().toString())
                    .provider(AuthProvider.LOCAL_AUTH_PROVIDER_ID)
                    .build();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<AaaUser> getByEmail(String email) {
        initLdapContext();
        return Optional.empty();
    }

    private static SearchControls getSearchControls() {
        SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String[] attrIDs = System.getenv("LDAP_ATTRIBUTES").split(",");
        cons.setReturningAttributes(attrIDs);
        return cons;
    }

}
