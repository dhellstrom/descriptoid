import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Used to read text from a file and save the annotated text in a file on binary Docforia format.
 */
public class AnnotateAndSave {

    public static void main(String[] args) {
        if (args.length == 1) {
            String fileName = args[0] + ".txt";

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
        } else {
            System.out.println("File name missing.");
        }
    }
}
