import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeEdge;
import se.lth.cs.docforia.graph.text.*;
import se.lth.cs.docforia.query.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NovelProcessor {

    Document doc;
    private ArrayList<String> bodyParts;
    private final String[] ignoreWordsArray = new String[]{"able", "own", "left", "right", "better", "due", "late", "sure", "same", "certain"};
    private final Set<String> ignoreWords = new HashSet<>(Arrays.asList(ignoreWordsArray));

    public NovelProcessor(Document doc) {
        this.doc = doc;
        readBodyParts();
    }

    /**
     * Returns a set of all named entities in the document.
     */
    public Set<String> extractUniqueNames() {

        Set<String> uniqueNames = new LinkedHashSet<>();

        NodeTVar<NamedEntity> NE = NamedEntity.var();
        List<NamedEntity> nes = doc.select(NE).stream().map(StreamUtils.toNode(NE)).collect(Collectors.toList());
        for (NamedEntity ne : nes) {
            if (ne.getLabel().equals("PERSON")) {
                uniqueNames.add(ne.toString().toLowerCase());
            }
        }

//        NodeTVar<Token> T = Token.var();
//        NodeTVar<NamedEntity> NE = NamedEntity.var();
//        Stream<PropositionGroup> namedEntities = doc.select(T, NE).where(T).coveredBy(NE)
//                .stream()
//                .collect(QueryCollectors.groupBy(doc, NE).orderByValue(T).collector())
//                .stream();
//
//
//        namedEntities.forEach(group -> {
//            StringBuffer sb = new StringBuffer();
//            group.forEach(token -> sb.append(token.get(T) + " "));
//            String name = sb.toString().trim().toLowerCase();
//            uniqueNames.add(name);
//        });
        return uniqueNames;
    }

    /**
     * Extracts descriptions of the named enetiites in uniqueNames. Returns a map with names as keys
     * and a list containing Tokens for the descriptive words.
     * <p>
     * The tokens are either adjectives or nouns. If it is a noun it will be a body part and the succeeding token will
     * be the description of that body part.
     */
    public Map<String, List<Token>> extractDescriptions(Set<String> uniqueNames) {

        Map<String, List<Token>> descriptionMap = new LinkedHashMap<>();

        NodeTVar<Token> T = Token.var();
        List<CoreferenceChain> chains = getCoreferenceChains();

        Set<Token> usedEntities = new HashSet<>();
        Set<Token> usedDescriptions = new HashSet<>();

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
                        if (!usedEntities.contains(token)) {
                            usedEntities.add(token);
                            description.addAll(getDescriptions(token, usedDescriptions));
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

        NodeTVar<NamedEntity> NE = NamedEntity.var();
        List<Token> namedEntities = doc.select(T, NE).where(T).coveredBy(NE)
                .stream()
                .map(StreamUtils.toNode(T))
                .collect(Collectors.toList());

        for (Token t : namedEntities) {
            if (!usedEntities.contains(t)) {
                List<Token> description = getDescriptions(t, usedDescriptions);
                if (description.size() > 0) {
                    if (descriptionMap.containsKey(t.toString().toLowerCase())) {
                        descriptionMap.get(t.toString().toLowerCase()).addAll(description);
                    } else {
                        descriptionMap.put(t.toString().toLowerCase(), description);
                    }
                }
            }
        }

        return descriptionMap;
    }


    public Map<Sentence, Integer> getMLSentences(Set<String> uniqueNames) {
        Random rand = new Random(42);
        Map<Sentence, Integer> sentenceMap = new HashMap<>();


        List<CoreferenceChain> chains = getCoreferenceChains();

        Set<Token> usedEntities = new HashSet<>();
        Set<Token> usedDescriptions = new HashSet<>();

        NodeTVar<Token> T = Token.var();
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
                        if (!usedEntities.contains(token)) {
                            usedEntities.add(token);
                            int prevSize = description.size();
                            description.addAll(getDescriptions(token, usedDescriptions));
                        NodeTVar<Sentence> S = Sentence.var();
                        List<Sentence> sentences = doc.select(S).where(S).covering(token)
                                .stream()
                                .map(StreamUtils.toNode(S))
                                .collect(Collectors.toList());

                            if (description.size() != prevSize) {
                                sentenceMap.put(sentences.get(0), 1);
                            } else if (rand.nextDouble() < 0.035) {
                                sentenceMap.put(sentences.get(0), 0);
                            }
                        }
                    }
                }
            }
        }

        NodeTVar<NamedEntity> NE = NamedEntity.var();
        List<Token> namedEntities = doc.select(T, NE).where(T).coveredBy(NE)
                .stream()
                .map(StreamUtils.toNode(T))
                .collect(Collectors.toList());

        for (Token t : namedEntities) {
            if (!usedEntities.contains(t)) {
                List<Token> description = getDescriptions(t, usedDescriptions);
                NodeTVar<Sentence> S = Sentence.var();
                List<Sentence> sentences = doc.select(S).where(S).covering(t)
                        .stream()
                        .map(StreamUtils.toNode(S))
                        .collect(Collectors.toList());
                if (description.size() > 0) {
                    sentenceMap.put(sentences.get(0), 1);
                } else if (rand.nextDouble() < 0.035) {
                    sentenceMap.put(sentences.get(0), 0);
                }
            }
        }
        return sentenceMap;
    }

    /**
     *  Finds all mentions that can be linked to a named entity and returns them as a map
     *  where the key is the entity name and the value is a list containing the Tokens corresponding
     *  to mentions of that entity.
     */
    public Map<String, List<Token>> getEntityMentions(Set<String> uniqueNames) {

        Map<String, List<Token>> entityMap = new HashMap<>();

        NodeTVar<Token> T = Token.var();
        List<CoreferenceChain> chains = getCoreferenceChains();

        Set<Token> usedEntities = new HashSet<>();

        for (CoreferenceChain chain : chains) {
            List<CoreferenceMention> mentions = chain.inboundNodes(CoreferenceMention.class).toList();
            String name = mostFrequentName(mentions, uniqueNames);
            if (name != null) {
                List<Token> entityTokens = new LinkedList<>();
                for (CoreferenceMention mention : mentions) {

                    List<Token> tokens = doc.select(T).where(T).coveredBy(mention)
                            .stream().map(StreamUtils.toNode(T)).collect(Collectors.toList());
                    entityTokens.addAll(tokens);
                    usedEntities.addAll(tokens);
                }

                if (entityMap.containsKey(name)) {
                    entityMap.get(name).addAll(entityTokens);
                } else {
                    entityMap.put(name, entityTokens);
                }
            }
        }

        NodeTVar<NamedEntity> NE = NamedEntity.var();
        List<Token> namedEntities = doc.select(T, NE).where(T).coveredBy(NE)
                .stream()
                .map(StreamUtils.toNode(T))
                .collect(Collectors.toList());

        for (Token t : namedEntities) {
            if (!usedEntities.contains(t)) {
                if (entityMap.containsKey(t.toString().toLowerCase())) {
                    entityMap.get(t.toString().toLowerCase()).add(t);
                } else {
                    List<Token> list = new LinkedList<>();
                    list.add(t);
                    entityMap.put(t.toString().toLowerCase(), list);
                }
            }
        }

        return entityMap;
    }

    private List<Token> getDescriptions(Token token, Set<Token> usedDescriptions) {
        List<Token> description = new LinkedList<>();
        for (NodeEdge<Token, DependencyRelation> inbound : token.inboundNodeEdges(DependencyRelation.class, Token.class)) {
            Token inboundNode = inbound.node();
            DependencyRelation inboundEdge = inbound.edge();
                            /*If the inbound node is an adjective describing the entity*/
            if (inboundNode.getCoarsePartOfSpeech().equals("ADJ") && inboundEdge.getRelation().equals("nsubj")) {
                description.addAll(extractAdjectives(inboundNode, usedDescriptions));

                            /*If the inbound node is a body part we trý to find an adjective describing it*/
            } else if (inboundNode.getCoarsePartOfSpeech().equals("NOUN") && inboundEdge.getRelation().equals("nmod:poss")
                    && bodyParts.contains(inboundNode.getLemma())) {
                description.addAll(extractBodyPart(inboundNode, usedDescriptions));

                             /*If the inbound node is a verb */
            } else if (inboundNode.getCoarsePartOfSpeech().equals("VERB")) {
                                /*If the verb is 'have' we see if the dobj is a body part and try to get the description of it*/
                if (inboundNode.getLemma().equals("have")) {
                    for (Token bodyPart : inboundNode.outboundNodes(Token.class)) {
                        if (bodyPart.getCoarsePartOfSpeech().equals("NOUN") && bodyParts.contains(bodyPart.getLemma())) {
                            description.addAll(extractMultipleBodyParts(bodyPart, usedDescriptions));
                        }
                    }
                } else if (inboundEdge.getRelation().equals("nsubj")) {
                    for (Token possibleAdj : inboundNode.outboundNodes(Token.class)) {
                        if (possibleAdj.getCoarsePartOfSpeech().equals("ADJ")) {
                            description.addAll(extractAdjectives(possibleAdj, usedDescriptions));
                        }
                    }
                }
            }
        }
        return description;
    }


    private String mostFrequentName(List<? extends Node> mentions, Set<String> uniqueNames) {
        Map<String, Integer> counter = new LinkedHashMap<>();
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
        Map<String, Set<String>> clusters = new LinkedHashMap<>();

        for (String name : names) {
            if (name.split(" ").length > 1) {
                multiWordNames.add(name);
            }
        }

        Set<String> coveredWords = new LinkedHashSet<>();

        for (String name : multiWordNames) {
            List<String> combinations = Utils.wordCombinations(name);
            for (String combination : combinations) {
                if (names.contains(combination) && !name.equals(combination)) {
                    coveredWords.add(combination);
                    if (clusters.containsKey(name)) {
                        clusters.get(name).add(combination);
                    } else {
                        Set<String> set = new LinkedHashSet<>();
                        set.add(name);
                        set.add(combination);
                        clusters.put(name, set);
                    }
                }
            }
        }

        for (String coveredName : coveredWords) {
            clusters.remove(coveredName);
        }


        return clusters;
    }

    public void clusterNames(Map<String, Set<String>> nameClusters, Map<String, List<Token>> descriptionMap) {

        Set<String> descriptionKeys = descriptionMap.keySet();

        for (Map.Entry<String, Set<String>> entry : nameClusters.entrySet()) {
            Set<String> intersection = new LinkedHashSet<>(entry.getValue());
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

    private List<Token> extractBodyPart(Token bodyPart, Set<Token> usedDescriptions) {

        List<Token> description = new LinkedList<>();

        for (Token bodyPartDesc : bodyPart.connectedNodes(Token.class)) {
            if (bodyPartDesc.getCoarsePartOfSpeech().equals("ADJ")) {
                extractAdjectives(bodyPartDesc, usedDescriptions).forEach(adjective -> {
                    if (!usedDescriptions.contains(adjective)) {
                        description.add(bodyPart);
                        description.add(adjective);
                        usedDescriptions.add(adjective);
                    }
                });
            }
        }
        return description;
    }

    private List<Token> extractMultipleBodyParts(Token bodyPart, Set<Token> usedDescriptions) {
        List<Token> description = new LinkedList<>();

        description.addAll(extractBodyPart(bodyPart, usedDescriptions));

        for (NodeEdge<Token, DependencyRelation> conj : bodyPart.outboundNodeEdges(DependencyRelation.class, Token.class)) {
            if (conj.edge().getRelation().equals("conj") && conj.node().getCoarsePartOfSpeech().equals("NOUN")
                    && bodyParts.contains(conj.node().getLemma())) {
                description.addAll(extractBodyPart(conj.node(), usedDescriptions));
            }
        }
        return description;
    }

    private List<Token> extractAdjectives(Token inboundNode, Set<Token> usedDescriptions) {

        List<Token> adjectives = new LinkedList<>();
        addAdjective(inboundNode, adjectives, usedDescriptions);

        /*Finds additional adjectives e.g 'thin' in "tall and thin"*/
        for (NodeEdge<Token, DependencyRelation> conj : inboundNode.outboundNodeEdges(DependencyRelation.class, Token.class)) {
            if (conj.edge().getRelation().equals("conj") && conj.node().getCoarsePartOfSpeech().equals("ADJ")) {
                addAdjective(conj.node(), adjectives, usedDescriptions);
            }
        }
        return adjectives;
    }

    private void addAdjective(Token adjective, List<Token> adjectives, Set<Token> usedDescriptions) {
        if (!ignoreWords.contains(adjective.getLemma()) && !usedDescriptions.contains(adjective)) {
            Token negation = null;
            for (NodeEdge<Token, DependencyRelation> ne : adjective.outboundNodeEdges(DependencyRelation.class, Token.class)) {
                if (ne.edge().getRelation().equals("neg")) {
                    negation = ne.node();
                    negation.setLemma("not");
                    break;
                }
            }
            if (negation != null) {
                adjectives.add(negation);
            }
            adjectives.add(adjective);
            usedDescriptions.add(adjective);
        }
    }

    private List<CoreferenceChain> getCoreferenceChains() {
        NodeTVar<Token> T = Token.var();
        NodeTVar<CoreferenceChain> CC = CoreferenceChain.var();
        return doc.select(CC)
                .stream()
                .sorted(StreamUtils.orderBy(CC))
                .map(StreamUtils.toNode(CC))
                .collect(Collectors.toList());
    }
}
