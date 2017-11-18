import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeEdge;
import se.lth.cs.docforia.graph.text.*;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.PropositionGroup;
import se.lth.cs.docforia.query.QueryCollectors;
import se.lth.cs.docforia.query.StreamUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NovelProcessor {

    Document doc;
    private ArrayList<String> bodyParts;

    public NovelProcessor(Document doc) {
        this.doc = doc;
        readBodyParts();
    }

    /**
     * Returns a set of all named entities in the document.
     */
    public Set<String> extractUniqueNames() {

        NodeTVar<Token> T = Token.var();
        NodeTVar<NamedEntity> NE = NamedEntity.var();
        Stream<PropositionGroup> namedEntities = doc.select(T, NE) .where(T).coveredBy(NE)
                .stream()
                .collect(QueryCollectors.groupBy(doc, NE).orderByValue(T).collector())
                .stream();


        Set<String> uniqueNames = new HashSet<>();
        namedEntities.forEach(group -> {
            StringBuffer sb = new StringBuffer();
            group.forEach(token -> sb.append(token.get(T) + " "));
            String name = sb.toString().trim().toLowerCase();
            uniqueNames.add(name);
        });
        return uniqueNames;
    }

    /**
     * Extracts descriptions of the named enetiites in uniqueNames. Returns a map with names as keys
     * and a list containing Tokens for the descriptive words.
     *
     * The tokens are either adjectives or nouns. If it is a noun it will be a body part and the succeeding token will
     * be the description of that body part.
     */
    public Map<String, List<Token>> extractDescriptions(Set<String> uniqueNames) {

        NodeTVar<Token> T = Token.var();
        NodeTVar<CoreferenceChain> CC = CoreferenceChain.var();
        List<CoreferenceChain> chains = doc.select(CC)
                .stream()
                .sorted(StreamUtils.orderBy(CC))
                .map(StreamUtils.toNode(CC))
                .collect(Collectors.toList());

        Map<String, List<Token>> descriptionMap = new HashMap<>();

        for (CoreferenceChain chain : chains) {
            List<CoreferenceMention> mentions = chain.inboundNodes(CoreferenceMention.class).toList();
            String name = mostFrequentName(mentions, uniqueNames);
            if (name != null) {
                List<Token> description = new LinkedList<>();
                for (Node mention : mentions) {
                    List<Token> tokens = doc.select(T)
                            .where(T).coveredBy(mention)
                            .stream()
                            .map(StreamUtils.toNode(T))
                            .collect(Collectors.toList());
                    for (Token token : tokens) {
                        for (NodeEdge<Token, DependencyRelation> inbound : token.inboundNodeEdges(DependencyRelation.class, Token.class)) {
                            Token inboundNode = inbound.node();
                            DependencyRelation inboundEdge = inbound.edge();
                            /*If the inbound node is an adjective describing the entity*/
                            if (inboundNode.getCoarsePartOfSpeech().equals("ADJ") ) {
                                description.addAll(extractAdjectives(inboundNode));

                            /*If the inbound node is a body part we trý to find an adjective describing it*/
                            } else if (inboundNode.getCoarsePartOfSpeech().equals("NOUN") && inboundEdge.getRelation().equals("nmod:poss")
                                    && bodyParts.contains(inboundNode.getLemma())) {
                                    description.addAll(extractBodyPart(inboundNode));

                             /*If the inbound node is a verb */
                            } else if (inboundNode.getCoarsePartOfSpeech().equals("VERB")) {
                                /*If the verb is 'have' we see if the dobj is a body part and try to get the description of it*/
                                if (inboundNode.getLemma().equals("have")) {
                                    for (Token bodyPart : inboundNode.outboundNodes(Token.class)) {
                                        if (bodyPart.getCoarsePartOfSpeech().equals("NOUN") && bodyParts.contains(bodyPart.getLemma())) {
                                            description.addAll(extractMultipleBodyParts(bodyPart));
                                        }
                                    }
                                }
                                /*If it is some other word we try to find an adverb describing how it is done*/
//                                else {
//                                    for (Token verbDesc : inboundNode.outboundNodes(Token.class)) {
//                                        if (verbDesc.getPartOfSpeech().equals("RB")) {
//                                            description.append(verbDesc.getLemma() + " ");
//                                        }
//                                    }
//                                }
                            }
                        }
                    }
                }
                if (descriptionMap.containsKey(name)) {
                    descriptionMap.get(name).addAll(description);
                } else {
                    descriptionMap.put(name, description);
                }
            }
        }
        return descriptionMap;
    }


    private String mostFrequentName(List<? extends  Node> mentions, Set<String> uniqueNames) {
        Map<String, Integer> counter = new HashMap<>();
        for (Node n : mentions) {
            String mentioned = n.toString().toLowerCase();

            if (mentioned.endsWith("'s")) {
                mentioned = mentioned.substring(0, mentioned.length() - 2);
            }


            if (uniqueNames.contains(mentioned)) {
                if (counter.containsKey(mentioned)) {
                    counter.put(mentioned, counter.get(mentioned) + 1);
                } else {
                    counter.put(mentioned, 1);
                }
            }
        }
        if (counter.size() == 0) {
            return null;
        } else {
            Map.Entry<String, Integer> max = null;
            for (Map.Entry<String, Integer> entry : counter.entrySet()) {
                if (max == null || entry.getValue() > max.getValue()) {
                    max = entry;
                }
            }
            return max.getKey();
        }
    }

    public Map<String, Set<String>> nameClusters(Set<String> names) {
        List<String> multiWordNames = new ArrayList<>();
        Map<String, Set<String>> clusters = new HashMap<>();

        for (String name : names) {
            if (name.split(" ").length > 1) {
                multiWordNames.add(name);
            }
        }

        Set<String> coveredWords = new HashSet<>();

        for (String name : multiWordNames) {
            List<String> combinations = Utils.wordCombinations(name);
            for (String combination : combinations) {
                if (names.contains(combination) && !name.equals(combination)) {
                    coveredWords.add(combination);
                    if (clusters.containsKey(name)) {
                        clusters.get(name).add(combination);
                    } else {
                        Set<String> set = new HashSet<>();
                        set.add(name);
                        set.add(combination);
                        clusters.put(name, set);
                    }
                }
            }
        }

        for (String coveredName : coveredWords){
            clusters.remove(coveredName);
        }


        return clusters;
    }

    public void clusterNames(Map<String, Set<String>> nameClusters, Map<String, List<Token>> descriptionMap) {

        Set<String> descriptionKeys = descriptionMap.keySet();

        for (Map.Entry<String, Set<String>> entry : nameClusters.entrySet()) {
            Set<String> intersection = new HashSet<>(entry.getValue());
            intersection.retainAll(descriptionKeys);
            List<Token> description = new LinkedList<>();
            for (String name : intersection) {
                description.addAll(descriptionMap.get(name));
                descriptionMap.remove(name);
            }
            descriptionMap.put(entry.getKey(), description);
        }
    }

    /**
     * Reads body parts from a file and puts them in a list.
     */
    private void readBodyParts() {
        bodyParts = new ArrayList<>();
        try {
            Files.lines(Paths.get("resources/body_parts.txt")).forEach(line -> bodyParts.add(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Token> extractBodyPart(Token bodyPart) {

        List<Token> description = new LinkedList<>();

        for (Token bodyPartDesc : bodyPart.connectedNodes(Token.class)) {
            if (bodyPartDesc.getCoarsePartOfSpeech().equals("ADJ")) {
                extractAdjectives(bodyPartDesc).forEach(adjective -> {
                    description.add(bodyPart);
                    description.add(adjective);
                });
            }
        }
        return description;
    }

    private List<Token> extractMultipleBodyParts(Token bodyPart) {
        List<Token> description = new LinkedList<>();

        description.addAll(extractBodyPart(bodyPart));

        for (NodeEdge<Token, DependencyRelation> conj : bodyPart.outboundNodeEdges(DependencyRelation.class, Token.class)) {
            if (conj.edge().getRelation().equals("conj") && conj.node().getCoarsePartOfSpeech().equals("NOUN")
                    && bodyParts.contains(conj.node().getLemma())) {
                description.addAll(extractBodyPart(conj.node()));
            }
        }
        return description;
    }

    private List<Token> extractAdjectives(Token inboundNode) {

        List<Token> adjectives = new LinkedList<>();
        adjectives.add(inboundNode);

        /*Finds additional adjectives e.g 'thin' in "tall and thin"*/
        for (NodeEdge<Token, DependencyRelation> conj : inboundNode.outboundNodeEdges(DependencyRelation.class, Token.class)) {
            if (conj.edge().getRelation().equals("conj") && conj.node().getCoarsePartOfSpeech().equals("ADJ")) {
                adjectives.add(conj.node());
            }
        }
        return adjectives;
    }
}
