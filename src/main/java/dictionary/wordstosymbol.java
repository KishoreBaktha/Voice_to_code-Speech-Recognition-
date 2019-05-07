package dictionary;

public class wordstosymbol
{
    String output="";
    public String handler(String response)
    {
        if(response.contains("capital"))
            output=conversion(response.substring(7,response.length()));
        else if(response.contains("open"))
            output=openhandler(response.substring(5,response.length()));
        else if(response.contains("close"))
            output=closehandler(response.substring(6,response.length()));
        else if(response.contains("increment"))
            output=incrmenthandler(response.substring(9,response.length()));
        else if(response.contains("decrement"))
            output=decrementhandler(response.substring(9,response.length()));
        return output;
    }
    public String conversion(String response)
    {
        if(response.charAt(0)>=65&&response.charAt(0)<=90)
            return response;
        return (char)(response.charAt(0)-32)+response.substring(1,response.length());

    }
    public String openhandler(String response)
    {
        if(response.equalsIgnoreCase("bracket"))
            return "(";
        if(response.equalsIgnoreCase("braces"))
            return "{";
        return "";
    }
    public String closehandler(String response)
    {
        if(response.equalsIgnoreCase("bracket"))
            return ")";
        if(response.equalsIgnoreCase("braces"))
            return "}";
        return "";
    }
    public String incrmenthandler(String response)
    {
        return response+"++";

    }
    public String decrementhandler(String response)
    {
        return response+"--";
    }

}