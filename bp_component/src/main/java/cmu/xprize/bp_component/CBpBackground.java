//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.bp_component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;

public class CBpBackground extends View implements ILoadableObject {

    private Paint mPaint;
    private Rect  mViewRegion = new Rect();

    // json loadable
    public String          style;
    public String          kind;
    public String          layout;
    public int             layoutcount;
    public String          colorbase;
    public String          colorchoice;
    public int[]           colors;

    private int   rows;
    private int   cols;
    private int   dim;
    private int[] viewColors;

    private IPrimitive[]    bgPrimitives;

    private float fDimension;
    private float fRows;
    private float fCols;


    static final String TAG = "CBpBackground";


    public CBpBackground() {
        super(CBP_Component.mContext);
        init(CBP_Component.mContext, null);
    }

    public CBpBackground(Context context) {
        super(context);
        init(context, null);
    }

    public CBpBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CBpBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    public void init(Context context, AttributeSet attrs) {

    }


    public void setColor(String color) {

        // Create a paint object to deine the line parameters
        mPaint = new Paint();

        mPaint.setColor(Color.parseColor(color));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(false);
    }



    @Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        int finalWidth, finalHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int desiredWidth = 2560;
        int desiredHeight = 1800;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {

            //Must be this size
            finalHeight = heightSize;
            //Log.d(TAG, "Height Using EXACTLY: " + height);

        } else if (heightMode == MeasureSpec.AT_MOST) {

            //Can't be bigger than...
            finalHeight = Math.min(desiredHeight, heightSize);
            //Log.d(TAG, "Height Using AT MOST: " + height);

        } else {
            //Be whatever you want
            finalHeight = desiredHeight;
            //Log.d(TAG, "Height Using UNSPECIFIED: " + height);
        }

        if (widthMode == MeasureSpec.EXACTLY) {

            //Must be this size
            finalWidth  = widthSize;
            //Log.d(TAG, "Height Using EXACTLY: " + height);

        } else if (widthMode == MeasureSpec.AT_MOST) {

            //Can't be bigger than...
            finalWidth = Math.min(desiredHeight, heightSize);
            //Log.d(TAG, "Height Using AT MOST: " + height);

        } else {
            //Be whatever you want
            finalWidth  = desiredWidth;
            //Log.d(TAG, "Height Using UNSPECIFIED: " + height);
        }

        setMeasuredDimension(finalWidth, finalHeight);
    }


    private void generateLayout() {

        int itemNdx = 0;

        switch (layout) {

            case "grid":

                dim  = (int) Math.ceil(fDimension);
                rows = (int) Math.ceil(fRows);
                cols = (int) Math.ceil(fCols);

                bgPrimitives = new IPrimitive[rows * cols];

                int top  = 0;

                for(int i1 = 0 ; i1 < rows ; i1++) {
                    int left = 0;

                    for(int i2 = 0 ; i2 < cols ; i2++) {

                        bgPrimitives[itemNdx++] = new CSquarePrimitive(new Rect(left, top ,left + dim , top + dim));

                        left += dim;
                    }
                    top += dim;
                }
                break;

            case "random":

                bgPrimitives = new IPrimitive[layoutcount];



                break;
        }

        genColorLayout();
    }


    private void genColorLayout() {

        int colorNdx = 0;

        for(IPrimitive item : bgPrimitives) {

            switch (colorchoice) {

                case "fixed":
                    item.setColor(colors[colorNdx]);
                    colorNdx = (colorNdx + 1) % colors.length;

                    break;

                case "random":
                    int color = colors[(int)(Math.random() * colors.length)];

                    Log.d(TAG, "Color:" + Integer.toHexString(color));

                    item.setColor(color);
                    break;
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        int width    = r - l;
        int height   = b - t;

        fDimension = width / 10f;
        fRows      = height / fDimension;
        fCols      = width  / fDimension;

        if(changed) {

            generateLayout();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {

        int itemNdx = 0;

        if(mPaint != null) {

            // make entire background a solid color by default
            //
            if(colorbase != null) {
                mPaint.setColor(Color.parseColor(colorbase));
                getDrawingRect(mViewRegion);
                canvas.drawRect(mViewRegion, mPaint);
            }

            switch (style) {

                case "primitives":

                    for(IPrimitive primitive : bgPrimitives) {

                        primitive.draw(canvas, mPaint);
                        itemNdx++;
                    }
                    break;

                case "bitmap":
                    break;
            }
        }

        super.onDraw(canvas);
    }




    //************ Serialization



    /**
     * Load the data source
     *
     * @param jsonData
     */
    @Override
    public void loadJSON(JSONObject jsonData, IScope scope) {

        JSON_Helper.parseSelf(jsonData, this, CClassMap.classMap, scope);

        setColor(colorbase);
    }

}