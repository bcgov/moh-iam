package ca.bc.gov.hlth.auth.provider.mapper;

import org.jboss.logging.Logger;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Maps a UserModel attribute to an ID Token claim. The token claim name can be a fully qualified nested object name,
 * such as "address.country", creating a nested JSON object within the token claim.
 * <p>
 * OverrideUserAttributeMapper is copied from the built-in UserAttributeMapper in Keycloak v22, with modifications to
 * change the default priority from 0 to 10, ensuring this mapper runs after any built-in mappers.
 * <p>
 * Source was copied from <a href="https://github.com/keycloak/keycloak/blob/release/22.0/services/src/main/java/org/keycloak/protocol/oidc/mappers/UserAttributeMapper.java">
 * Keycloak's UserAttributeMapper</a>.
 * <p>
 * Author: <a href="mailto:david.a.sharpe@cgi.com">David Sharpe</a>
 */
@SuppressWarnings("unused")
public class OverrideUserAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger LOGGER = Logger.getLogger(OverrideUserAttributeMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        LOGGER.infof("Loading class: %s", OverrideUserAttributeMapper.class.getName());

        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, OverrideUserAttributeMapper.class);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.MULTIVALUED);
        property.setLabel(ProtocolMapperUtils.MULTIVALUED_LABEL);
        property.setHelpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.AGGREGATE_ATTRS);
        property.setLabel(ProtocolMapperUtils.AGGREGATE_ATTRS_LABEL);
        property.setHelpText(ProtocolMapperUtils.AGGREGATE_ATTRS_HELP_TEXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-override-usermodel-attribute-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Override User Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom user attribute to a token claim. Runs after the built-in User Attribute Mapper.";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    /*
    This method has been deprecated since Keycloak 20, but it remains available in Keycloak 24. The built-in
    UserAttributeMapper has used it consistently across multiple major versions. To simplify maintenance, we will also
    continue using the deprecated method to avoid extensive changes to the built-in UserAttributeMapper.
    */
    @SuppressWarnings("deprecation")
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        boolean aggregateAttrs = Boolean.valueOf(mappingModel.getConfig().get(ProtocolMapperUtils.AGGREGATE_ATTRS));
        Collection<String> attributeValue = KeycloakModelUtils.resolveAttribute(user, attributeName, aggregateAttrs);
        if (attributeValue == null) return;
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean accessToken, boolean idToken, boolean multivalued) {
        return createClaimMapper(name, userAttribute, tokenClaimName, claimType,
                accessToken, idToken, multivalued, false);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean accessToken, boolean idToken,
                                                        boolean multivalued, boolean aggregateAttrs) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(name, userAttribute,
                tokenClaimName, claimType,
                accessToken, idToken,
                PROVIDER_ID);

        if (multivalued) {
            mapper.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
        }
        if (aggregateAttrs) {
            mapper.getConfig().put(ProtocolMapperUtils.AGGREGATE_ATTRS, "true");
        }

        return mapper;
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean accessToken, boolean idToken) {
        return createClaimMapper(name, userAttribute, tokenClaimName, claimType,
                accessToken, idToken, false, false);
    }

}