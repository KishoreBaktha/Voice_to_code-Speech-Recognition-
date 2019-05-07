import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class StartRecording extends AnAction {
    public static AnActionEvent anActionEvent;

    public StartRecording() {
        super("Start");
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            anActionEvent=event;
            VoiceToCode vc = new VoiceToCode(event);
            vc.start();
//            new showOffset(event.getData(PlatformDataKeys.EDITOR).getCaretModel(), vc).start();
        } catch (NullPointerException nex) {
            Messages.showMessageDialog(event.getProject(), "Please put your cursor in the editor and restart the plugin.", "Almost there!", Messages.getInformationIcon());
        } catch (Exception e) {
        }

    }
    private class showOffset extends Thread{
        CaretModel caretModel;
        VoiceToCode vc;

        showOffset(CaretModel caretModel, VoiceToCode vc){
            this.vc = vc;
            this.caretModel = caretModel;
        }

        @Override
        public void run()
        {
            while (true){
                try {
                    Thread.sleep(3000);
                    System.out.println(vc.getCurrentPsiElement() + " offset=" + caretModel.getOffset());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}