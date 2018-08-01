package com.amitshekhar.tflite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;


class CustomView extends View {
    Paint paint = new Paint();

    private static CustomView instance;
    public static CustomView getInstance() {
        return instance;
    }
    public List<Box> DrawingBox;

    private void init() {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        instance = this;

    }

    public CustomView(Context context) {
        super(context);
        init();
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onDraw(Canvas canvas) {

        
        if(DrawingBox != null) {

            for (Box box : DrawingBox) {

                // * 200.0f / 224.0f
                Box b = box.getScaledBox(this.getWidth(),this.getHeight());
                //canvas.drawRect(b.x1, b.y1, b.x2, b.y2, paint);

                canvas.drawLine(b.x1, b.y1, b.x1, b.y2, paint);
                canvas.drawLine(b.x1, b.y1, b.x2,b.y1, paint);
                canvas.drawLine(b.x1, b.y2, b.x2, b.y2, paint);
                canvas.drawLine(b.x2, b.y1, b.x2, b.y2, paint);

            }

        }
        canvas.drawLine(0, 0, 20, 20, paint);
        canvas.drawLine(20, 0, 0, 20, paint);
    }
}