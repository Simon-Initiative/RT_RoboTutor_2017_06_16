//*********************************************************************************
//
//    Copyright(c) 2016-2017  Kevin Willows All Rights Reserved
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

package cmu.xprize.robotutor.tutorengine.widgets.core;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cmu.xprize.comp_logging.ITutorLogger;
import cmu.xprize.robotutor.RoboTutor;
import cmu.xprize.robotutor.tutorengine.CMediaController;
import cmu.xprize.robotutor.tutorengine.CMediaManager;
import cmu.xprize.robotutor.tutorengine.CMediaPackage;
import cmu.xprize.robotutor.tutorengine.CTutor;
import cmu.xprize.robotutor.tutorengine.CObjectDelegate;
import cmu.xprize.robotutor.tutorengine.ITutorGraph;
import cmu.xprize.robotutor.tutorengine.ITutorObjectImpl;
import cmu.xprize.robotutor.tutorengine.ITutorSceneImpl;
import cmu.xprize.robotutor.tutorengine.graph.vars.IScope2;
import cmu.xprize.robotutor.tutorengine.graph.vars.IScriptable2;
import cmu.xprize.robotutor.tutorengine.graph.vars.TBoolean;
import cmu.xprize.robotutor.tutorengine.graph.vars.TInteger;
import cmu.xprize.robotutor.tutorengine.graph.vars.TString;
import cmu.xprize.rt_component.CRt_Component;
import cmu.xprize.rt_component.IRtComponent;
import cmu.xprize.comp_logging.CErrorManager;
import cmu.xprize.util.IBehaviorManager;
import cmu.xprize.util.IEventSource;
import cmu.xprize.comp_logging.ILogManager;
import cmu.xprize.util.IPublisher;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;
import cmu.xprize.util.TCONST;
import edu.cmu.xprize.listener.ListenerBase;

import static cmu.xprize.util.TCONST.ASREventMap;
import static cmu.xprize.util.TCONST.EMPTY;
import static cmu.xprize.util.TCONST.LANG_AUTO;
import static cmu.xprize.util.TCONST.LOCAL_STORY_AUDIO;
import static cmu.xprize.util.TCONST.MEDIA_STORY;
import static cmu.xprize.util.TCONST.QGRAPH_MSG;
import static cmu.xprize.util.TCONST.SPEAK_UTTERANCE;
import static cmu.xprize.util.TCONST.TUTOR_STATE_MSG;

public class TRtComponent extends CRt_Component implements IBehaviorManager, ITutorObjectImpl, Button.OnClickListener, IRtComponent, IDataSink, IEventSource, IPublisher, ITutorLogger {

    private CTutor mTutor;
    private CObjectDelegate mSceneObject;
    private CMediaManager mMediaManager;

    private HashMap<String, String> volatileMap = new HashMap<>();
    private HashMap<String, String> stickyMap = new HashMap<>();

    private HashMap<String,String>  _StringVar  = new HashMap<>();
    private HashMap<String,Integer> _IntegerVar = new HashMap<>();
    private HashMap<String,Boolean> _FeatureMap = new HashMap<>();

    
    static private String TAG = "TRtComponent";


    public TRtComponent(Context context) {
        super(context);
    }

    public TRtComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    

    @Override
    public void init(Context context, AttributeSet attrs) {

        super.init(context, attrs);

        mSceneObject = new CObjectDelegate(this);
        mSceneObject.init(context, attrs);

        // Push the ASR listener reference into the super class in the Java domain
        //
        prepareListener(CMediaController.getTTS());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSceneObject.onDestroy();
    }


    //************************************************************************
    //************************************************************************
    // IBehaviorManager Interface START


    public void setVolatileBehavior(String event, String behavior) {

        Log.d("SET_BEHAVIOR - Volatile", "Event: " + event + "  - behavior: " + behavior );

        enableOnClickBehavior(event, behavior);

        if (behavior.toUpperCase().equals(TCONST.NULL)) {

            if (volatileMap.containsKey(event)) {
                volatileMap.remove(event);
            }
        } else {
            volatileMap.put(event, behavior);
        }

        // Configure the ASR static events in the listener itself
        //
        Integer eventType = ASREventMap.get(event);

        if(eventType != null) switch(eventType) {

            case TCONST.SILENCE_EVENT:
            case TCONST.SOUND_EVENT:
            case TCONST.WORD_EVENT:

                if (behavior.toUpperCase().equals(TCONST.NULL)) {

                    mListener.resetStaticEvent(eventType);
                } else {
                    mListener.configStaticEvent(eventType);
                }
                break;
        }
    }


    /** Special Behavior processing for timed ASR events which must be setup in the listener component
     *
     * @param event
     * @param behavior
     * @param timeout
     */
    public void setVolatileBehavior(String event, String behavior, int timeout) {

        // Setup the behavior
        //
        setVolatileBehavior(event, behavior);

        // Configure the ASR timed events in the listener itself
        //
        Integer eventType = ASREventMap.get(event);

        if(eventType != null) switch (eventType) {

            case TCONST.TIMEDSILENCE_EVENT:
            case TCONST.TIMEDSOUND_EVENT:
            case TCONST.TIMEDWORD_EVENT:

                if (behavior.toUpperCase().equals(TCONST.NULL)) {

                    mListener.resetTimedEvent(eventType);
                }
                else {
                    mListener.configTimedEvent(eventType, timeout);
                }
                break;
        }
    }


    public void setStickyBehavior(String event, String behavior) {

        Log.d("SET_BEHAVIOR - Sticky", "Event: " + event + "  - behavior: " + behavior );

        enableOnClickBehavior(event, behavior);

        if (behavior.toUpperCase().equals(TCONST.NULL)) {

            if (stickyMap.containsKey(event)) {
                stickyMap.remove(event);
            }
        } else {
            stickyMap.put(event, behavior);
        }

        // Configure the ASR static events in the listener itself
        //
        Integer eventType = ASREventMap.get(event);

        if(eventType != null) switch(eventType) {

            case TCONST.SILENCE_EVENT:
            case TCONST.SOUND_EVENT:
            case TCONST.WORD_EVENT:

                if (behavior.toUpperCase().equals(TCONST.NULL)) {

                    mListener.resetStaticEvent(eventType);
                }
                else {
                    mListener.configStaticEvent(eventType);
                }
                break;
        }
    }


    /** Special Behavior processing for timed ASR events which must be setup in the listener component
     *
     * @param event
     * @param behavior
     * @param timeout
     */
    public void setStickyBehavior(String event, String behavior, int timeout) {

        // Setup the behavior
        //
        setStickyBehavior(event, behavior);

        // Configure the ASR timed events in the listener itself
        //
        Integer eventType = ASREventMap.get(event);

        if(eventType != null) switch(eventType) {

            case TCONST.TIMEDSILENCE_EVENT:
            case TCONST.TIMEDSOUND_EVENT:
            case TCONST.TIMEDWORD_EVENT:

                if (behavior.toUpperCase().equals(TCONST.NULL)) {

                    mListener.resetTimedEvent(eventType);
                }
                else {
                    mListener.configTimedEvent(eventType, timeout);
                }
                break;
        }
    }


    // Execute script target if behavior is defined for this event
    //
    @Override
    public boolean applyBehavior(String event) {

        boolean result = false;
        
        if(!(result = super.applyBehavior(event))) {

            if (volatileMap.containsKey(event)) {

                RoboTutor.logManager.postEvent_D(QGRAPH_MSG, "target:" + TAG + ",action:applybehavior,type:volatile,behavior:" + event);
                applyBehaviorNode(volatileMap.get(event));

                // clear the volatile behavior after use and update the listener if the event is a
                // listener event.
                //
                setVolatileBehavior(event, TCONST.NULL, 0);

                result = true;

            } else if (stickyMap.containsKey(event)) {

                RoboTutor.logManager.postEvent_D(QGRAPH_MSG, "target:" + TAG + ",action:applybehavior,type:sticky,behavior:" + event);
                applyBehaviorNode(stickyMap.get(event));

                result = true;
            }
        }

        return result;
    }


    /**
     * Apply Events in the Tutor Domain.
     *
     * @param nodeName
     */
    @Override
    public void applyBehaviorNode(String nodeName) {
        IScriptable2 obj = null;

        if (nodeName != null && !nodeName.equals("") && !nodeName.toUpperCase().equals("NULL")) {

            try {
                obj = mTutor.getScope().mapSymbol(nodeName);

                if (obj != null) {

                    RoboTutor.logManager.postEvent_D(QGRAPH_MSG, "target:" + TAG + ",action:applybehaviornode,type:" + obj.getType() + ",behavior:" + nodeName);

                    switch(obj.getType()) {

                        case TCONST.SUBGRAPH:

                            mTutor.getSceneGraph().post(this, TCONST.SUBGRAPH_CALL, nodeName);
                            break;

                        case TCONST.MODULE:

                            // Disallow module "calls"
                            RoboTutor.logManager.postEvent_E(QGRAPH_MSG, "target:" + TAG + ",action:applybehaviornode,type:modulecall,behavior:" + nodeName +  ",ERROR:MODULE Behaviors are not supported");
                            break;

                        // Note that we should not preEnter queues - they may need to be cancelled
                        // which is done internally.
                        //
                        case TCONST.QUEUE:

                            if(obj.testFeatures()) {
                                obj.applyNode();
                            }
                            break;

                        default:

                            if(obj.testFeatures()) {
                                obj.preEnter();
                                obj.applyNode();
                            }
                            break;
                    }
                }

            } catch (Exception e) {
                // TODO: Manage invalid Behavior
                e.printStackTrace();
            }
        }
    }


    /**
     * Do button like behavior defined for component itself - i.e. click anywhere
     *
     * @param v
     */
    @Override
    public void onClick(View v) {

        if(v == this) {
            Log.v(QGRAPH_MSG, "event.click: " + " view");

            applyBehavior(TCONST.ON_CLICK);
        }

    }


    @Override
    public void nextScene() {
        mTutor.mTutorGraph.post(this, TCONST.NEXTSCENE);
    }


    @Override
    public void nextNode() {
        mTutor.mSceneGraph.post(this, TCONST.NEXT_NODE);
    }


    private void enableOnClickBehavior(String event, String behavior) {

        if(event.toUpperCase().equals(TCONST.ON_CLICK)) {

            if (behavior.toUpperCase().equals(TCONST.NULL)) {
                setOnClickListener(null);
            } else {
                setOnClickListener(this);
            }
        }
    }

    // IBehaviorManager Interface END
    //************************************************************************
    //************************************************************************



    //***********************************************************
    // ITutorLogger - Start

    private void extractHashContents(StringBuilder builder, HashMap map) {

        Iterator<?> tObjects = map.entrySet().iterator();

        while(tObjects.hasNext() ) {

            builder.append(',');

            Map.Entry entry = (Map.Entry) tObjects.next();

            String key   = entry.getKey().toString();
            String value = "#" + entry.getValue().toString();

            builder.append(key);
            builder.append(value);
        }
    }

    private void extractFeatureContents(StringBuilder builder, HashMap map) {

        StringBuilder featureset = new StringBuilder();

        Iterator<?> tObjects = map.entrySet().iterator();

        // Scan to build a list of active features
        //
        while(tObjects.hasNext() ) {

            Map.Entry entry = (Map.Entry) tObjects.next();

            Boolean value = (Boolean) entry.getValue();

            if(value) {
                featureset.append(entry.getKey().toString() + ";");
            }
        }

        // If there are active features then trim the last ',' and add the
        // comma delimited list as the "$features" object.
        //
        if(featureset.length() != 0) {
            featureset.deleteCharAt(featureset.length()-1);

            builder.append(",$features#" + featureset.toString());
        }
    }

    @Override
    public void logState(String logData) {

        StringBuilder builder = new StringBuilder();

        extractHashContents(builder, _StringVar);
        extractHashContents(builder, _IntegerVar);
        extractFeatureContents(builder, _FeatureMap);

        RoboTutor.logManager.postTutorState(TUTOR_STATE_MSG, "target#reading_tutor," + logData + builder.toString());
    }

    // ITutorLogger - End
    //***********************************************************



    //************************************************************************
    //************************************************************************
    // IEventSource Interface START


    @Override
    public String getEventSourceName() {
        return name();
    }

    @Override
    public String getEventSourceType() {
        return "Reading_Component";
    }


    // IEventSource Interface END
    //************************************************************************
    //************************************************************************


    /**
     * Note: We make a tacit assumption that SOURCEFILE (i.e. [file]) type descriptors have all their
     * assets external - i.e. in the public sdcard/robotutor_assets folder
     *
     * @param dataNameDescriptor
     */
    @Override
    public void setDataSource(String dataNameDescriptor) {

        try {

            // Note that here the {folder] load-type semantics is for a direct encoded link to an
            // external storydata.json file location
            //
            // TODO: work toward consistent [file] semantics as externally sourced files
            //
            if (dataNameDescriptor.startsWith(TCONST.LOCAL_FILE)) {

                String storyFolder = dataNameDescriptor.substring(TCONST.LOCAL_FILE.length()).toLowerCase();

                String[] levelval   = storyFolder.split("_");

                String levelFolder = levelval[0];

                DATASOURCEPATH  = TCONST.DOWNLOAD_RT_TUTOR + "/" +  TCONST.STORY_ASSETS + "/" + mMediaManager.getLanguageIANA_2(mTutor) + "/";
                STORYSOURCEPATH = DATASOURCEPATH + levelFolder + "/" + storyFolder + "/";

                // The audio for the story is in a  story specific folder -
                // Create the story specific sound package and push it into the soundMap in the MediaManager
                //
                AUDIOSOURCEPATH = TCONST.STORY_PATH + levelFolder + "/" + storyFolder ;

                // NOTE: we override the CMediaPackage srcpath folder to point to the debug LOCAL_STORY_AUDIO - in Download
                //
                configListenerLanguage(mMediaManager.getLanguageFeature(mTutor));
                mMediaManager.addSoundPackage(mTutor, MEDIA_STORY, new CMediaPackage(LANG_AUTO, AUDIOSOURCEPATH, LOCAL_STORY_AUDIO));

                loadStory(STORYSOURCEPATH, "ASB_Data", TCONST.EXTERN);

            } else if (dataNameDescriptor.startsWith(TCONST.ENCODED_FOLDER)) {

                String storyFolder = dataNameDescriptor.substring(TCONST.ENCODED_FOLDER.length()).toLowerCase();

                String[] levelval   = storyFolder.split("_");

                String levelFolder = levelval[0];

                DATASOURCEPATH  = TCONST.ROBOTUTOR_ASSETS + "/" +  TCONST.STORY_ASSETS + "/" + mMediaManager.getLanguageIANA_2(mTutor) + "/";
                STORYSOURCEPATH = DATASOURCEPATH + levelFolder + "/" + storyFolder + "/";

                // The audio for the story is in a  story specific folder -
                // Create the story specific sound package and push it into the soundMap in the MediaManager
                //
                AUDIOSOURCEPATH = TCONST.STORY_PATH + levelFolder + "/" + storyFolder ;

                configListenerLanguage(mMediaManager.getLanguageFeature(mTutor));
                mMediaManager.addSoundPackage(mTutor, MEDIA_STORY, new CMediaPackage(LANG_AUTO, AUDIOSOURCEPATH));

                loadStory(STORYSOURCEPATH, "ASB_Data", TCONST.EXTERN);
            }

            // Note that here the {file] load-type semantics is for an external file and [asset] may be used
            // for internal assets.
            //
            // TODO: work toward consistent [file] semantics as externally sourced files
            //
            else if (dataNameDescriptor.startsWith(TCONST.SOURCEFILE)) {

                // The story index is appended as a int
                String[] storyval   = dataNameDescriptor.split(":");
                int      storyIndex = Integer.parseInt(storyval[1]);

                String dataFile = storyval[0].substring(TCONST.SOURCEFILE.length()).toLowerCase();

                DATASOURCEPATH = TCONST.ROBOTUTOR_ASSETS + "/" +  TCONST.STORY_ASSETS + "/" + mMediaManager.getLanguageIANA_2(mTutor) + "/";

                String jsonData = JSON_Helper.cacheData(DATASOURCEPATH + dataFile, TCONST.DEFINED);

                // Load the datasource in the component module - i.e. the superclass
                //
                loadJSON(new JSONObject(jsonData), mTutor.getScope() );

                configListenerLanguage(mMediaManager.getLanguageFeature(mTutor));
                setStory(dataSource[storyIndex].storyName, TCONST.EXTERN);
            }

            else if (dataNameDescriptor.startsWith(TCONST.ASSETFILE)) {

                String dataFile = dataNameDescriptor.substring(TCONST.ASSETFILE.length());

                // Generate a langauage specific path to the data source -
                // i.e. tutors/word_copy/assets/data/<iana2_language_id>/
                // e.g. tutors/word_copy/assets/data/sw/
                //
                DATASOURCEPATH = TCONST.TUTORROOT + "/" + mTutor.getTutorName() + "/" + TCONST.TASSETS +
                                 "/" +  TCONST.DATA_PATH + "/" + mMediaManager.getLanguageIANA_2(mTutor) + "/";

                String jsonData = JSON_Helper.cacheData(DATASOURCEPATH + dataFile);

                // Load the datasource in the component module - i.e. the superclass
                //
                loadJSON(new JSONObject(jsonData), mTutor.getScope() );

                configListenerLanguage(mMediaManager.getLanguageFeature(mTutor));
                setStory(dataSource[0].storyName, TCONST.ASSETS);

            } else if (dataNameDescriptor.startsWith("db|")) {
            } else if (dataNameDescriptor.startsWith("{")) {

                loadJSON(new JSONObject(dataNameDescriptor), null);

            } else {
                throw (new Exception("BadDataSource"));
            }
        }
        catch (Exception e) {
            CErrorManager.logEvent(TAG, "Invalid Data Source for : " + mTutor.getTutorName(), e, false);
        }
    }


    /**
     *  Inject the listener into the MediaManageer
     */
    @Override
    public void setListener(ListenerBase listener) {
        CMediaController.setListener(listener);
    }


    /**
     *  Remove the listener from the MediaManageer
     */
    @Override
    public void removeListener(ListenerBase listener) {
        CMediaController.removeListener(listener);
    }



    //************************************************************************
    //************************************************************************
    // IPublisher - START

    @Override
    public void publishState() {

    }

    @Override
    public void publishValue(String varName, String value) {

        _StringVar.put(varName,value);

        // update the response variable  "<ComponentName>.<varName>"
        mTutor.getScope().addUpdateVar(name() + varName, new TString(value));

    }

    @Override
    public void publishValue(String varName, int value) {

        _IntegerVar.put(varName,value);

        // update the response variable  "<ComponentName>.<varName>"
        mTutor.getScope().addUpdateVar(name() + varName, new TInteger(value));

    }

    @Override
    public void publishFeatureSet(String featureSet) {

        // Add new features - no duplicates
        List<String> featArray = Arrays.asList(featureSet.split(","));

        for(String feature : featArray) {

            publishFeature(feature);
        }
    }

    @Override
    public void retractFeatureSet(String featureSet) {

        // Add new features - no duplicates
        List<String> featArray = Arrays.asList(featureSet.split(","));

        for(String feature : featArray) {

            retractFeature(feature);
        }
    }

    @Override
    public void publishFeature(String feature) {

        _FeatureMap.put(feature, true);
        mTutor.addFeature(feature);
    }

    /**
     * Note that we may retract features before they're published to add them to the
     * FeatureSet that should be pushed/popped when using pushDataSource
     * e.g. we want EOD to track even if it has never been set
     *
     * @param feature
     */
    @Override
    public void retractFeature(String feature) {

        _FeatureMap.put(feature, false);
        mTutor.delFeature(feature);
    }


    /**
     *
     * @param featureMap
     */
    @Override
    public void publishFeatureMap(HashMap featureMap) {

        Iterator<?> tObjects = featureMap.entrySet().iterator();

        while(tObjects.hasNext() ) {

            Map.Entry entry = (Map.Entry) tObjects.next();

            Boolean active = (Boolean)entry.getValue();

            if(active) {
                String feature = (String)entry.getKey();

                mTutor.addFeature(feature);
            }
        }
    }


    /**
     *
     * @param featureMap
     */
    @Override
    public void retractFeatureMap(HashMap featureMap) {

        Iterator<?> tObjects = featureMap.entrySet().iterator();

        while(tObjects.hasNext() ) {

            Map.Entry entry = (Map.Entry) tObjects.next();

            Boolean active = (Boolean)entry.getValue();

            if(active) {
                String feature = (String)entry.getKey();

                mTutor.delFeature(feature);
            }
        }
    }


    // publish component state data - EBD
    //************************************************************************
    //************************************************************************


    //**********************************************************
    //**********************************************************
    //*****************  Scripting Interface


    @Override
    public void setVisibility(String visible) {

        mSceneObject.setVisibility(visible);
    }


    /**
     * Defer to the base-class
     *
     * @param storyName
     */
    public void setStory(String storyName, String assetLocation) {

        super.setStory(storyName, assetLocation);
    }


    @Override
    public void setFeature(String feature, boolean fadd) {

        if(fadd) {
            publishFeature(feature);
        }
        else {
            retractFeature(feature);
        }

    }


    @Override
    public boolean testFeature(String feature) {

        return mTutor.testFeature(feature);
    }


    public void next() {

        reset();

        super.next();

        if(dataExhausted())
            publishFeature(TCONST.FTR_EOI);
    }


    public void startStory() {
        super.startStory();
    }


    public TBoolean test() {
        boolean correct = isCorrect();

        if(correct)
            publishFeature("FTR_RIGHT");
        else
            publishFeature("FTR_WRONG");

        return new TBoolean(correct);
    }


    public void setPageFlipButton(String command) {

        super.setPageFlipButton(command);
    }

    public void setSpeakButton(String command) {

        super.setSpeakButton(command);
    }


    public void onButtonClick(String buttonName) {

        switch(buttonName) {
            case TCONST.PAGEFLIP_BUTTON:
                applyBehavior(buttonName);
                break;

            case TCONST.SPEAK_BUTTON:
                applyBehavior(buttonName);
                break;
        }
    }


    @Override
    public void UpdateValue(boolean correct) {

        reset();

        if(correct)
            publishFeature(TCONST.GENERIC_RIGHT);
        else
            publishFeature(TCONST.GENERIC_WRONG);
    }


    public void reset() {

        retractFeature(TCONST.GENERIC_RIGHT);
        retractFeature(TCONST.GENERIC_WRONG);
    }


    @Override
    public void zoomInOut(Float scale, Long duration) {
        mSceneObject.zoomInOut(scale, duration);
    }

    @Override
    public void wiggle(String direction, Float magnitude, Long duration, Integer repetition ) {
        mSceneObject.wiggle(direction, magnitude, duration, repetition);
    }

    @Override
    public void setAlpha(Float alpha) {
        mSceneObject.setAlpha(alpha);
    }

    @Override
    public void seekToPage(int pageIndex) {
        mViewManager.seekToPage(pageIndex);
    }

    @Override
    public void nextPage() {
        mViewManager.nextPage();
    }

    @Override
    public void prevPage() {
        mViewManager.prevPage();
    }

    @Override
    public void seekToParagraph(int paraIndex) {
        mViewManager.seekToParagraph(paraIndex);
    }

    @Override
    public void nextPara() {
        mViewManager.nextPara();
    }

    @Override
    public void prevPara() {
        mViewManager.prevPara();

    }

    @Override
    public void seekToLine(int lineIndex) {
        mViewManager.seekToLine(lineIndex);
    }

    @Override
    public void nextLine() {
        mViewManager.nextLine();
    }

    @Override
    public void echoLine() {
        mViewManager.echoLine();
    }


    @Override
    public void prevLine() {
        mViewManager.prevLine();
    }

    @Override
    public void seekToWord(int wordIndex) {
        mViewManager.seekToWord(wordIndex);
    }

    @Override
    public void nextWord() {
        mViewManager.nextWord();
    }

    @Override
    public void prevWord() {
        mViewManager.prevWord();
    }

    @Override
    public void setHighLight(String highlight) {
        mViewManager.setHighLight(highlight, true);
    }

    @Override
    public boolean endOfData() {
        return mViewManager.endOfData();
    }

    @Override
    public void continueListening() {
        mViewManager.continueListening();
    }


    // Scripting Interface  End
    //************************************************************************
    //************************************************************************



    //**********************************************************
    //**********************************************************
    //*****************  Common Tutor Object Methods

    @Override
    public void setName(String name) {
        mSceneObject.setName(name);
    }

    @Override
    public String name() {
        return mSceneObject.name();
    }

    @Override
    public void setParent(ITutorSceneImpl mParent) {
        mSceneObject.setParent(mParent);
    }

    @Override
    public void setTutor(CTutor tutor) {
        mTutor = tutor;
        mSceneObject.setTutor(tutor);

        // The media manager is tutor specific so we have to use the tutor to access
        // the correct instance for this component.
        //
        mMediaManager = CMediaController.getManagerInstance(mTutor.getTutorName());
    }

    @Override
    public void onCreate() {}

    @Override
    public void setNavigator(ITutorGraph navigator) {
        mSceneObject.setNavigator(navigator);
    }

    @Override
    public void setLogManager(ILogManager logManager) {
        mSceneObject.setLogManager(logManager);
    }


    @Override
    public CObjectDelegate getimpl() {
        return mSceneObject;
    }





    // *** Serialization



    /**
     * Load the data source
     *
     * @param jsonObj
     */
    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        // Log.d(TAG, "Loader iteration");
        super.loadJSON(jsonObj, (IScope2) scope);
    }
}
