package ca.bc.gov.hlth.auth.provider.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverrideUserAttributeMapperTest {

    @Test
    public void testProtocolMapperFileContents() throws IOException {
        String filePath = "src/main/resources/META-INF/services/org.keycloak.protocol.ProtocolMapper";
        String expectedClassName = OverrideUserAttributeMapper.class.getCanonicalName();

        List<String> lines = Files.readAllLines(Paths.get(filePath));

        // There should be exactly one line matching the fully qualified class name
        assertEquals(1, lines.size(), "File should contain exactly one line");
        assertEquals(expectedClassName, lines.get(0), "The content of the file should match the expected class name");
    }

    @Test
    void getPriority() {
        assertEquals(10, new OverrideUserAttributeMapper().getPriority(), "Priority must come after built-in mappers with priority 0");
    }


    @Test
    void getId() {
        assertEquals("oidc-override-usermodel-attribute-mapper", new OverrideUserAttributeMapper().getId(), "ID must be overridden to not conflict with built-in mappers.");
    }
}