import com.intellij.openapi.project.Project;
import dictionary.*;

import java.io.File;
import java.util.*;

public class Dictionary {
    public VoiceToCode vc;
    public SpeechService sp;
    public Project project;
    public static boolean casematched=false;

    public static ArrayList<Integer> countbeforeundo=new ArrayList<Integer>();
    public static ArrayList<Integer> countafterundo=new ArrayList<Integer>();
    public static ArrayList<String> variabletypes= new ArrayList<String>();
    public static ArrayList<String> variablenames= new ArrayList<String>();
    File file;
    public boolean paused = false;
    final static ArrayList<String> wordstoconvert = new ArrayList<String>() {
        {
            add("open");
            add("close");
            add("capital");
            add("increment");
            add("decrement");
        }
    };
    static ArrayList<String> datatypes = new ArrayList<String>() {
        {
            add("int");
            add("String");
            add("boolean");
            add("double");
            add("float");
        }
    };
    public Dictionary(VoiceToCode voiceToCode, SpeechService sp)
    {
        this.sp = sp;
        this.vc = voiceToCode;
        project = voiceToCode.project;
    }

    public void generateCode(String response) {
        if (response.equalsIgnoreCase("restart")&& paused){
            paused = false;
            vc.notifyUser(VoiceToCode.RESTART_NOTIFICATION);
            sp.hasRecommended = false;
            sp.last_pause = System.currentTimeMillis();
            paused = false;
          //  sp.checked=false;
            casematched=true;
        }
        else if (response.equalsIgnoreCase("pause")&& !paused){
            paused = true;
            if(!sp.val)
                vc.notifyUser(VoiceToCode.NO_NET_NOTIFICATION);
            else
            vc.notifyUser(VoiceToCode.PAUSE_NOTIFICATION);
            casematched=true;
        }
        else if (!paused){
            if (response.startsWith("undo"))
            {
                vc.undo();
                casematched=true;
            }
            else if(response.startsWith("copy"))
            {
                String[] split=response.split(" ");
                String number = response.substring(response.indexOf("line") + 4).trim();
                if(split.length==3){
                    try {
                    vc.singlelinecopy(Integer.parseInt(number));//copy line 2
                    } catch (NumberFormatException nex){
                        vc.singlelinecopy(wordToNumber.convert(number));
                    }
                }
                else{
                    int start_index = response.indexOf("line") + 4;
                    int end_index = response.indexOf("to");
                    String start_line = response.substring(start_index, end_index).trim();
                    String end_line = response.substring(end_index+2).trim();
                    try {
                        vc.multiplelinecopy(Integer.parseInt(start_line),Integer.parseInt(end_line)); //copy line 2 to 4
                    } catch (NumberFormatException nex){
                        vc.multiplelinecopy(wordToNumber.convert(start_line), wordToNumber.convert(end_line));
                    }
                }

                casematched=true;
            }
            else if (response.startsWith("paste"))
            {
                vc.paste();
                casematched=true;
            }
            else if (wordstoconvert.contains(response.split(" ")[0].trim()))
            {
                String m=simpleconversion(response);
                if(!m.equals(""))
                {
                    vc.writeAnyText(m,"");
                    casematched=true;
                }
            }
            else if (datatypes.contains(simpleconversion(response.split(" ")[0]).trim()))
            {
                if(!variablehandler(response).equals(""))
                {
                    vc.writeAnyText(variablehandler(response), "assignment");
                    casematched=true;
                }
            }
            else if (response.equalsIgnoreCase("finish line")){
                vc.finishLine();
            }
            else if (response.startsWith("back space")||response.startsWith("backspace")) {
                vc.backspace();
                casematched=true;
            }
            else if (response.equalsIgnoreCase("tab")) {
                vc.tab();
                casematched=true;
            }
            else if (response.startsWith("output")||response.startsWith("out put"))
            {
                printhandler(response.substring(7,response.length()));
                casematched=true;
            }
            else if(response.startsWith("if"))
            {
                ifhandler(response.substring(3,response.length()));
                casematched=true;
            }
            else if(response.startsWith("Loop")||response.startsWith("loop"))
            {
                loophandler(response.substring(5,response.length()).split(" "));
                casematched=true;
            }
            else if (response.startsWith("create")) {
                receiveinput(response);
            } else if (response.equalsIgnoreCase("turn off")) {
                sp.stop_flag = true;
                casematched=true;
            } else if (response.equalsIgnoreCase("new line")){
                vc.newLine();
                casematched=true;
            } else if (response.startsWith("intellisense") || response.equalsIgnoreCase("dot")) {
                casematched=true;
                String[] s = response.split(" ");
                if (s.length > 1) {
                    try {
                        vc.chooseIntellisense(Integer.parseInt(s[1]));
                    } catch (Exception e) {
                        vc.chooseIntellisense(wordToNumber.convert(s[1]));
                    }
                } else vc.showIntellisense();
            }
            else if (response.startsWith("run")) {
                casematched=true;
                String[] s = response.split(" ");
                if (s.length > 1) {
                    try {
                        vc.showRunMenu();
                    } catch (Exception e) {
                        vc.showRunMenu();
                    }
                } else vc.runProgram();
            }
                else if (response.startsWith("go to")) {
                casematched=true;
                navigationhandler(response.substring(6, response.length()));
            } else if (response.equals("enter")) {
                casematched=true;
                vc.newLine();
            } else if (response.startsWith("comment")) {
                casematched=true;
                commentHandler(response, false);
            } else if (response.startsWith("uncomment"))
            {
                casematched=true;
                commentHandler(response, true);
            }
            else if(response.contains("equals")&&!response.startsWith("equals")&&!datatypes.contains(simpleconversion(response.split(" ")[0]).trim()))
            {
                String value=variableassignmenthandler(response);
                if(!value.equals(""))
                {
                    casematched=true;
                    vc.writeAnyText(value,"assignment");
                }
            }
            else if(!simpleconversion(response.split(" ")[0]).equals(response.split(" ")[0]))
            {
                    casematched=true;
                    vc.writeAnyText(simpleconversion(response),"");
            }
            else if(variablenames.contains(camelcasehandler(response)))
            {
                casematched=true;
                vc.writeAnyText(camelcasehandler(response),"");
            }
            else if (variablenames.contains(camelcasehandler(response).split("\\.")[0])){   // show intellisense when saying "a."
                String camelCasedResponse = camelcasehandler(response);
                vc.writeAnyText(camelCasedResponse,"");
                if (camelCasedResponse.endsWith(".")){
                    vc.showIntellisense();
                }
            }
            else if(SpeechService.hintPhrases.contains(response.split(" ")[0]))
            {
                casematched=true;
                vc.writeAnyText(simpleconversion(response),"");
            }
        }
    }

    public String simpleconversion(String response) {
        String[] seperate = response.split(" ");
        int m = 0;
        boolean nospace = false;
        StringBuilder result = new StringBuilder();
        for (; m < seperate.length; m++) {
            if (wordstoconvert.contains(seperate[m]))
            {
                result.append(new wordstosymbol().handler(seperate[m] + " "+seperate[m + 1])); //ch
                nospace = true;
                m++;
            } else
                result.append(new wordtosymbol().convert(seperate[m]));
            if (response.length()!=1&&m != seperate.length - 1 && new wordtosymbol().convert(seperate[m + 1]).equals(seperate[m + 1]) && new wordtosymbol().convert(seperate[m]).equals(seperate[m]) && !nospace && !wordstoconvert.contains(seperate[m + 1]))
            {
                result.append(" ");
            }
            nospace = false;
        }
        return result.toString();
    }

    private void navigationhandler(String response) {
        String[] split = response.split(" ");
        if (split[0].equalsIgnoreCase("next")) {
            if (split[1].equalsIgnoreCase("line"))
                vc.gotoNextLine();
            else if (split[1].equalsIgnoreCase("column"))
                vc.gotoNextColumn();
        } else if (split[0].equals("previous")) {
            if (split[1].equals("line"))
                vc.gotoPrevLine();
            else if (split[1].equals("column"))
                vc.gotoPrevColumn();
        } else if (split[0].equalsIgnoreCase("line")){
            int lineNumber;
            try {
                lineNumber = Integer.parseInt(split[1]);
            } catch (NumberFormatException nex){
                try {
                    lineNumber = wordToNumber.convert(split[1]);
                }
                catch (NullPointerException nex1){
                    casematched=false;
                    return;
                }
            }
            vc.gotoLine(lineNumber);
        }
        else if (split[0].equalsIgnoreCase("file")){
            String directoryname="";
            String filename = new wordstosymbol().conversion(split[1]);
            if(response.contains("in directory"))
                directoryname=split[4];
            vc.FileOpen(filename ,directoryname);
            datatypes.add(filename);
        }
    }

    private void receiveinput(String response) {
        String[] splitted = response.split(" ");
        if (splitted[1].equals("method")) {    //create method - start from index 14
            String[] words = new String[splitted.length - 2];
            System.arraycopy(splitted, 2, words, 0, splitted.length - 2);
            casematched=true;
            methodhandler(words);
        }
        else if (splitted[1].equals("class")) {    //create class - start from index 13
            casematched=true;
                vc.createClassWithName(camelcasehandler(response.substring(13)));

        } else if (splitted[1].equals("file")) {    //create file - start from index 10
            if(response.length()<=12)
                return;
            casematched=true;
            filehandler(response.substring(12, response.length()));
        }
        if (response.split(" ")[1].equals("directory")) {    //create directory - start from index 17
            if(response.length()<=17)
                return;
            casematched=true;
            directoryhandler(response.substring(17, response.length()));
        }
    }


    private void methodhandler(String[] seperatemethodresponse)
    {
        String resultmethod = "";
        int m = 0;
        boolean argumentsflag = false;
        int index = 0;    //create method public
        if(seperatemethodresponse[0].contains("main"))
        {
            resultmethod+="public static void main(String[] args)\n\t{\n\t}";
        }
        else {
            if (seperatemethodresponse[1].equalsIgnoreCase("static")) {
                resultmethod += simpleconversion(seperatemethodresponse[0]).trim() + " " +
                        simpleconversion(seperatemethodresponse[1]).trim() + " "
                        + simpleconversion(seperatemethodresponse[2]).trim() + " "
                        + simpleconversion(seperatemethodresponse[3]).trim();
                m = 4;
            } else {
                resultmethod += simpleconversion(seperatemethodresponse[0]).trim() + " "
                        + simpleconversion(seperatemethodresponse[1]).trim() + " "
                        + simpleconversion(seperatemethodresponse[2]).trim();
                m = 3;
            }
            index = m;
            for (; m < seperatemethodresponse.length; m++) {
                if (!seperatemethodresponse[m].equals("arguments")) {
                    resultmethod += new wordstosymbol().conversion(seperatemethodresponse[m]);
                    index++;
                } else {
                    index++;
                    StringBuilder sb = new StringBuilder();
                    for (int i = index; i < seperatemethodresponse.length; i++) {
                        sb.append(seperatemethodresponse[i]);
                        if (i != seperatemethodresponse.length - 1)
                            sb.append(" ");
                    }
                    String varresult = variablehandler(sb.toString());
                    resultmethod += "(" + varresult.substring(0, varresult.length() - 1) + ")\n\t{\n" + "\t}";
                    argumentsflag = true;
                    break;
                }
            }
            if (!argumentsflag) {
                resultmethod += "()\n\t{\n" + "\t}";
            }
        }
        vc.writeAnyText(resultmethod,"method");
    }

    private void loophandler(String[] seperateloopresponse) {
        StringBuilder resultloop = new StringBuilder();
        int response_length = seperateloopresponse.length;
        int m = 1;
        if (seperateloopresponse[0].equals("while")) {
            resultloop.append("while(");
            for (int i = 1; i < response_length - 1; i++) {
                resultloop.append(simpleconversion(seperateloopresponse[i]));
            }
            resultloop.append(seperateloopresponse[response_length - 1]);
            resultloop.append(")\n\t{\n\t}");
            //for int i 0 to 5 increment by 1
        } else {
            resultloop.append("for("); //loop i from 0 to 5
            if (seperateloopresponse[1].equalsIgnoreCase("from"))
            {
                resultloop.append( "int "+seperateloopresponse[0]+" = "+seperateloopresponse[2]+"; "+seperateloopresponse[0]);
                if(Integer.parseInt(seperateloopresponse[2])<Integer.parseInt(seperateloopresponse[4]))
                    resultloop.append(" < "+seperateloopresponse[4]+"; "+seperateloopresponse[0]+"++");
                else
                    resultloop.append(" > "+seperateloopresponse[4]+"; "+seperateloopresponse[0]+"--");
            }
            else {
                for (; m < seperateloopresponse.length; m++) {
                      if (wordstoconvert.contains(seperateloopresponse[m])) {
                        resultloop.append(new wordstosymbol().handler(seperateloopresponse[m] + seperateloopresponse[m + 1]));
                        m++;
                    } else
                        resultloop.append(new wordtosymbol().convert(seperateloopresponse[m]));
                    if (m != seperateloopresponse.length - 1 && new wordtosymbol().convert(seperateloopresponse[m + 1]).equals(seperateloopresponse[m + 1]) && new wordtosymbol().convert(seperateloopresponse[m]).equals(seperateloopresponse[m]) && !(seperateloopresponse[m].charAt(0) >= 48 && seperateloopresponse[m].charAt(0) <= 57))
                        resultloop.append(" ");
                    if ((seperateloopresponse[m].charAt(0) >= 48 && seperateloopresponse[m].charAt(0) <= 57 && m != seperateloopresponse.length - 1) || seperateloopresponse[m].equals("function"))
                        resultloop.append(";");
                }
            }
            resultloop.append(")\n\t\t{\n\t\t}");
        }
        vc.writeAnyText(resultloop.toString(),"loop");
    }

    private void ifhandler(String response) {
        String resultif = "if(";
        resultif += simpleconversion(response.replaceFirst("than ", ""));
        resultif += ")\n\t\t\t{\n\t\t\t}";
        vc.writeAnyText(resultif,"if");
    }

    private void printhandler(String response) {
        String[] split=response.split(" ");
        StringBuilder resultprint=new StringBuilder("System.out.println(");
        if(split[0].equalsIgnoreCase("string"))
        {
            resultprint.append("\"");
            resultprint.append(response.substring(7,response.length()));
            resultprint.append("\"");
        }
        else
            resultprint.append(camelcasehandler(response));
        resultprint.append(");");
        vc.writeAnyText(resultprint.toString(),"print");
        vc.newLine();
    }

    private void filehandler(String response) {
        String[] split=response.split(" ");
        String filename=new wordstosymbol().conversion(split[0])+".java";
        String directoryname="";
        if(response.contains(" in directory"))
            directoryname=split[3];
        vc.createFile(filename,directoryname);
        vc.FileOpen(filename.substring(0,filename.indexOf(".")),directoryname);
    }
    private void directoryhandler(String response) {
        vc.makerdir(response);
    }

    private String variablehandler(String response)
    {
        StringBuilder resultvariable = new StringBuilder();
        String[] split=response.split(" ");
        String variablename="";
        int m=2;
        if(split[1].equals("equals"))
            return "";
        resultvariable.append(simpleconversion(split[0]).trim()+" ");
        variablename+=simpleconversion(split[1]).trim();
        for(;m<split.length;m++)
        {
            if(!split[m].equalsIgnoreCase("equals"))
                    variablename+=new wordstosymbol().conversion(split[m]);
            else
                break;
        }
        if(!response.contains("equals"))
        {
            resultvariable.append(variablename);
            variabletypes.add(simpleconversion(split[0]).trim());
            variablenames.add(variablename);
        }
        else if(m!=split.length)
        {
            resultvariable.append(variablename);
            if(split[0].equalsIgnoreCase("String"))
                resultvariable.append(" = \"") ;
            else
                resultvariable.append(" = ") ;
            m++;
            variabletypes.add(simpleconversion(split[0]).trim());
            variablenames.add(variablename);
            for(;m<split.length-1;m++)
            {
                resultvariable.append(simpleconversion(split[m])+" ");
            }
            resultvariable.append(simpleconversion(split[m]));
            if(split[0].equalsIgnoreCase("String"))
                resultvariable.append("\"") ;
        }
        resultvariable.append(";");
        return resultvariable.toString();
    }

    private void commentHandler(String response, boolean uncomment){
        String[] splitted = response.split(" ");
        if (splitted[1].equals("line")){
            if (splitted.length > 2){
                int line;
                try {
                    line = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nex){
                    line = wordToNumber.convert(splitted[2]);
                }
                if (uncomment){
                    vc.uncommentLine(line);
                }
                else vc.commentLine(line);
            }
            else {
                if (uncomment){
                    vc.uncommentCurrentLine();
                }
                else vc.commentCurrentLine();
            }
        }
        else if (splitted[1].equals("here"))
        {
            vc.addComment(response.substring(13)); // comment here
        }
    }
    private String camelcasehandler(String response)
    {
        String[] split=response.split(" ");
        StringBuilder resultcamel=new StringBuilder();
        resultcamel.append(simpleconversion(split[0]));
        for(int i=1;i<split.length;i++)
        {
            resultcamel.append(new wordstosymbol().conversion(split[i]));
        }
        return resultcamel.toString();
    }
    private String variableassignmenthandler(String response)
    {
        int pos=response.indexOf("equals");
        String variablename=camelcasehandler(response.substring(0,pos-1));
        String value=camelcasehandler(response.substring(pos+7,response.length()));
        String type="";
        if(variablenames.contains(variablename))
        {
            for(int i=0;i<variablenames.size();i++)
            {
                if(variablenames.get(i).equals(variablename))
                {
                    type=variabletypes.get(i);
                    break;
                }
            }
            if(!variablenames.contains(value))
            {
                if(type.equalsIgnoreCase("String"))
                    return variablename + " = "+"\""+ simpleconversion(response.substring(pos+7,response.length()))+"\""+";";
            }
             return variablename + " = "+value+";";
        }
        return "";
    }
}
