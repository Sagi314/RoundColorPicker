package com.sagi314.roundcolorpicker;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getting the color picker from xml
        RoundColorPicker roundColorPicker = findViewById(R.id.round_color_picker);

        //setting the 'onColorChanged' listener to the method below (onColorChanged(int color))
        roundColorPicker.setOnColorChangedListener(this::onColorChanged);

        //select a new color. you can choose if you want to send an event for this specific change of color
        roundColorPicker.selectColor(Color.BLUE, true);

        //get current selected color
        roundColorPicker.getSelectedColor();
    }

    //this method will be activated any time the user will select a color
    private void onColorChanged(int color)
    {
        //do something with the color
    }
}