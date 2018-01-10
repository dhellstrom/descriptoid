import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.StreamUtils;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs the description extrator on a local file. Use by providing filename and mode as arguments. If mode is "existing"
 * it will look for the file in the "annotations" folder where already annotated documents are. If mode is "new" it will
 * look in the "corpus" folder and select an unnanotated document and send it to the Langforia API for annotation.
 * Depending on the file size annotation can take a lot of time.
 */
public class RunLocal {
	public static void main(String[] args) {
		try {


		    if (args.length == 2) {
                String fileName = args[0];
                String mode = args[1];

                Document doc = null;
                if (mode.equals("existing")) {
                    doc = DocforiaReader.readBinaryFile(Paths.get("annotations/" + fileName +".txt"));
                 } else if (mode.equals("new")) {
		            Novel novel = GutenbergReader.readNovel(Paths.get("corpus/" + fileName + ".txt"));
		            byte[] binary = HttpRequester.requestBinary(novel.getContent());
                    doc = MemoryDocument.fromBytes(binary);
                } else {
                    System.out.println("Invalid mode. Expected either 'existing' or 'new'.");
                    System.exit(1);
                }
                NovelProcessor processor = new NovelProcessor(doc);
                Set<String> uniqueNames = processor.extractUniqueNames();

                Map<String, List<Token>> descriptionMap = processor.extractDescriptions(uniqueNames);

                Map<String, Set<String>> clusters = processor.nameClusters(uniqueNames);

                processor.clusterNames(clusters, descriptionMap);

                BufferedWriter writer = Files.newBufferedWriter(Paths.get("descriptions/" + fileName + ".txt"));
                Set<String> sentences = new HashSet<>();
                for (Map.Entry<String, List<Token>> e : descriptionMap.entrySet()) {
                    if (e.getValue().size() > 0) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(e.getKey());
                        sb.append("\t");
                        Iterator<Token> descIterator = e.getValue().iterator();
                        while (descIterator.hasNext()) {
                            Token t = descIterator.next();
                            if (t.getCoarsePartOfSpeech().equals("NOUN")) {
                                Token desc = descIterator.next();
                                sb.append(desc.getLemma().toLowerCase() + "_" + t.toString().toLowerCase() + " ");
                            } else if(t.getLemma().equals("not")) {
                                Token adjective = descIterator.next();
                                sb.append(t.getLemma().toLowerCase() + "_" + adjective.getLemma().toLowerCase() + " ");
                            } else {
                                sb.append(t.toString().toLowerCase() + " ");
                            }
                            NodeTVar<Sentence> S = Sentence.var();
                            List<Sentence> sent = doc.select(S).where(S).covering(t)
                                    .stream()
                                    .map(StreamUtils.toNode(S))
                                    .collect(Collectors.toList());
                            sentences.add(sent.get(0).toString());
                        }
                        writer.write(sb.toString() + "\n");
                    }
                }

                writer.close();

                sentences.forEach(System.out::println);

            } else {
		        System.out.println("Wrong number of arguments. Expected two: file name and mode.");
            }

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
