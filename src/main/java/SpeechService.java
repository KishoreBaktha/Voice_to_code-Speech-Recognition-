import com.google.api.gax.rpc.*;
import com.google.cloud.speech.v1p1beta1.*;

import com.google.protobuf.ByteString;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;

import javax.sound.sampled.*;
import java.util.*;
import java.net.*;

public class SpeechService
{
    public boolean bluetoothRunning = false;
    public boolean stop_flag =false;
    public long last_pause;
    public boolean hasRecommended;
   public boolean val=true;
   private static int milliseconds = 1*60000;

    private static final Notification RECOMMEND_PAUSE = new Notification("", "Recommended break", "You've been working for " + milliseconds /60000 + "minutes. Need a break? Pause by saying \"pause\"", NotificationType.INFORMATION);
//    private static final boolean DEBUG = true;
    static List<String> hintPhrases = new ArrayList<>(Arrays.asList(
            "integer", "string", "void", "public", "private", "variable", "class", "create", "main", "static", "method","java",
            "intellisense", "row", "column", "next", "previous", "dot", "comma", "enter", "plus", "minus", "multiply", "divide",
            "star", "space", "end", "not", "or", "enter", "colon", "array", "semicolon", "equals", "lesser", "greater", "quotes",
            "go to", "pause", "restart", "new line", "comment", "here", "file", "uncomment", "camel","import","system","out","println",
            "scanner","new","in","increment","decrement","open","close","quote", "tab", "finish line", "output", "copy line", "paste"
    ));

    public void startListening(Dictionary dictionary) throws Exception
    {
        last_pause = System.currentTimeMillis();
        ResponseObserver<StreamingRecognizeResponse> responseObserver = null;
        try (SpeechClient client = SpeechClient.create())
        {
            if (!bluetoothRunning) {
                bluetoothRunning = true;
                BluetoothService.discoverDevices(this, dictionary);
            }

            responseObserver = createResponseObserver(dictionary);

            ClientStream<StreamingRecognizeRequest> clientStream =
                    client.streamingRecognizeCallable().splitCall(responseObserver);
            RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(16000)
                    .setMaxAlternatives(5)
                    .setEnableSpeakerDiarization(true)
                    .setDiarizationSpeakerCount(1);

            configBuilder.addSpeechContextsBuilder().addAllPhrases(hintPhrases);

            RecognitionConfig recognitionConfig = configBuilder.build();

            StreamingRecognitionConfig streamingRecognitionConfig =
                    StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build();

            StreamingRecognizeRequest request =
                    StreamingRecognizeRequest.newBuilder()
                            .setStreamingConfig(streamingRecognitionConfig)
                            .build(); // The first request in a streaming call has to be a config

            clientStream.send(request);

            // SampleRate:16000Hz, SampleSizeInBits: 16, Number of channels: 1, Signed: true,
            // bigEndian: false
            AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);

            DataLine.Info targetInfo =
                    new DataLine.Info(
                            TargetDataLine.class,
                            audioFormat); // Set the system information to read from the microphone audio stream

            if (!AudioSystem.isLineSupported(targetInfo)) {
                System.out.println("Microphone not supported");
                System.exit(0);
            }
            // Target data line captures the audio stream the microphone produces.
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            long startTime = System.currentTimeMillis();
            // Audio Input Stream
            AudioInputStream audio = new AudioInputStream(targetDataLine);
                while (true) {
                    if (stop_flag) {
                        targetDataLine.close();
                        return;
                    }
                    if (!hasRecommended && !dictionary.paused && ((System.currentTimeMillis() - last_pause) > (milliseconds))) {    // recommend break after 30 milliseconds
                        dictionary.vc.notifyUser(RECOMMEND_PAUSE);
                        hasRecommended = true;
                    }
                    val = checkConnection();
                    if (!val) {
                        targetDataLine.stop();
                        targetDataLine.close();
                        stop_flag = false;
                        return;
                    }
                    try {
                        long estimatedTime = System.currentTimeMillis() - startTime;
                        if (estimatedTime > 60000) { // 60 seconds
                            targetDataLine.stop();
                            targetDataLine.close();
                            break;
                        }
                        byte[] data = new byte[6400];
                        audio.read(data);
                        request = StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(data))
                                .build();

                        clientStream.send(request);
                    } catch (Exception e) {
                        System.out.println("no connection");
                        targetDataLine.stop();
                        targetDataLine.close();
                        throw e;
                    }
                }

        } catch (Exception e) {
            e.printStackTrace();
        }
        startListening(dictionary);
        // timeout after 60s
    }

    private static ResponseObserver<StreamingRecognizeResponse> createResponseObserver(Dictionary dictionary)
    {
        return new ResponseObserver<StreamingRecognizeResponse>() {
            ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();
            int previous_index = 0;

            public void onStart(StreamController controller) {}

            public void onResponse(StreamingRecognizeResponse response) {
                try
                {
                    Dictionary.casematched=false;
                    String alt="";
                    StreamingRecognitionResult result = response.getResultsList().get(0);
                    SpeechRecognitionAlternative alternative=null;
                    int size=result.getAlternativesList().size();
                    for(int i=0;i<size;i++)
                    {
                        alt=result.getAlternativesList().get(i).getTranscript();
                        alternative=result.getAlternativesList().get(i);
                        if (!dictionary.paused){
                            System.out.println("alt is-"+alt.trim().toLowerCase());
                        }
                        if (multipleSpeakers(alternative)){
                            System.out.println("Multiple speakers -> pause: " + alt);
                            previous_index = alternative.getWordsList().size();
                            return;
                        }
                        else if(alt.contains("\n"))
                        {
                            System.out.println("enter");
                            dictionary.generateCode("enter");
                            break;
                        }
                        else {
                            dictionary.generateCode(alt.trim().toLowerCase());
                        }
                        if(Dictionary.casematched){ // when transcript is matched in dictionary
                            previous_index = alternative.getWordsList().size();
                            break;
                        }

                    }
                    responses.add(response);
                }
                catch (Exception e)
                {
                    System.out.println("Error on response: ");
                    e.printStackTrace();
                }

            }
            // Khoa: This is not called anywhere at the moment, could be use when we implement stop  listening command later.
            public void onComplete() {
                for (StreamingRecognizeResponse response : responses) {
                    StreamingRecognitionResult result = response.getResultsList().get(0);
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    if (!dictionary.paused){
                        System.out.printf("Transcript : %s\n", alternative.getTranscript());
                    }
                }
            }

            public void onError(Throwable t) {
                System.out.println(t);
            }

            private boolean multipleSpeakers(SpeechRecognitionAlternative alternative){
                List<WordInfo> list = alternative.getWordsList();
                ListIterator<WordInfo> wordListIterator;
                try {
                    wordListIterator = list.listIterator(previous_index);
                } catch (IndexOutOfBoundsException e){
                    wordListIterator = list.listIterator(0);
                }
                Set<Integer> speakers = new HashSet<>();
                while(wordListIterator.hasNext()){
                    WordInfo w = wordListIterator.next();
                    if (!w.getWord().isEmpty()){
                        speakers.add(w.getSpeakerTag());
//                        System.out.print(w.getWord() + " ");
                        if (speakers.size() > 1){
                            dictionary.generateCode("pause");
//                                        if (DEBUG){
//                                            for (WordInfo wi : wordList){
//                                                System.out.println(wi.getSpeakerTag() + ": " + wi.getWord());
//                                            }
//                                        }
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
    public static boolean checkConnection()
    {
        Socket socket=new Socket();
        InetSocketAddress socketAddress=new InetSocketAddress("www.google.com",80);
        try
        {
            socket.connect(socketAddress,2000);
            socket.close();
            return true;
        }
        catch (Exception e) {
            try {
                socket.close();
            } catch (Exception e1) {
                return false;
            }
            return false;
        }
    }
    public static void checkforrestart()
    {
        while(true)
        {
            if(checkConnection())
            {
                ApplicationManager.getApplication().runReadAction(() -> {
                    VoiceToCode vc = new VoiceToCode(StartRecording.anActionEvent);
                    vc.start();
                });
                break;
            }
        }
    }

}