package org.jboss.maven.plugins.qstools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    /**
     * Read Ignored quickstarts
     * 
     * @return
     * @throws IOException
     */
    public static List<String> readIgnoredFile() {
        List<String> result = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(".quickstarts_ignore"));
            while (br.ready()) {
                String line = br.readLine();
                result.add(line);
            }
        } catch (IOException e) {
            // No .quickstarts_ignore file found. Proceeding without one.
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
