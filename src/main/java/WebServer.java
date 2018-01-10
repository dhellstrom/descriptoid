import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.On;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.StreamUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WebServer {
    public static void main(String[] args) {
        try {

            On.post("/").json((Req req) -> {
                System.out.println("Received post with data: " + req.data().keySet().iterator().next());
                byte[] binary = HttpRequester.requestBinary((String) req.data().keySet().iterator().next());
                Document doc = MemoryDocument.fromBytes(binary);
                NovelProcessor processor = new NovelProcessor(doc);
                Set<String> uniqueNames = processor.extractUniqueNames();

                Map<String, List<Token>> descriptionMap = processor.extractDescriptions(uniqueNames);

                Map<String, Set<String>> clusters = processor.nameClusters(uniqueNames);

                processor.clusterNames(clusters, descriptionMap);

                Map<String, List<Map<String, String>>> returnMap = new HashMap<>();
                for (Map.Entry<String, List<Token>> entry : descriptionMap.entrySet()) {
                    String name = entry.getKey();
                    List<Map<String, String>> descriptions = new LinkedList<>();
                    for (Token token : entry.getValue()) {
                        System.out.println(token.toString());
                        Map<String, String> tokenMap = new HashMap<>();
                        tokenMap.put("form", token.toString());
                        tokenMap.put("lemma", token.getLemma());
                        tokenMap.put("coarsePartOfSpeech", token.getCoarsePartOfSpeech());
                        tokenMap.put("partOfSpeech", token.getPartOfSpeech());

                        NodeTVar<Sentence> S = Sentence.var();
                        List<Sentence> sentences = doc.select(S).where(S).covering(token)
                                .stream()
                                .map(StreamUtils.toNode(S))
                                .collect(Collectors.toList());
                        tokenMap.put("sentence", sentences.get(0).toString());
                        descriptions.add(tokenMap);
                    }
                    returnMap.put(name, descriptions);
                }

                Resp resp = req.response();
                resp.header("Access-Control-Allow-Origin", "*");
                resp.result(returnMap);
                return resp;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

