import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static Document readBinaryFile(Path path) {
        byte[] data = new byte[4096];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;

        try {
            InputStream stream = Files.newInputStream(path);
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return MemoryDocument.fromBytes(buffer.toByteArray());
    }
}
