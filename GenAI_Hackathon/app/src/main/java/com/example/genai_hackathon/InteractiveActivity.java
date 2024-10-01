package com.example.genai_hackathon;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;

import androidx.annotation.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.speech.tts.TextToSpeech;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.*;

public class InteractiveActivity extends AppCompatActivity{

    private boolean recordPermissionGranted;

    private EditText messageInput;
    private LinearLayout chatLayout;
    private CustomScrollView chatScrollView;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;

    private boolean isRecording = false;
    private Thread recordingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_interactive);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.interactive), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // receive permissions from MainActivity
        recordPermissionGranted = getIntent().getBooleanExtra("RECORD_PERMISSION_GRANTED", false);

        /* initialize all widgets */
        messageInput = findViewById(R.id.messageInput);
        Button sendButton = findViewById(R.id.sendButton);
        Button backButton = findViewById(R.id.backButton);
        Button helpButton = findViewById(R.id.helpButton);
        chatLayout = findViewById(R.id.chatLayout);
        chatScrollView = findViewById(R.id.chatScrollView);
        chatScrollView.setFocusable(true);
        chatScrollView.setFocusableInTouchMode(true);
        messageInput.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {
                chatScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        chatScrollView.scrollTo(0, messageInput.getBottom());
                        }
                });
            }
        });
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINESE); // 设置语言为中文
//                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                    // 语言不支持的处理
//                }
            }
        });
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // 处理错误
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0); // 取第一个识别结果
                    sendMessage(recognizedText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        /* widgets initializing complete */

        // press screen to speak
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if(!isRecording) {
                    startRecording();
                } else {
                    return;
                }
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                return true;
            }
        });

        chatScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                chatScrollView.performClick(); // 确保可访问性
                if(isRecording) {
                    stopRecording();
                    startRecognition();
                }
            }
            return false; // 返回 false 允许 ScrollView 继续处理滚动事件
        });

        // send message
        sendButton.setOnClickListener(v -> {
            sendMessage();
        });

        // help
        helpButton.setOnClickListener(v -> {
            Intent intent = new Intent(InteractiveActivity.this, HelpActivity.class);
            startActivity(intent);
        });

        // back
        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    /* audio recording */
    private void startRecording() {
        if (recordPermissionGranted) {
            isRecording = true;
            recordingThread = new Thread(this::recordAudio);
            recordingThread.start();
            Toast.makeText(this, "Recording in progress", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
        Toast.makeText(this, "Recording complete", Toast.LENGTH_SHORT).show();
    }

    private void recordAudio() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            ar.startRecording();

            // read in audio and store it in audioBuf
            byte[] audioBuf = new byte[bufferSize];
            while (isRecording) {
                ar.read(audioBuf, 0, audioBuf.length);
            }

            ar.stop();
            ar.release();

            startRecognition();
        } catch (SecurityException e) {
            Toast.makeText(this, "Record permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // 设置语言为英语
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // start recognizing
        speechRecognizer.startListening(intent);
    }

    /* message sending */
    // logic
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            addMessage(message, true);
            messageInput.setText("");
        }
    }

    private void sendMessage(String message) {
        if (!message.isEmpty()) {
            addMessage(message, true);
            messageInput.setText("");
        }
    }

    // ui
    public void addMessage(String message, boolean isUserMessage) {
        CardView cv = new CardView(this);
        cv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        cv.setCardElevation(5);
        cv.setCardBackgroundColor(Color.parseColor("#ADD8E6"));
        cv.setRadius(16);

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(16, 16, 16, 16);
        tv.setTextSize(16);

        // add TextView to CardView
        cv.addView(tv);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 16, isUserMessage ? 27 : 0, 16);
        layoutParams.gravity = isUserMessage ? Gravity.END : Gravity.START;
        cv.setLayoutParams(layoutParams);
        // add CardView to layout
        chatLayout.addView(cv);

        chatScrollView.post(() -> chatScrollView.scrollTo(0, chatLayout.getBottom()));
    }

    /* speak out received texts */
    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void onResponseReceived(String response) {
        speakOut(response);
    }
}
