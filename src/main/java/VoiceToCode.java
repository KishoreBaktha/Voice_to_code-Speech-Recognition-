
import com.intellij.lang.java.JavaLanguage;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

public class VoiceToCode extends Thread {
    private SpeechService sp;
    public Project project;
    private Editor editor;
    private CaretModel caretModel;
    private PsiElementFactory elementFactory;
    private PsiFile current_file;
    private Robot robot;
    private boolean showingIntellisense;
    private boolean showingRunMenu=false;
    final ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    public static final Notification START_NOTIFICATION = new Notification("", "Voice-to-code has started", "Your microphone is now starting to record.", NotificationType.INFORMATION);
    public static final Notification NO_NET_NOTIFICATION = new Notification("", "No Internet", "Check your network connection. Voice-to-code has stopped.", NotificationType.INFORMATION);
    public static final Notification PAUSE_NOTIFICATION = new Notification("", "Voice-to-code has paused", "You will still be recorded but Voice-to-code will be inactive. Restart by saying \"restart\".", NotificationType.INFORMATION);
    public static final Notification RESTART_NOTIFICATION = new Notification("", "Voice-to-code has restarted", "Voice-to-code is ready to go again!", NotificationType.INFORMATION);
    public static final Notification STOP_NOTIFICATION = new Notification("", "Stopped", "Voice-to-code has stopped.", NotificationType.INFORMATION);

    String copiedText ="";

    public VoiceToCode(@NotNull AnActionEvent event)
    {
        project = event.getProject();
        editor = event.getData(PlatformDataKeys.EDITOR);
        current_file = event.getData(LangDataKeys.PSI_FILE);
        elementFactory = JavaPsiFacade.getElementFactory(project);
        caretModel = editor.getCaretModel();
        //System.out.println("size is "+Dictionary.variablenames.size());
        getVariables();
        try {
            robot = new Robot();
        } catch (AWTException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
//        System.out.println("Voice-to-code started!");
        notifyUser(START_NOTIFICATION);
        sp = new SpeechService();
        Dictionary dictionary = new Dictionary(this, sp);
        try {
            sp.startListening(dictionary);
        } catch (Exception e)
        {
        }
        if(!sp.val)
        {
            notifyUser(NO_NET_NOTIFICATION);
            SpeechService.checkforrestart();
        }
        else
            notifyUser(STOP_NOTIFICATION);
    }

    public void notifyUser(Notification n){
        n.notify(project);
    }

    private PsiClass getCurrentPsiClass() {
        if (current_file == null || editor == null) {
            return null;
        }
        PsiElement elementAt = getCurrentPsiElement();
        final PsiClass[] psiClass = new PsiClass[1];
        ApplicationManager.getApplication().runReadAction(() -> {
            psiClass[0] = PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
        });
        return psiClass[0];
    }

    public PsiElement getCurrentPsiElement() {
        final PsiElement[] psiElement = new PsiElement[1];
        ApplicationManager.getApplication().runReadAction(() -> {
            psiElement[0] = current_file.findElementAt(caretModel.getOffset());
        });
        return psiElement[0];
    }

    public void writeStatement(String s) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Dictionary.countbeforeundo.add(caretModel.getOffset());
            PsiClass psiClass = getCurrentPsiClass();
            PsiElement elementAt = getCurrentPsiElement();
//            int moveCursor = caretModel.getOffset() + s.length();
            PsiStatement expressionFromText = elementFactory.createStatementFromText(s, psiClass);
            elementAt.getParent().addBefore(expressionFromText, elementAt);
//            caretModel.moveToOffset(moveCursor);
            showingIntellisense = false;
            Dictionary.countafterundo.add(caretModel.getOffset()+3);
        });
    }

    public void writeAnyText(String s,String type) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Dictionary.countbeforeundo.add(caretModel.getOffset());
            editor.getDocument().insertString(caretModel.getOffset(),s);// works but no intellisense :(
            showingIntellisense = false;
            if(type.equals("if")||type.equals("loop")||type.equals("method")) {
                gotoNextLine();
                gotoNextColumn();
                if (type.equals("method")){
                    Dictionary.countafterundo.add(caretModel.getOffset() + 3);
                }
                else{
                    Dictionary.countafterundo.add(caretModel.getOffset() + 4);
                }

            }
            else if (type.equals("assignment")){
                gotoNextColumn(s.length()); // move cursor to the end of the written string
                newLine();
                Dictionary.countafterundo.add(caretModel.getOffset());
            }
            else
            {
                gotoNextColumn(s.length()); // move cursor to the end of the written string
                Dictionary.countafterundo.add(caretModel.getOffset());
            }

        });
    }

    public void createMethod(String s) {
        PsiClass psiClass = getCurrentPsiClass();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiMethod methodFromText = elementFactory.createMethodFromText(s, psiClass);
            psiClass.add(methodFromText);
        });

        showingIntellisense = false;
    }

    public void createVariable(PsiType type, String name, String init) {
        PsiClass psiClass = getCurrentPsiClass();
        PsiElement elementAt = getCurrentPsiElement();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiExpression expr = elementFactory.createExpressionFromText(init, psiClass);
            PsiDeclarationStatement statement = elementFactory.createVariableDeclarationStatement(name, type, expr, psiClass);
            elementAt.getParent().addBefore(statement, elementAt);
        });
        showingIntellisense = false;
    }

    public void createClassWithContent(String content) {
        PsiClass psiClass = getCurrentPsiClass();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiClass classFromText = elementFactory.createClassFromText(content, psiClass);
            psiClass.add(classFromText);
        });
        showingIntellisense = false;
    }

    public void createClassWithName(String name) {
        PsiClass psiClass = getCurrentPsiClass();
        Dictionary.datatypes.add(name);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiClass classFromName = elementFactory.createClass(name);
            PsiElement added = psiClass.add(classFromName);
            caretModel.moveToOffset(added.getTextOffset() + 2 + name.length());
            newLine();
        });
        showingIntellisense = false;
    }

    public void finishLine(){
        WriteCommandAction.runWriteCommandAction(project, () -> {
            caretModel.moveToOffset(caretModel.getVisualLineEnd()-1);
            writeAnyText(";", "");
        });
        newLine();
    }

    public void gotoLine(int line) {
        if (line > 0) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                caretModel.moveToLogicalPosition(new LogicalPosition(line - 1, caretModel.getVisualLineEnd()));
            });
        }
        showingIntellisense = false;
    }

    public void gotoNextLine() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            LogicalPosition current = caretModel.getLogicalPosition();
            caretModel.moveToLogicalPosition(new LogicalPosition(current.line + 1, caretModel.getVisualLineEnd()));
        });
        showingIntellisense = false;
    }

    public void gotoPrevLine() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            LogicalPosition current = caretModel.getLogicalPosition();
            if (current.line > 0)
                caretModel.moveToLogicalPosition(new LogicalPosition(current.line - 1, caretModel.getVisualLineEnd()));
        });
        showingIntellisense = false;
    }

    public void gotoNextColumn() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            LogicalPosition current = caretModel.getLogicalPosition();
            caretModel.moveToLogicalPosition(new LogicalPosition(current.line, current.column + 1));
        });
        showingIntellisense = false;
    }

    public void gotoNextColumn(int i) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            LogicalPosition current = caretModel.getLogicalPosition();
            caretModel.moveToLogicalPosition(new LogicalPosition(current.line, current.column + i));
        });
        showingIntellisense = false;
    }

    public void gotoPrevColumn() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            LogicalPosition current = caretModel.getLogicalPosition();
            if (current.column > 0)
                caretModel.moveToLogicalPosition(new LogicalPosition(current.line, current.column - 1));
        });
        showingIntellisense = false;
    }

    public void commentCurrentLine(){
        commentLine(caretModel.getLogicalPosition().line + 1);
    }

    public void commentLine(int i){
        gotoLine(i);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            caretModel.moveToOffset(caretModel.getVisualLineStart());
            editor.getDocument().insertString(caretModel.getOffset(), "//");
        });
    }

    public void addComment(String text){
        WriteCommandAction.runWriteCommandAction(project, () -> caretModel.moveToOffset(caretModel.getVisualLineEnd()-1));
        writeAnyText("      // " + text,"");
    }

    public void uncommentCurrentLine(){
        uncommentLine(caretModel.getLogicalPosition().line + 1);
    }

    public void uncommentLine(int i){
        gotoLine(i);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            caretModel.moveToOffset(caretModel.getVisualLineStart());
            if (getCurrentPsiElement() instanceof PsiComment){  // check that it really is a comment
                int offset = caretModel.getOffset();
                editor.getDocument().deleteString(offset, offset+2);
            }
        });
    }

    public void newLine(){
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    public void showIntellisense(){
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_SPACE);
        showingIntellisense = true;
    }
    public void showRunMenu()
    {
        if(!showingRunMenu)
        {
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.VK_F10);
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.keyRelease(KeyEvent.VK_F10);
            robot.keyRelease(KeyEvent.VK_ALT);
            showingRunMenu=true;
        }
        else
        {
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            showingRunMenu=false;
        }
    }

    public void runProgram()
    {
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_F10);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_F10);
    }

    public void chooseIntellisense(int option){
        if (option > 0) option--;
        if (showingIntellisense){
            for (int i = 0; i < option; i++){
                robot.keyPress(KeyEvent.VK_DOWN);
                robot.keyRelease(KeyEvent.VK_DOWN);
            }
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        }
        showingIntellisense = false;
    }
    public void createFile(String filename,String directoryname)
    {
        final PsiDirectory[] directory = new PsiDirectory[1];
        application.runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if(directoryname.equals(""))
                        directory[0] = current_file.getOriginalFile().getContainingDirectory();
                    else
                        directory[0] = current_file.getOriginalFile().getContainingDirectory().findSubdirectory(directoryname);
                }
                catch (Exception e)
                {
                    System.out.println(e);
                }
            }
        });
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run()
            {
                int m=filename.indexOf(".");
                String text=" class "+filename.substring(0,m)+"\n{ \n}";
                createFromTemplate(directory[0],filename,text);
                }
                }, ModalityState.NON_MODAL
        );
    }
    public void FileOpen(String filename,String directoryname)
    {
        final PsiDirectory[] directory = new PsiDirectory[1];
        application.runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if(directoryname.equals(""))
                        directory[0] = current_file.getOriginalFile().getContainingDirectory();
                    else
                        directory[0] = current_file.getOriginalFile().getContainingDirectory().findSubdirectory(directoryname);
                }
                catch (Exception e) {
                    System.out.println(e);
                }
            }
        });
        String path= directory[0].toString().substring(13, directory[0].toString().length())+"/"+filename+".java";
        File file=new File(path);
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run()
            {
                openFile(LocalFileSystem.getInstance().findFileByIoFile(file));
                }
                }, ModalityState.NON_MODAL
        );
    }

    public void openFile(com.intellij.openapi.vfs.VirtualFile file)
    {
//          sp.stop_flag =true;
          new OpenFileDescriptor(project, file).navigate(true);
    }
    public PsiElement createFromTemplate(PsiDirectory directory, String fileName, String text2)
    {
        final PsiFileFactory[] factory = new PsiFileFactory[1];
        PsiClass psiClass = getCurrentPsiClass();
        application.runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                      factory[0] = PsiFileFactory.getInstance(directory.getProject());
                }
                catch (Exception e) {
                }
            }
        });
        final PsiJavaFile[] file = new PsiJavaFile[1];
                System.out.println(directory.toString());
        if ((new File(fileName)).exists()) {
            throw new RuntimeException("File already exists");
        }
        WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> {
             file[0] = (PsiJavaFile) factory[0].createFileFromText(fileName, JavaLanguage.INSTANCE,  text2);
            return directory.add(file[0]);
        });
        return null;
    }
    public void makerdir(String name)
    {
        final PsiDirectory[] directory = new PsiDirectory[1];
        application.runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    directory[0] = current_file.getOriginalFile().getContainingDirectory();
                }
                catch (Exception e) {
                }
            }
        });
        String path= directory[0].toString().substring(13, directory[0].toString().length());
        File file=new File(path);
        File newDirectory = new File(file, name);
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run()
            {
                newDirectory.mkdir();
                project.getProjectFile().refresh(false,true);
                project.getBaseDir().refresh(false,true);
                project.getWorkspaceFile().refresh(false,true);
                }
                }, ModalityState.NON_MODAL
        );
        System.out.println("refreshed");
    }
    public void undo()
    {
        WriteCommandAction.runWriteCommandAction(project, () ->
        {
            try {
                int before = Dictionary.countbeforeundo.get(Dictionary.countbeforeundo.size() - 1);
                editor.getDocument().deleteString(before, Dictionary.countafterundo.get(Dictionary.countafterundo.size() - 1));
                caretModel.moveToOffset(before);
                Dictionary.countbeforeundo.remove(Dictionary.countbeforeundo.size() - 1);
                Dictionary.countafterundo.remove(Dictionary.countafterundo.size() - 1);
            } catch (IndexOutOfBoundsException e){
                System.out.println("undo: out of bounds exception...");
            }
        });
    }

    public void tab(){
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
    }
    public void backspace()
    {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().deleteString(caretModel.getOffset()-1,caretModel.getOffset());
        });
    }
    public void singlelinecopy(int line)
    {
        try {
            if (line > 0) {
                line--;
                Document d = editor.getDocument();
                TextRange t = new TextRange(d.getLineStartOffset(line), d.getLineEndOffset(line));
                copiedText = d.getText(t).trim();
                line++;
                Notification n = new Notification("", "Copied!", "Line " + line + " has been copied.", NotificationType.INFORMATION);
                notifyUser(n);
            }
        } catch (Exception e){

        }
    }
    public void multiplelinecopy(int line1,int line2)
    {
        try {
            if (line1 > 0 && line2 > 0) {
                line1--;
                line2--;
                Document d = editor.getDocument();
                TextRange t = new TextRange(d.getLineStartOffset(line1), d.getLineEndOffset(line2));
                copiedText = d.getText(t).trim();
                line1++;
                line2++;
                Notification n = new Notification("", "Copied!", "Line " + line1 + " to " + line2 + " has been copied.", NotificationType.INFORMATION);
                notifyUser(n);
            }
        } catch (Exception e){

        }
    }
    public void paste()
    {
        String[] splitted = copiedText.split("\n");
        System.out.println(Arrays.toString(splitted));
        for (String s : splitted){
            writeAnyText(s.trim(), "");
            newLine();
            try {
                Thread.sleep(10);
            } catch (Exception ex){

            }
        }

    }
    public void getVariables()
    {
        final PsiElement[] vars = PsiTreeUtil.collectElements(current_file, new PsiElementFilter() {
            public boolean isAccepted(PsiElement e) {
                if (e instanceof PsiVariable) {
                    String type=((PsiVariable) e).getType().toString();
                    String name= e.toString();
                    if(name.length()>17)
                    {
                        Dictionary.variabletypes.add(type.substring(8,type.length()));
                        Dictionary.variablenames.add(name.substring(17,name.length()));
                    }
                    return true;
                }
                return false;
            }
        });
    }

}
