package cmu.xprize.robotutor.tutorengine.widgets.core;

import android.content.Context;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.util.Date;

import cmu.xprize.robotutor.BuildConfig;
import cmu.xprize.robotutor.RoboTutor;
import cmu.xprize.util.TCONST;

import static cmu.xprize.util.TCONST.GRAPH_MSG;

/**
 * This View is purpose built to diplay the current app build version name from the Gradle app script
 *
 */
public class TVersionView extends TTextView {

    public TVersionView(Context context) {
        super(context);
    }

    public TVersionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TVersionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);

        String rtVersion = "RoboTutor " + BuildConfig.VERSION_NAME;
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        setText(rtVersion);
    }
}
