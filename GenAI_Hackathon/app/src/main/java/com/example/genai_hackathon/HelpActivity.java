package com.example.genai_hackathon;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help); // 设置主界面的布局

        Log.i("HelpActivity", "reading file...");
        TextView myTextView = findViewById(R.id.helpText);
        String fileContent = readFromFile("help.txt"); // 读取文件内容
        Button backButton = findViewById(R.id.backButton);

        myTextView.setText(fileContent); // 在 TextView 中显示内容

        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    private String readFromFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        AssetManager assetManager = getAssets();
        try (InputStream inputStream = assetManager.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // 逐行读取内容
            }
        } catch (IOException e) {
            Log.e("HelpActivity", "读取文件时出错: " + e.getMessage());
            return "读取文件时出错"; // 错误提示
        }
        return stringBuilder.toString(); // 返回文件内容
    }
}
