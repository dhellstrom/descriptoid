import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocforiaReader {

    public static Document readJsonFile(Path path) {
        StringBuffer sb = new StringBuffer();
        try {
            Files.lines(path).forEach(line -> sb.append(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return MemoryDocument.fromJson(sb.toString());
    }
}
