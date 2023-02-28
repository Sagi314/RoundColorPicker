package com.sagi314.roundcolorpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.function.Consumer;

public class RoundColorPicker extends View
{
    private static final float HS_PICKER_RADIUS_PERCENT = 0.7f;
    private static final float B_PICKER_THICKNESS_PERCENT = 0.2f;

    private static final float HS_THUMB_RADIUS_PT = 3;
    private static final float HS_THUMB_STROKE_WIDTH_PT = 1;
    private static final float B_THUMB_STROKE_WIDTH_PT = 1;

    private Consumer<Integer> onColorChangedListener;

    private final Paint hsPicker = new Paint();

    private final Paint hsThumb = new Paint();
    private final Point hsThumbCenter = new Point();
    private final float hsThumbRadiusInPixels;
    private final float hsThumbStrokeWidthInPixels;

    private final Paint bPicker = new Paint();

    private final Paint bThumb = new Paint();
    private Point[] bThumbPosition = new Point[2];
    private final float bThumbStrokeWidthInPixels;

    private final Paint hsPickerBrightnessShadow = new Paint();

    private Touch currentTouch;

    private HSBColor pickedColor;

    private boolean brightnessShadow;

    //region CONSTRUCTORS
    public RoundColorPicker(Context context, int color)
    {
        this(context, color, true);
    }

    public RoundColorPicker(Context context, int color, boolean brightnessShadow)
    {
        this(context, null);

        selectColor(color, false);
        setBrightnessShadow(brightnessShadow);
    }

    public RoundColorPicker(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public RoundColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }
    //endregion

    //MAIN CONSTRUCTOR - any other constructor should call this eventually
    public RoundColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        var pt = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 1, getResources().getDisplayMetrics());

        hsThumbRadiusInPixels = HS_THUMB_RADIUS_PT * pt;
        hsThumbStrokeWidthInPixels = HS_THUMB_STROKE_WIDTH_PT * pt;
        bThumbStrokeWidthInPixels = B_THUMB_STROKE_WIDTH_PT * pt;

        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        var attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundColorPicker, defStyleAttr, defStyleRes);

        selectColor( attributes.getColor(R.styleable.RoundColorPicker_color, Color.WHITE), false);
        setBrightnessShadow(attributes.getBoolean(R.styleable.RoundColorPicker_brightnessShadow, true));

        attributes.recycle();
    }

    private void init()
    {
        initHSPicker();

        initHSThumb();

        initBPicker();

        initBThumb();

        updateHSPickerBrightnessShadow();

        updateHSThumbPositionByHueAndSaturation();
        updateBThumbPositionByBrightness();
    }

    //region DRAW
    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        drawHSPicker(canvas);
        drawBPicker(canvas);
        drawHSPickerBrightnessShadow(canvas);
        drawHSThumb(canvas);
        drawBThumb(canvas);
    }
    //endregion

    //region ON TOUCH
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        var x = event.getX();
        var y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            var distanceFromCenter = distanceFromCenter(x, y);
            if (distanceFromCenter <= getHSPickerRadius())
            {
                currentTouch = Touch.HS;
            }
            else if (distanceFromCenter <= getRadius() && distanceFromCenter >= getRadius() - getBPickerThickness())
            {
                currentTouch = Touch.B;
            }
            else
            {
                currentTouch = Touch.NULL;
            }
        }

        switch(currentTouch)
        {
            case HS:
                setHue(x, y);
                setSaturation(x, y);
            break;
            case B:
                setBrightness(x, y);
            break;
        }

        update(true);

        return true;
    }

    private void update(boolean sendColorChangedEvent)
    {
        updatePickersColors();
        updateThumbsPositions();
        updateThumbsColors();

        invalidate();

        if(sendColorChangedEvent)
        {
            callOnColorChanged();
        }
    }


    //endregion

    //region ON SIZE CHANGED
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        super.onSizeChanged(w, h, oldW, oldH);

        init();
    }
    //endregion


    //region BUNDLE METHODS
    private void updateThumbsPositions()
    {
        updateHSThumbPositionByHueAndSaturation();
        updateBThumbPositionByBrightness();
    }

    private void updateThumbsColors()
    {
        updateHSThumbColor();
        updateBThumbColor();
    }

    private void updatePickersColors()
    {
        updateBPickerColors();
        updateHSPickerBrightnessShadow();
    }
    //endregion


    //region B PICKER
    private void initBPicker()
    {
        bPicker.setStyle(Paint.Style.STROKE);
        bPicker.setStrokeWidth(getBPickerThickness());

        updateBPickerColors();
    }

    private void updateBPickerColors()
    {
        var pickedColorRGB = pickedColor.toRGB();

        var brightestPickedColor = pickedColor.getCopy().maxBrightness().toRGB();

        var colors = new int[] { brightestPickedColor, Color.BLACK, pickedColorRGB };

        var shader = new SweepGradient(getCenterX(), getCenterY(), colors, new float[] { 0f, 0.5f, 0.5f });
        rotateShader(shader, -90, getCenterX(), getCenterY());

        bPicker.setShader(shader);
    }

    private void drawBPicker(Canvas canvas)
    {
        canvas.drawCircle(getCenterX(), getCenterY(), getRadius() - getBPickerThickness() / 2, bPicker);
    }
    //endregion

    //region B THUMB
    private void initBThumb()
    {
        bThumb.setStyle(Paint.Style.STROKE);
        bThumb.setStrokeWidth(bThumbStrokeWidthInPixels);

        updateBThumbColor();
    }

    private void updateBThumbPositionByBrightness()
    {
        var innerRadius = getRadius() - getBPickerThickness();
        var outerRadius = getRadius();

        var brightnessInRadians =  Math.toRadians(pickedColor.getBrightness() * -180 + 90);

        var cos = Math.cos(brightnessInRadians);
        var sin = Math.sin(brightnessInRadians);

        var p1 = new Point((int) (cos * innerRadius + getRadius()), (int) (sin * innerRadius + getRadius()));
        var p2 = new Point((int) (cos * outerRadius + getRadius()), (int) (sin * outerRadius + getRadius()));

        bThumbPosition = new Point[] { p1, p2 };
    }

    private void updateBThumbColor()
    {
        bThumb.setColor(pickedColor.getBrightness() < 0.5 ? Color.WHITE : Color.BLACK);
    }

    private void drawBThumb(Canvas canvas)
    {
        canvas.drawLine(bThumbPosition[0].x, bThumbPosition[0].y, bThumbPosition[1].x, bThumbPosition[1].y, bThumb);
    }
    //endregion

    //region HS PICKER
    private void initHSPicker()
    {
        int[] rainbowColors = { Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE,Color.MAGENTA, Color.RED };
        var rainbowShader = new SweepGradient(getCenterX(), getCenterY(), rainbowColors, null);

        int[] fogColors = { Color.WHITE, Color.TRANSPARENT };
        var fogShader = new RadialGradient(getCenterX(), getCenterY(), getRadius(), fogColors, null, Shader.TileMode.REPEAT);

        var combinationShader = new ComposeShader(rainbowShader, fogShader, PorterDuff.Mode.SRC_OVER);

        hsPicker.setShader(combinationShader);
    }

    private void drawHSPicker(Canvas canvas)
    {
        canvas.drawCircle(getCenterX(), getCenterY(), getHSPickerRadius(), hsPicker);
    }
    //endregion

    //region HS THUMB
    private void initHSThumb()
    {
        hsThumb.setStyle(Paint.Style.STROKE);
        hsThumb.setStrokeWidth(hsThumbStrokeWidthInPixels);
    }

    private void updateHSThumbColor()
    {
        if (isBrightnessShadow() && pickedColor.getBrightness() < 0.5)
        {
            hsThumb.setColor(Color.WHITE);
        }
        else
        {
            hsThumb.setColor(Color.BLACK);
        }
    }

    private void updateHSThumbPositionByHueAndSaturation()
    {
        var multiplier = getHSPickerRadius() * pickedColor.getSaturation();

        var hueInRadians = Math.toRadians(pickedColor.getHue());

        hsThumbCenter.x = (int) (Math.cos(hueInRadians) * multiplier + getRadius());
        hsThumbCenter.y = (int) (Math.sin(hueInRadians) * multiplier + getRadius());
    }

    private void drawHSThumb(Canvas canvas)
    {
        canvas.drawCircle(hsThumbCenter.x, hsThumbCenter.y, hsThumbRadiusInPixels, hsThumb);
    }
    //endregion

    //region HS PICKER BRIGHTNESS SHADOW
    private void updateHSPickerBrightnessShadow()
    {
        hsPickerBrightnessShadow.setColor(Color.argb((int) ((1 - pickedColor.getBrightness()) * 255), 0,0,0));
    }

    private void drawHSPickerBrightnessShadow(Canvas canvas)
    {
        if (isBrightnessShadow())
        {
            canvas.drawCircle(getCenterX(), getCenterY(), getHSPickerRadius(), hsPickerBrightnessShadow);
        }
    }
    //endregion


    //region PRIVATE GETTERS
    private float getCenterX()
    {
        return getRadius();
    }

    private float getCenterY()
    {
        return getRadius();
    }

    private float getRadius()
    {
        return Math.min(getHeight(), getWidth()) / 2.0f;
    }

    private float getHSPickerRadius()
    {
        return getRadius() * HS_PICKER_RADIUS_PERCENT;
    }

    private float getBPickerThickness()
    {
        return getRadius() * B_PICKER_THICKNESS_PERCENT;
    }
    //endregion

    //region PRIVATE SETTERS
    private void setHue(float x, float y)
    {
        var angle = angleFromCenter(x, y);

        pickedColor.setHue((float) angle);
    }

    private void setSaturation(float x, float y)
    {
        var distance = distanceFromCenter(x, y);

        var hsRadius = getHSPickerRadius();
        var saturation = Math.min(distance, hsRadius) / hsRadius;

        pickedColor.setSaturation((float) saturation);
    }

    private void setBrightness(float x, float y)
    {
        var radius = getRadius();

        //if the touch is on the left side
        if (x < radius)
        {
            //if its on the upper left - 1, lower left - 0.
            pickedColor.setBrightness(y < radius ? 1 : 0);
        }
        else
        {
            var offset = 90;
            var oppositeBrightness = (angleFromCenter(x, y) + offset) % 360 / 180f;
            var brightness = 1 - oppositeBrightness;

            pickedColor.setBrightness((float) brightness);
        }
    }
    //endregion

    //region PRIVATE HELPERS
    private void rotateShader(Shader shader, @SuppressWarnings("SameParameterValue") float degrees, float xPivot, float yPivot)
    {
        var matrix = new Matrix();
        matrix.preRotate(degrees, xPivot, yPivot);

        shader.setLocalMatrix(matrix);
    }

    private double angleFromCenter(float x, float y)
    {
        var angleInRadians = Math.atan2(y - getCenterY(), x - getCenterX());

        return (Math.toDegrees(angleInRadians) + 360) % 360;
    }

    private double distanceFromCenter(float x, float y)
    {
        return Math.sqrt( Math.pow(getCenterX() - x, 2) + Math.pow(getCenterY() - y, 2) );
    }

    private void callOnColorChanged()
    {
        if (onColorChangedListener != null)
        {
            onColorChangedListener.accept(pickedColor.toRGB());
        }
    }
    //endregion


    //region PUBLIC GETTERS
    public boolean isBrightnessShadow()
    {
        return brightnessShadow;
    }

    @SuppressWarnings("unused")
    public int getSelectedColor()
    {
        return pickedColor.toRGB();
    }
    //endregion

    //region PUBLIC SETTERS
    public void selectColor(int color, boolean sendColorChangedEvent)
    {
        var hsv = new float[3];
        Color.colorToHSV(color, hsv);

        var hsbColor = new HSBColor(hsv[0], hsv[1], hsv[2]);
        selectColor(hsbColor, sendColorChangedEvent);
    }

    public void selectColor(HSBColor hsbColor, boolean sendColorChangedEvent)
    {
        this.pickedColor = hsbColor;

        update(sendColorChangedEvent);

        invalidate();
    }

    public void setBrightnessShadow(boolean brightnessShadow)
    {
        this.brightnessShadow = brightnessShadow;
    }

    @SuppressWarnings("unused")
    public void setOnColorChangedListener(Consumer<Integer> onColorChangedListener)
    {
        this.onColorChangedListener = onColorChangedListener;
    }
    //endregion


    private enum Touch
    {
        HS, B, NULL
    }
}