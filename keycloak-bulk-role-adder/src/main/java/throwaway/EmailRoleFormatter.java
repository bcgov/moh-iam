package throwaway;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EmailRoleFormatter {

    public static void main(String[] args) {
                try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\david.a.sharpe\\Desktop\\prod_admin_input.txt"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String output = line + "@bcp,ADMIN";
                System.out.println(output);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}