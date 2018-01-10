import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.StreamUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used to create the initial seed dataset to be used in a machine learning classifier. Overwrites
 * the data in "ML_Dataset.txt", so do not use this if you have a good dataset in that file.
 */
public class MLDatasetCreator {
    public static void main(String[] args) {

        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get("ML_Dataset.txt"));
            Stream<Path> filePaths = Files.walk(Paths.get("annotations/"));

            int positives = 0;
            int negatives = 0;
            for (Path filePath : filePaths.filter(Files::isRegularFile).collect(Collectors.toList())) {

                Document doc = DocforiaReader.readBinaryFile(filePath);
                NovelProcessor processor = new NovelProcessor(doc);
                Set<String> uniqueNames = processor.extractUniqueNames();

                Map<Sentence, Integer> sentenceMap = processor.getMLSentences(uniqueNames);

                System.out.println(filePath.toString());

                for (Map.Entry<Sentence, Integer> e : sentenceMap.entrySet()) {

                    int value = e.getValue();
                    if (value == 1) {
                        positives++;
                    } else {
                        negatives++;
                    }

                    NodeTVar<Token> T = Token.var();
                    List<Token> tokens = doc.select(T).where(T).coveredBy(e.getKey()).stream().map(StreamUtils.toNode(T)).collect(Collectors.toList());

                    StringBuffer sb = new StringBuffer();
                    Set<DependencyRelation> deprels = new HashSet<>();
                    sb.append(e.getKey().toString() + " ");
                    for (Token t : tokens) {
                        sb.append(t.getPartOfSpeech() + " ");
                        deprels.addAll(t.connectedEdges(DependencyRelation.class).toList());
                    }

                    for (DependencyRelation deprel : deprels) {
                        sb.append(deprel.getRelation() + " ");
                    }

                    writer.write(sb.toString() + "::-::" + e.getValue() + "\n");
                }
            }

            System.out.println("Positives:" + positives + " Negatives: " + negatives);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
