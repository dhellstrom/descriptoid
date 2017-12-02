import org.rapidoid.gui.Btn;
import org.rapidoid.gui.GUI;
import org.rapidoid.gui.GUIActions;
import org.rapidoid.gui.input.Form;
import org.rapidoid.html.Tag;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.graph.text.CoreferenceMention;
import se.lth.cs.docforia.graph.text.NamedEntity;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.StreamUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
	public static void main(String[] args) {
		try {

//            On.post("/").json((Req req) -> {
//                System.out.println("Received post with data: " + req.data().keySet().iterator().next());
//                byte[] binary = HttpRequester.requestBinary((String) req.data().keySet().iterator().next());
//                Document doc = MemoryDocument.fromBytes(binary);
////            Document doc = DocforiaReader.readJsonFile(Paths.get("annotations/" + fileName +".txt"));
//                NovelProcessor processor = new NovelProcessor(doc);
//                Set<String> uniqueNames = processor.extractUniqueNames();
//
//                Map<String, List<Token>> descriptionMap = processor.extractDescriptions(uniqueNames);
//
//                Map<String, Set<String>> clusters = processor.nameClusters(uniqueNames);
//
//                processor.clusterNames(clusters, descriptionMap);
//
//                Map<String, List<Map<String, String>>> returnMap = new HashMap<>();
//                for (Map.Entry<String, List<Token>> entry : descriptionMap.entrySet()) {
//                    String name = entry.getKey();
//                    List<Map<String, String>> descriptions = new LinkedList<>();
//                    for (Token token : entry.getValue()) {
//                        Map<String, String> tokenMap = new HashMap<>();
//                        tokenMap.put("form", token.toString());
//                        tokenMap.put("lemma", token.getLemma());
//                        tokenMap.put("coarsePartOfSpeech", token.getCoarsePartOfSpeech());
//                        tokenMap.put("partOfSpeech", token.getPartOfSpeech());
//
//                        NodeTVar<Sentence> S = Sentence.var();
//                        List<Sentence> sentences = doc.select(S).where(S).covering(token)
//                                .stream()
//                                .map(StreamUtils.toNode(S))
//                                .collect(Collectors.toList());
//                        tokenMap.put("sentence", sentences.get(0).toString());
//                        descriptions.add(tokenMap);
//                    }
//                    returnMap.put(name, descriptions);
//                }
//
//                Resp resp = req.response();
//                resp.header("Access-Control-Allow-Origin", "*");
//                resp.result(returnMap);
//                return resp;
//            });

            String fileName = "warbreaker1";
//		    Novel novel = GutenbergReader.readNovel(Paths.get("corpus/" + fileName + ".txt"));
//		    byte[] binary = HttpRequester.requestBinary(novel.getContent());
//            Document doc = MemoryDocument.fromBytes(binary);
            Document doc = DocforiaReader.readBinaryFile(Paths.get("annotations/" + fileName +".txt"));
            NovelProcessor processor = new NovelProcessor(doc);
            Set<String> uniqueNames = processor.extractUniqueNames();

            Map<String, List<Token>> descriptionMap = processor.extractDescriptions(uniqueNames);

            Map<String, Set<String>> clusters = processor.nameClusters(uniqueNames);

            processor.clusterNames(clusters, descriptionMap);



            BufferedWriter writer = Files.newBufferedWriter(Paths.get("descriptions/" + fileName + ".txt"));
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
//                        NodeTVar<Sentence> S = Sentence.var();
//                        List<Sentence> sentences = doc.select(S).where(S).covering(t)
//                                .stream()
//                                .map(StreamUtils.toNode(S))
//                                .collect(Collectors.toList());
//                        sb.append(sentences.get(0) + " ");
                    }
                    writer.write(sb.toString() + "\n");
                }
            }

            writer.close();

//
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
