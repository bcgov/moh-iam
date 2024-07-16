package com.cgi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;


public class ThemeGeneratorTest
{

    @Test
    public void testExtractFolderName() {
        assertEquals("IDIR", ThemeGenerator.extractFolderName("IDIR : [\"idir\"]"));
        assertEquals("ALL", ThemeGenerator.extractFolderName("ALL : [\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\", \"fnha_aad\", \"bceid_business\", \"bcprovider_aad\", \"moh_idp\"]"));
        assertEquals("IDIR-HA", ThemeGenerator.extractFolderName("IDIR-HA : [\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\"]"));
        assertEquals("IDIR-IDIR_AAD", ThemeGenerator.extractFolderName("IDIR-IDIR_AAD : [\"idir\", \"idir_aad\"]"));
        assertEquals("NONE", ThemeGenerator.extractFolderName("NONE : []"));
        assertNull(ThemeGenerator.extractFolderName("Invalid line without colon"));
    }

    @Test
    public void testExtractIdpsToShow() {
        assertEquals("[\"idir\", \"idir_aad\"]", ThemeGenerator.extractIdpsToShow("IDIR : [\"idir\", \"idir_aad\"]"));
        assertEquals("[\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\", \"fnha_aad\", \"bceid_business\", \"bcprovider_aad\", \"moh_idp\"]", ThemeGenerator.extractIdpsToShow("ALL : [\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\", \"fnha_aad\", \"bceid_business\", \"bcprovider_aad\", \"moh_idp\"]"));
        assertEquals("[\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\"]", ThemeGenerator.extractIdpsToShow("IDIR-HA : [\"idir\", \"idir_aad\", \"phsa\", \"phsa_aad\"]"));
        assertEquals("[]", ThemeGenerator.extractIdpsToShow("NONE : []"));
        assertEquals("[]", ThemeGenerator.extractIdpsToShow("Invalid line without array"));
    }

}
