package dictionary;

public class wordtosymbol
{
    private String output;
    public String convert(String response)
    {
        switch(response.toLowerCase()){
            case "comma": output=","; break;
            case "integer": output="int "; break;
            case "scanner": output=" Scanner "; break;
            case "system": output="System"; break;
            case "java": output="java"; break;
            case "java.": output=" java."; break;
            case "io.": output="io. "; break;
            case "dot": output="."; break;
            case "plus": output="+"; break;
            case "minus": output="-"; break;
            case "multiply": output="*"; break;
            case "divide": output="/"; break;
            case "star": output="*"; break;
            case "space": output=" "; break;
            case "and": output="&&"; break;
            case "not": output="!"; break;
            case "or": output="||"; break;
            case "enter": output="\n"; break;
            case "colon": output=":"; break;
            case "array": output="[]"; break;
            case "string": output="String "; break;
            case "semicolon": output=";"; break;
            case "equals": output=" = "; break;
            case "lesser": output=" < "; break;
            case "greater": output=" > "; break;
            case "quote": output="'"; break;
            case "function": output="()"; break;
            case "quotes": output="\""; break;
            default: output = response; break;
        }
        return output;
    }
}