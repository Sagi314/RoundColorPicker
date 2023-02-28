package com.sagi314.roundcolorpicker;

import android.graphics.Color;

//todo unit test
public class HSBColor
{
    public static final float MAX_SATURATION = 1.0f;
    public static final float MAX_BRIGHTNESS = 1.0f;

    private float hue;
    private float saturation;
    private float brightness;

    public HSBColor(float hue, float saturation, float brightness)
    {
        setHue(hue);
        setSaturation(saturation);
        setBrightness(brightness);
    }


    public int toRGB()
    {
        return Color.HSVToColor(new float[] { hue, saturation, brightness });
    }

    public HSBColor getCopy()
    {
        return new HSBColor(hue, saturation, brightness);
    }

    public HSBColor maxBrightness()
    {
        setBrightness(MAX_BRIGHTNESS);

        return this;
    }


    //region GETTERS
    public float getHue()
    {
        return hue;
    }

    public float getSaturation()
    {
        return saturation;
    }

    public float getBrightness()
    {
        return brightness;
    }
    //endregion

    //region SETTERS
    public void setHue(float hue)
    {
        this.hue = hue % 360;
    }

    public void setSaturation(float saturation)
    {
        this.saturation = throwIfNotInBoundsIncluding(0, MAX_SATURATION, saturation, "saturation");
    }

    public void setBrightness(float brightness)
    {
        this.brightness = throwIfNotInBoundsIncluding(0, MAX_BRIGHTNESS, brightness, "brightness");
    }
    //endregion

    private float throwIfNotInBoundsIncluding(float min, float max, float value, String valueName)
    {
        if (min >= max)
        {
            throw new IllegalArgumentException("private class error: min must be < max");
        }
        if (min > value || value > max)
        {
            throw new IllegalArgumentException(String.format("%s must be between %f to %f (current value: %f)", valueName, min, max, value));
        }

        return value;
    }

    @Override
    public String toString()
    {
        return "HSBColor{" +
                "hue=" + hue +
                ", saturation=" + saturation +
                ", brightness=" + brightness +
                '}';
    }
}