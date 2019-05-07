package dictionary;

public class nofixedlengthwords
{
    String command;
    public nofixedlengthwords(String command)
    {
        this.command=command;
    }
    public String handlecase(String response)
    {
        if(command.equals("camel"))
        {
            return camelcasehandler(response);
        }
        return "";
    }

    private String camelcasehandler(String response)
    {
        return new wordstosymbol().conversion(response);
    }
}