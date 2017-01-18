package com.dodrio.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * Created by Spivey on 11/19/2016.
 */

public class ResultsActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Intent intent = getIntent();
        int anger = (int)(intent.getFloatExtra("anger",2.5f)*100);
        int disgust = (int)(intent.getFloatExtra("disgust",2.5f)*100);
        int fear = (int)(intent.getFloatExtra("fear",2.5f)*100);
        int joy = (int)(intent.getFloatExtra("joy",2.5f)*100);
        int sadness = (int)(intent.getFloatExtra("sadness",2.5f)*100);
        float analytical = intent.getFloatExtra("analytical",2.5f);
        float confident = intent.getFloatExtra("confident",2.5f);
        float tentative = intent.getFloatExtra("tentative",2.5f);
        int emotional_range = (int)(intent.getFloatExtra("emotional range",2.5f)*100);

        ProgressBar angerBar = (ProgressBar)findViewById(R.id.angerbar);
        ProgressBar disgustBar = (ProgressBar)findViewById(R.id.disgustbar);
        ProgressBar fearBar = (ProgressBar)findViewById(R.id.fearbar);
        ProgressBar joyBar = (ProgressBar)findViewById(R.id.joybar);
        ProgressBar sadnessBar = (ProgressBar)findViewById(R.id.sadnessbar);
        TextView angerPercent = (TextView)findViewById(R.id.angerpercent);
        TextView disgustPercent = (TextView)findViewById(R.id.disgustpercent);
        TextView fearPercent = (TextView)findViewById(R.id.fearpercent);
        TextView joyPercent = (TextView)findViewById(R.id.joypercent);
        TextView sadnessPercent = (TextView)findViewById(R.id.sadnesspercent);
        TextView languageTone = (TextView)findViewById(R.id.language_tone);
        TextView emotionalRange = (TextView)findViewById(R.id.emotional_range);
        TextView query = (TextView)findViewById(R.id.query);
        query.setText(intent.getStringExtra("Query"));


        angerBar.setProgress(anger);
        angerPercent.setText(Integer.toString(anger)+"%");
        disgustBar.setProgress(disgust);
        disgustPercent.setText(Integer.toString(disgust)+"%");
        fearBar.setProgress(fear);
        fearPercent.setText(Integer.toString(fear)+"%");
        joyBar.setProgress(joy);
        joyPercent.setText(Integer.toString(joy)+"%");
        sadnessBar.setProgress(sadness);
        sadnessPercent.setText(Integer.toString(sadness)+"%");

        if(analytical > confident && analytical > tentative) {
            languageTone.setText("Language Tone: Analytical");
        } else if(confident > analytical && confident > tentative) {
            languageTone.setText("Language Tone: Confident");
        } else if(tentative > analytical && tentative > confident) {
            languageTone.setText("Language Tone: Tentative");
        } else {
            languageTone.setText("Language Tone: None detected");
        }

        emotionalRange.setText("Emotional Range: " + Integer.toString(emotional_range) +"%");


    }
}
