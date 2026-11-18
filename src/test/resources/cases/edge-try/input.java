package edge;

import java.io.BufferedReader;
import java.io.IOException;

public class EdgeTry {
    public String read(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException | IllegalStateException e) {
            return e.getMessage();
        } finally {
            System.out.println("done");
        }
    }

    public void close(BufferedReader reader) throws IOException {
        try (reader) {
            System.out.println(reader.readLine());
        }
    }
}
