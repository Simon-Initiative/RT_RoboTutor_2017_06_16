/**
 Copyright(c) 2015-2017 Kevin Willows
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package cmu.xprize.bp_component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class CBubbleStimulus extends FrameLayout {

    private   Context         mContext;

    private ImageView         mIcon;
    private TextView          mText;
    private float             mScale;

    private float             mScaleCorrection;

    private Paint   mPaint;
    public String   mColorbase = "#000000";

    private Rect    mViewRegion = new Rect();



    static final String TAG = "CStimulus";



    public CBubbleStimulus(Context context) {
        super(context);
        init(context, null);
    }

    public CBubbleStimulus(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CBubbleStimulus(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        mContext = context;

        float instanceDensity = mContext.getResources().getDisplayMetrics().density;
        mScaleCorrection  = BP_CONST.DESIGN_SCALE / instanceDensity;

        // Create a paint object to deine the line parameters
        mPaint = new Paint();

        mPaint.setColor(Color.parseColor(mColorbase));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5f);
        mPaint.setAntiAlias(true);

        // Allow onDraw to be called to start animations
        //
        setWillNotDraw(false);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setClipChildren(false);

        mIcon = (ImageView) findViewById(R.id.SIcon);
        mText = (TextView) findViewById(R.id.SText);

        setScale(1.0f);
    }


    public void setScale(float newScale) {

        setAssignedScale(newScale);

        setScaleX(mScale);
        setScaleY(mScale);
    }


    @Override
    public void setScaleX(float newScale) {
        super.setScaleX(newScale *  mScaleCorrection);
    }

    @Override
    public void setScaleY(float newScale) {
        super.setScaleY(newScale *  mScaleCorrection);
    }

    @Override
    public float getScaleX() {
        return super.getScaleX() / mScaleCorrection;
    }

    @Override
    public float getScaleY() {
        return super.getScaleY() / mScaleCorrection;
    }

    public void setAssignedScale(float newScale) {
        mScale = newScale;
    }

    public float getAssignedScale() {
        return mScale;
    }

    public float getScaledWidth() {
        return getWidth() * mScale;
    }

    public void setContents(int resID, String text) {

        mIcon.setVisibility(View.INVISIBLE);
        mText.setVisibility(View.INVISIBLE);

        if(text == null) {
            mIcon.setImageResource(resID);
            mIcon.setVisibility(View.VISIBLE);
        }
        else {
            mText.setText(text);
            mText.setVisibility(View.VISIBLE);
        }

        invalidate();
    }


    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        getDrawingRect(mViewRegion);

        RectF bounds = new RectF(mViewRegion.left, mViewRegion.top, mViewRegion.right, mViewRegion.bottom);

        // Draw the stimulus outline box - partially transparent
        //
        mPaint.setColor(Color.parseColor("#AAAAAAAA"));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bounds, 30f, 30f, mPaint);

        mPaint.setColor(Color.parseColor("#AA000000"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2.0f);
        canvas.drawRoundRect(bounds, 30f, 30f, mPaint);
    }

}
