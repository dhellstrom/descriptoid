import java.util.LinkedList;
import java.util.List;

public class Utils {

    /**
     * Generates all possible conbinations of words in the string.
     * @param s The string to generate combinations of.
     * @return A list containing the combinations.
     */
    public static List<String> wordCombinations(String s) {
        List<String> result = new LinkedList<>();
        String[] words = s.split(" ");
        int combinations = (int) Math.pow(words.length, 2);
        for (int i = 0; i < combinations; i++) {
            String binary = Integer.toBinaryString(i);
            while (binary.length() < words.length) {
                binary = '0' + binary;
            }
            StringBuffer combination = new StringBuffer();
            for (int j  = 0; j < words.length; j++) {
                if (binary.charAt(j) == '1') {
                    combination.append(words[j]);
                    combination.append(" ");
                }
            }
            result.add(combination.toString().trim());
        }
        result.remove("");
        return result;
    }
}
