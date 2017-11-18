import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AnnotateAndSave {

    public static void main(String[] args) {
        String fileName = "test";
        Novel novel = GutenbergReader.readNovel(Paths.get("corpus/" + fileName + ".txt"));
        String json = HttpRequester.requestJSON(novel.getContent());

        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get("annotations/" + fileName + ".txt"));
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
