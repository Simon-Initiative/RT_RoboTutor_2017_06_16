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

package cmu.xprize.robotutor.tutorengine;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cmu.xprize.robotutor.tutorengine.graph.type_handler;
import cmu.xprize.robotutor.tutorengine.graph.type_timeline;
import cmu.xprize.robotutor.tutorengine.graph.type_timer;
import cmu.xprize.util.CErrorManager;
import cmu.xprize.util.TCONST;


/**
 * The MediaManager is a per tutor media repository - when a tutor is shut down it's
 * associated MediaManager should be destroyed as well to clean up any operations.
 * Audio , timers etc.
 *
 * This object centralizes access to ongoing processes like audio playback and timelines so
 * that we can pause, play, stop, or destroy them globally.  There is a system wide hard limit on the number
 * of active MediaPlayers in Android. (TODO: need reference)
 *
 * It provides volume control centralization
 *
 * It provides Media caching support
 *
 *
 */
public class CMediaManager {

    private CMediaController                mMediaController;

    private ArrayList<PlayerManager>        mPlayerCache   = new ArrayList<PlayerManager>();
    private HashMap<String, type_timer>     mTimerMap      = new HashMap<String, type_timer>();
    private HashMap<String, type_handler>   mHandlerMap    = new HashMap<String, type_handler>();
    private HashMap<String, type_timeline>  mTimeLineMap   = new HashMap<String, type_timeline>();

    private HashMap<CTutor, HashMap>        mMediaPackage  = new HashMap<>();
    private AssetManager                    mAssetManager;

    // Note that there is per tutor Language capability
    //
    private HashMap<CTutor, String>  mLangFtrMap     = new HashMap<CTutor, String>();

    final static public String TAG = "CMediaManager";


    /**
     *
     * @param controller
     * @param manager
     */
    public CMediaManager(CMediaController controller, AssetManager manager) {
        mMediaController = controller;
        mAssetManager    = manager;
    }


    public void restartMediaManager() {

        for(PlayerManager controller : mPlayerCache) {
            if(controller.isPlaying()) {
                controller.kill();
            }
        }

        Iterator<?> timerObjects = mTimerMap.entrySet().iterator();

        while(timerObjects.hasNext() ) {
            Map.Entry entry = (Map.Entry) timerObjects.next();

            type_timer timer = ((type_timer)(entry.getValue()));

            timer.globalPause();
        }


        Iterator<?> handlerObjects = mHandlerMap.entrySet().iterator();

        while(handlerObjects.hasNext() ) {
            Map.Entry entry = (Map.Entry) handlerObjects.next();

            type_handler handler = ((type_handler)(entry.getValue()));

            handler.globalPause();
        }


        Iterator<?> timelineObjects = mTimeLineMap.entrySet().iterator();

        while(timelineObjects.hasNext() ) {
            Map.Entry entry = (Map.Entry) timelineObjects.next();

            type_timeline timeline = ((type_timeline)(entry.getValue()));

            timeline.globalPause();
        }


        mPlayerCache = new ArrayList<PlayerManager>();
        mTimerMap      = new HashMap<String, type_timer>();
        mHandlerMap    = new HashMap<String, type_handler>();
        mTimeLineMap   = new HashMap<String, type_timeline>();

        mMediaPackage  = new HashMap<>();
        mLangFtrMap    = new HashMap<CTutor, String>();
    }



    //**************************************************************************
    // Language management START

    // Use two letter language codes as defined here:
    // https://www.w3.org/International/questions/qa-lang-2or3

    public String getLanguageIANA_2(CTutor tTutor) {

        return TCONST.langMap.get(mLangFtrMap.get(tTutor));
    }

    public String getLanguageFeature(CTutor tTutor) {

        try {
            return mLangFtrMap.get(tTutor);
        }
        catch(Exception e) {
            Log.d(TAG, "Excep:"  + e);

            return("LANG_SW");

        }
    }

    public void setLanguageFeature(CTutor tTutor, String langFtr) {

        mLangFtrMap.put(tTutor, langFtr);

        tTutor.updateLanguageFeature(langFtr);
    }

    public void setMediaPackage(CTutor tTutor, HashMap soundMap) {

        mMediaPackage.put(tTutor, soundMap);
    }


    public String mapMediaPackage(CTutor tTutor, String packageName, String langOverride) {

        HashMap<String,CMediaPackage> soundMap;
        CMediaPackage   mediaPack;
        String          autoLang;
        String          soundPackage;

        if(langOverride != null) {
            autoLang = mapLanguageIANA_2(langOverride);
        }
        else {
            autoLang = getLanguageIANA_2(tTutor);
        }

        try {
            // If the tutor is configured for mediapackages in the tutor_descriptor
            // Old tutors may not contain soundMaps - these default to what they expect.
            // non-soundMap tutors are deprecated.
            //
            soundMap = mMediaPackage.get(tTutor);

            if (soundMap != null) {

                // If the user didn't define a sound package use the default
                //
                if (packageName == null)
                    packageName = TCONST.DEFAULT_SOUND_PACKAGE;

                mediaPack = soundMap.get(packageName);

                switch (mediaPack.language) {
                    case TCONST.LANG_AUTO:
                        // Do nothing - Use the standard autoLang
                        break;

                    case TCONST.LANG_EFFECT:
                        // Use the non-language specific path - sound effects etc.
                        autoLang = "effect";
                        break;

                    default:
                        // Use the override language
                        autoLang = mapLanguageIANA_2(langOverride);
                        break;
                }

                soundPackage = autoLang + "/" + mediaPack.path;

            } else {
                soundPackage = autoLang;
            }
        }
        catch(Exception e) {
            soundPackage = autoLang;
        }

        return soundPackage;
    }


    public String mapLanguageIANA_2(String _language) {

        return TCONST.langMap.get(_language);
    }

    // Language management END
    //**************************************************************************



    //********************************************************************
    //*************  Timer Management START

    public void createTimer(String key, type_timer owner) {

        if(mTimerMap.containsKey(key)) {
            CErrorManager.logEvent(TAG,  "Duplicate Timer Name:" + key, new Exception("no-exception"), false);
        }
        mTimerMap.put(key, owner);
    }


    public type_timer removeTimer(String key) {
        return mTimerMap.remove(key);
    }


    public type_timer mapTimer(String key) {
        return mTimerMap.get(key);
    }


    public boolean hasTimer(String key) {
        return mTimerMap.containsKey(key);
    }


    //*************  Timer Management END
    //********************************************************************



    //********************************************************************
    //*************  Handler Management START

    public void createHandler(String key, type_handler owner) {

        if(mHandlerMap.containsKey(key)) {
            CErrorManager.logEvent(TAG,  "Duplicate Handler Name:" + key, new Exception("no-exception"), false);
        }
        mHandlerMap.put(key, owner);
    }


    public type_handler removeHandler(String key) {
        return mHandlerMap.remove(key);
    }


    public type_handler mapHandler(String key) {
        return mHandlerMap.get(key);
    }


    public boolean hasHandler(String key) {
        return mHandlerMap.containsKey(key);
    }


    //*************  Handler Management END
    //********************************************************************




    //********************************************************************
    //*************  Timeline Management START

    public void createTimeLine(String key, type_timeline owner) {

        if(mTimeLineMap.containsKey(key)) {
            CErrorManager.logEvent(TAG,  "Duplicate Timer Name:" + key, new Exception("no-exception"), false);
        }
        mTimeLineMap.put(key, owner);
    }


    public type_timeline removeTimeLine(String key) {
        return mTimeLineMap.remove(key);
    }


    public type_timeline mapTimeLine(String key) {
        return mTimeLineMap.get(key);
    }


    public boolean hasTimeLine(String key) {
        return mTimeLineMap.containsKey(key);
    }


    //*************  Timeline Management END
    //********************************************************************




    //********************************************************************
    //*************  MediaPlayer Management START


    public PlayerManager attachMediaPlayer(String dataSource, IMediaListener owner) {

        PlayerManager manager = null;

        // First look for an unattached (cached) controller that uses this datasource and reuse it.
        //
        for(PlayerManager playerInstance : mPlayerCache) {

            if(!playerInstance.isAttached() && playerInstance.compareSource(dataSource)) {
                Log.i(TAG, "Attach to existing PlayerManager");

                manager = playerInstance;
                manager.attach(owner);
                break;
            }
        }

        // If we don't find a usable controller then try and reuse the player that has been unused
        // for the longest period. i.e. assume the oldest unused audioclip has least probability of
        // being reused.
        //
        if(manager == null) {

            PlayerManager oldest   = null;
            long          timeTest = 0L;

            for(PlayerManager managerInstance : mPlayerCache) {

                if(!managerInstance.isAttached()) {
                    if(managerInstance.lastUsed() > timeTest) {
                        oldest = managerInstance;
                    }
                }
            }

            manager = oldest;

            // If we are re-purposing an old controller then we need to release it and
            // reset its dataSource.
            //
            if(manager != null) {

                Log.i(TAG, "Re-purpose an existing PlayerManager");

                manager.releasePlayer();

                manager.attach(owner);               // Need to reattach to a new owner
                manager.createPlayer(dataSource);    // Update the datasource
            }
        }

        // If we don't find a RE-usable controller then create a new one
        //
        if(manager == null) {

            Log.i(TAG, "Creating new PlayerManager");

            manager = new PlayerManager(owner, dataSource);

            mPlayerCache.add(manager);
        }

        return manager;
    }


    /**
     * Clean up the media controller for the given object - it is assumed that an owner may only
     * be attached to a single mediaController
     *
     * @param owner
     */
    public void  detachMediaPlayer(Object owner) {

        PlayerManager controller = null;

        // First look for an unattached (cached) controller that uses this datasource and reuse it.
        //
        for(PlayerManager managerInstance : mPlayerCache) {

            if(managerInstance.compareOwner(owner)) {

                managerInstance.detach();
            }
        }
    }


    public class PlayerManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

        private IMediaListener mOwner;

        private MediaPlayer  mPlayer        = null;
        private boolean      mPlaying       = false;
        private boolean      mIsReady       = false;
        private boolean      mIsAlive       = true;

        private Long         lastUsed       = 0L;
        private String       mDataSource = "";

        private boolean      mDeferredStart = false;
        private boolean      mDeferredSeek  = false;
        private long         mSeekPoint     = 0;


        final static public String TAG = "PlayerManager";


        protected PlayerManager(Object owner, String _dataSource) {

            mOwner      = (IMediaListener)owner;
            mDataSource = _dataSource;

            createPlayer(mDataSource);
        }


        protected void createPlayer(String dataSource) {

            try {
                mIsReady = false;
                mPlayer  = new MediaPlayer();

                // TODO: permit local file sources - see LoadTrack in type_timeline
                AssetFileDescriptor soundData = mAssetManager.openFd(dataSource);

                mPlayer.setDataSource(soundData.getFileDescriptor(), soundData.getStartOffset(), soundData.getLength());
                soundData.close();

                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
                mPlayer.setLooping(mOwner.isLooping());

                float volume = mOwner.getVolume();

                if(volume > 0) {
                    mPlayer.setVolume(volume, volume);
                }

                mPlayer.prepareAsync();

                Log.d(TAG, "Audio Loading: " + dataSource);

            } catch (Exception e) {
                Log.e(TAG, "Audio error: " + e);

                // Do the completion event to keep the tutor moving.
                //
                onCompletion(mPlayer);
            }
        }


        public void releasePlayer() {

            if(mPlayer != null) {
                mPlayer.pause();
                mPlaying = false;
                mIsAlive = true;

                mPlayer.reset();
                mPlayer.release();
                mPlayer = null;

                mDataSource = "";
            }
        }


        protected void attach(Object _owner) {
            mOwner = (IMediaListener)_owner;
        }


        /**
         * Note that we leave the MediaPlayer in a playable state
         */
        public void detach() {

            Log.d(TAG, "detach MediaPlayer");

            stop();
            mOwner = null;
        }


        protected boolean isAttached() {

            return mOwner != null;
        }


        protected boolean compareSource(String _dataSource) {

            return mDataSource.equals(_dataSource);
        }


        protected Long lastUsed() {

            return lastUsed;
        }


        protected boolean compareOwner(Object _owner) {

            return mOwner == _owner;
        }


        public boolean isPlaying() {
            return mPlaying;
        }


        // TODO : need tighter control over media player lifetime - running out of resources
        // since they aren't being released.
        public void play() {

            if(!mPlaying && mIsAlive) {

                if(mIsReady) {
                    // TODO: this will need a tweak for background music etc.
                    mMediaController.startSpeaking();

                    Log.d(TAG, "PLAY " + mDataSource);
                    mPlayer.start();

                    mPlaying       = true;
                    mDeferredStart = false;
                }
                else
                    mDeferredStart = true;
            }
        }


        public void stop() {

            Log.d(TAG, "stop MediaPlayer");

            pause();
            seek(0L);
        }


        /**
         * TODO: need to recycle the controller
         */
        public void kill() {

            Log.d(TAG, "Kill MediaPlayer");
            mIsAlive = false;

            // Note: using stop instead of pause seems to be preferable. pause seek combination
            // seems to have a probability of brief restart
            //
            if(mPlaying) {
                mPlayer.stop();
            }
        }


        public void pause() {
            if(mPlaying) {
                mPlayer.pause();
                mMediaController.stopSpeaking();
            }
            mPlaying = false;
        }


        public void seek(long frame) {

            mSeekPoint = frame;

            if(mIsReady) {
                int iframe = (int) (frame * 1000 / TCONST.FPS);

                seekTo(iframe);
            }
            else
                mDeferredSeek = true;

        }


        public void seekTo(int frameTime) {

            // No errors occur - but don't try to seek past the end
            // Note: we don't want to seek after death
            //
            if(mPlayer != null && mIsAlive && frameTime < mPlayer.getDuration())
                mPlayer.seekTo(frameTime);
        }


        @Override
        public void onPrepared(MediaPlayer mp) {

            mIsReady = true;

            // If seek was called before we were ready play
            if(mDeferredSeek)
                seek(mSeekPoint);

            // If play was called before we were ready play
            if(mDeferredStart)
                play();
        }


        /**
         * When play completes we pause the audio stream and seek to origin so it's ready to
         * play if another component want to reuse it.
         *
         * @param mp
         */
        @Override
        public void onCompletion(MediaPlayer mp) {

            // Track when each clip is last used so we can reuse cached clips intelligently
            // by re-purposing the oldest first.  i.e. We want to limit the reloading of clips
            // if possible.
            //
            lastUsed = System.currentTimeMillis();

            // Allow the owning node to do type specific processing of completion event.
            //
            if (mOwner != null) {

                mOwner.onCompletion(this);
            }
        }
    }

    //*************  MediaPlayer Management END
    //********************************************************************

}
