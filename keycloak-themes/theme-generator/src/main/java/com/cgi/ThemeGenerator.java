package com.cgi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ThemeGenerator
{
    public static void main( String[] args )
    {
        try {
            // Paths for the input file, template folder, and output folder
            Path inputFilePath = Paths.get(ThemeGenerator.class.getResource("/theme-input.txt").toURI());
            Path templateFolderPath = Paths.get(ThemeGenerator.class.getResource("/theme-template").toURI());
            Path outputFolderPath = Paths.get(ThemeGenerator.class.getResource("/").toURI()).resolve("theme-output");

            // Read all lines from the input file
            List<String> lines = Files.readAllLines(inputFilePath);

            for (String line : lines) {
                String folderName = extractFolderName(line);
                String idpsToShow = extractIdpsToShow(line);
                if (folderName != null && !folderName.isEmpty()) {
                    // Create the destination path
                    Path destination = outputFolderPath.resolve(folderName);
                    // Copy the template folder to the destination
                    copyFolder(templateFolderPath.toFile(), destination.toFile());
                    System.out.println("Copied " + templateFolderPath + " to " + destination);
                    updateScriptJs(destination.resolve("login/resources/js/script.js"), idpsToShow);
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /*
    Folder name regex:
    ^ Asserts the start of the line.
    [\\w-]+ This part allows one or more occurrences of word characters (\\w, which includes letters, digits, and underscores) and the hyphen -.
    \\s* Matches zero or more whitespace characters (spaces, tabs, etc.).
    : Matches a literal colon : character.
     */
    protected static String extractFolderName(String line) {
        Pattern pattern = Pattern.compile("^([\\w-]+)\\s*:");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /*
    IDP list regex:
    : Matches a literal colon : character.
    \\s* Matches zero or more whitespace characters (spaces, tabs, etc.).
    (\\[.*\\])
        \\[ Matches a literal opening square bracket [.
        .* Matches any character (except newline) zero or more times.
        \\] Matches a literal closing square bracket ].
     */
    protected static String extractIdpsToShow(String line) {
        Pattern pattern = Pattern.compile(":\\s*(\\[.*\\])");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "[]";
    }

    private static void copyFolder(File source, File destination) throws IOException {
        FileUtils.copyDirectory(source, destination);
    }

    private static void updateScriptJs(Path scriptJsPath, String idpsToShow) throws IOException {
        List<String> lines = Files.readAllLines(scriptJsPath);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("const IDPS_TO_SHOW = []")) {
                lines.set(i, "const IDPS_TO_SHOW = " + idpsToShow + ";");
                break;
            }
        }
        Files.write(scriptJsPath, lines);
    }
}
