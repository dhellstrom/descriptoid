import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class AnnotateAndSave {

    public static void main(String[] args) {
        String fileName = "pg3748.txt";
//        try (Stream<Path> paths = Files.walk(Paths.get("corpus/"))) {

//            paths.filter(Files::isRegularFile).map(s -> s.toString().split("\\\\")[1]).forEach(fileName -> {

                System.out.println(fileName);
                Novel novel = GutenbergReader.readNovel(Paths.get("corpus/" + fileName));
                byte[] binary = HttpRequester.requestBinary(novel.getContent());

                try {
                    OutputStream writer = Files.newOutputStream(Paths.get("annotations/" + fileName));
                    writer.write(binary);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                System.out.println("One file done, some more to go...");
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
