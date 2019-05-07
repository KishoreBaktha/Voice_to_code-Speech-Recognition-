package dictionary;

import java.util.HashMap;

public class wordToNumber {

    static HashMap<String, Integer> numbers= new HashMap<String, Integer>();

    static {
        numbers.put("zero", 0);
        numbers.put("one", 1);
        numbers.put("two", 2);
        numbers.put("to", 2);   // google api cannot distinguish between "to" and "two"
        numbers.put("three", 3);
        numbers.put("for", 4);
        numbers.put("four", 4);
        numbers.put("five", 5);
        numbers.put("six", 6);
        numbers.put("seven", 7);
        numbers.put("eight", 8);
        numbers.put("nine", 9);
        numbers.put("ten", 10);
        numbers.put("eleven", 11);
        numbers.put("twelve", 12);
        numbers.put("thirteen", 13);
        numbers.put("fourteen", 14);
        numbers.put("fifteen", 15);
        numbers.put("sixteen", 16);
        numbers.put("seventeen", 17);
        numbers.put("eighteen", 18);
        numbers.put("nineteen", 19);
        numbers.put("twenty", 20);
        numbers.put("twenty one", 21);
        numbers.put("twenty two", 22);
        numbers.put("twenty three", 23);
        numbers.put("twenty four", 24);
        numbers.put("twenty five", 25);
        numbers.put("twenty six", 26);
        numbers.put("twenty seven", 27);
        numbers.put("twenty eight", 28);
    }

    public static int convert(String input) {
        return numbers.get(input);
    }

}