package pdbexplorer.model;

import javafx.concurrent.Task;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonString;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class contains to tasks. One gets a list of all PDB files currently contained on the pdb website. The other one
 * downloads a specific PDB file.
 */
public class PDBWebClient {
    /**
     * This task gets a list of all PDB files currently held at the PDB website and returns it as an ArrayList.
     */
    public static class GetList extends Task<ArrayList<String>> {
        /**
         * Constructor of the GetList task.
         */
        public GetList() {}
        @Override
        public ArrayList<String> call() throws IOException {
            ArrayList<String> output = new ArrayList<>();
            URL url = new URL("https://data.rcsb.org/rest/v1/holdings/current/entry_ids");
            try (var r = Json.createReader(getFromURL(url))) {
                JsonArray text = r.readArray();
                int total = text.size();
                for (int i = 0; i < total; i++) {
                    output.add(((JsonString) text.get(i)).getString());
                    updateProgress(i, total);
                }
            }
            Collections.sort(output);
            return output;
        }
    }

    /**
     * This task downloads a specified PDB file and returns it in String format.
     */
    public static class GetPDBFile extends Task<String> {
        private final String input;

        /**
         * Constructor for the GetPDBFile task.
         * @param input (String): the four-letter name of the PDB file
         */
        public GetPDBFile(String input) {
            this.input = input.toLowerCase();
        }

        @Override
        public String call() throws IOException {
            try {
                URL url = new URL("https://files.rcsb.org/download/" + input + ".pdb");
                return new String(getFromURL(url).readAllBytes());
            } catch (Exception e) {
                throw new IOException();
            }
        }
    }

    /**
     * Returns an InputStream for the given URL.
     * @param url (URL): the specified URL from which data should be retrieved
     * @return InputStream
     * @throws IOException in case of errors
     */
    public static InputStream getFromURL(URL url) throws IOException {
        var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getInputStream();
    }

}
