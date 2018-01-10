import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Used to read a plaintext file.
 */
public class GutenbergReader {

    public static Novel readNovel(Path path) {
        StringBuffer sb = new StringBuffer();
        try {
            Files.lines(path).forEach(line -> sb.append(line + ' '));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Novel("Title", sb.toString());
    }
}
