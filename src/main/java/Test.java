import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.memstore.MemoryDocument;

public class Test {

    public static void main(String[] args) {
        for (String s : Utils.wordCombinations("Long John Silver")) {
            System.out.println(s);
        }
    }
}
