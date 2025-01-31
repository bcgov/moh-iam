import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UsernameProcessor {

    // Map for known domain fixes
    private static final Map<String, String> domainFixes = new HashMap<>();

    static {
        domainFixes.put("interio", "interiorhealth.ca");
        domainFixes.put("norther", "northernhealth.ca");
        domainFixes.put("vancouv", "vancouverislandhealth.ca");
        // Add more domain mappings as needed
    }

    public static void main(String[] args) {
        // Directory containing the files
        String directoryPath = "C:\\Users\\david.a.sharpe\\Desktop\\PHSA Extract";

        // Output file path
        String outputPath = "corrected_usernames.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Process each .txt file in the directory
            Files.list(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt")) // Only process .txt files
                    .forEach(filePath -> {
                        System.out.println("Processing file: " + filePath.getFileName());
                        processFile(filePath, writer);
                    });

            System.out.println("Corrected usernames saved to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error accessing files: " + e.getMessage());
        }
    }

    private static void processFile(Path filePath, BufferedWriter writer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath.toFile()), "UTF-16"))) {
            String line;
            boolean isDataSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Write headers and blank lines as-is
                if (line.isEmpty() || line.startsWith("samaccountname")) {
                    writer.write(line);
                    writer.newLine();
                    isDataSection = true; // Start processing after the header
                    continue;
                }

                // Process data lines
                if (isDataSection) {
                    String correctedLine = fixLine(line);
                    writer.write(correctedLine);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + filePath.getFileName() + " - " + e.getMessage());
        }
    }

    private static String fixLine(String line) {
        // Split the line into `samaccountname` and `userprincipalname`
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2) {
            return line; // Return as-is if the format is invalid
        }

        String samaccountname = parts[0];
        String userprincipalname = parts[1];

        // Fix the userprincipalname domain
        String correctedUPN = fixTruncatedDomain(userprincipalname);

        // Return the corrected line
        return String.format("%-15s %s", samaccountname, correctedUPN); // Align `samaccountname` to 15 characters
    }

    private static String fixTruncatedDomain(String upn) {
        // Split into local part and domain
        String[] parts = upn.split("@", 2);
        if (parts.length < 2) {
            return upn; // Return as-is if no domain part
        }

        String localPart = parts[0];
        String domainPart = parts[1].replaceAll("\\.+$", ""); // Remove trailing dots

        // Fix the domain if it matches a known truncated domain
        for (String partial : domainFixes.keySet()) {
            if (domainPart.startsWith(partial)) {
                domainPart = domainFixes.get(partial);
                break;
            }
        }

        // Reconstruct the UPN
        return localPart + "@" + domainPart;
    }
}
