import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.StreamUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use with a filename present in the "test_annotations" folder. Creates a text file that can be used by the
 * python file "MLPredictor" to determine whether a sentence contains a description.
 */
public class MLInputGenerator {
    public static void main(String[] args) {

        if (args.length == 1) {
            String fileName = args[0];
            Document doc = DocforiaReader.readBinaryFile(Paths.get("annotations/" + fileName + ".txt"));
            NovelProcessor processor = new NovelProcessor(doc);
            Set<String> uniqueNames = processor.extractUniqueNames();

            uniqueNames.forEach(System.out::println);

            Map<String, List<Token>> entityMap = processor.getEntityMentions(uniqueNames);

            Map<String, Set<String>> clusters = processor.nameClusters(uniqueNames);

            processor.clusterNames(clusters, entityMap);

            try {
                BufferedWriter writer = Files.newBufferedWriter(Paths.get("MLInput.txt"));
                BufferedWriter writer2 = Files.newBufferedWriter(Paths.get("MLSentences.txt"));

                NodeTVar<Sentence> S = Sentence.var();
                for (Map.Entry<String, List<Token>> e : entityMap.entrySet()) {
                    String name = e.getKey();
                    List<Token> entities = e.getValue();

                    System.out.println(name);
                    writer.write("###" + name + "\n");
                    writer2.write("###" + name + "\n");

                    Set<Sentence> sentences = new HashSet<>();
                    for (Token entity : entities) {
                        Sentence sentence = doc.select(S).where(S).covering(entity)
                                .stream().map(StreamUtils.toNode(S)).collect(Collectors.toList()).get(0);
                        sentences.add(sentence);
                        System.out.println("Entity: " + entity.toString() + "\nSentence: " + sentence.toString());
                    }

                    for (Sentence sentence : sentences) {
                        NodeTVar<Token> T = Token.var();
                        List<Token> tokens = doc.select(T).where(T).coveredBy(sentence).stream().map(StreamUtils.toNode(T)).collect(Collectors.toList());

                        StringBuffer sb = new StringBuffer();
                        Set<DependencyRelation> deprels = new HashSet<>();
                        sb.append(sentence.toString() + " ");
                        for (Token t : tokens) {
                            sb.append(t.getPartOfSpeech() + " ");
                            deprels.addAll(t.connectedEdges(DependencyRelation.class).toList());
                        }

                        for (DependencyRelation deprel : deprels) {
                            sb.append(deprel.getRelation() + " ");
                        }
                        writer.write(sb.toString() + '\n');
                        writer2.write(sentence.toString() + '\n');
                    }
                }
                writer.close();
                writer2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Missing file name.");
        }
    }
}
