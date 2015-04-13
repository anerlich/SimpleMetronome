package com.example.andrew.simplemetronome;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
    private SoundPool soundPool;
    private boolean loaded;
    private boolean started;
    private int soundId;
    float actualVolume = 0f;
    float maxVolume = 0f;
    float volume = 0f;
    double bpm = 120;
    double bpmInt;
    private static final int MIN_BPM = 1;
    private static final int MAX_BPM = 240;
    AnimationDrawable metroAnim;

    Handler handler = new Handler();
    Runnable runner = new Runnable() {
        @Override
        public void run() {
            metroAnim.stop();
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
            metroAnim.start();
            //Log.e("Test", "Played sound");
            handler.postDelayed(this, Math.round(bpmInt));
        }
    };

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
*/
        started = false;

        final EditText txtBpm = (EditText)findViewById(R.id.txtBpm);
        txtBpm.setText(Long.toString(Math.round(bpm)));

        final SeekBar skbBpm = (SeekBar)findViewById(R.id.skbBpm);
        skbBpm.setMax(MAX_BPM - MIN_BPM);
        TextView txtBpmMax = (TextView)findViewById(R.id.txtMax);
        TextView txtBpmMin = (TextView)findViewById(R.id.txtMin);

        txtBpmMax.setText(Integer.toString(MAX_BPM));
        txtBpmMin.setText(Integer.toString(MIN_BPM));
        skbBpm.setProgress((int)bpm - MIN_BPM);
        txtBpm.setText(Integer.toString(skbBpm.getProgress() + MIN_BPM));

        skbBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bpm = progress + MIN_BPM;
                    txtBpm.setText(Integer.toString((int)bpm));
                    metroAnim = buildAnimation(txtBpm);
                    ImageView imgMetro = (ImageView)findViewById(R.id.imgMetro);
                    imgMetro.setBackground(metroAnim);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

/*
        txtBpm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    bpm = Integer.parseInt(v.getText().toString());
                    skbBpm.setProgress((int)bpm - MIN_BPM);
                    metroAnim = buildAnimation(txtBpm);
                    ImageView imgMetro = (ImageView)findViewById(R.id.imgMetro);
                    imgMetro.setBackground(metroAnim);
                    return true;
                }
                return false;
            }
        });
*/

        txtBpm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
                    bpm = Integer.parseInt(s.toString());
                    skbBpm.setProgress((int) bpm - MIN_BPM);
                    metroAnim = buildAnimation(txtBpm);
                    ImageView imgMetro = (ImageView)findViewById(R.id.imgMetro);
                    imgMetro.setBackground(metroAnim);
                }
            }
        });

        final Button btnGoStop = (Button)findViewById(R.id.btnGoStop);
        btnGoStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpm = Integer.parseInt(txtBpm.getText().toString());
                bpmInt = 60/bpm * 1000;
                // Getting the user sound settings
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                actualVolume = (float) audioManager
                        .getStreamVolume(AudioManager.STREAM_MUSIC);
                maxVolume = (float) audioManager
                        .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                volume = actualVolume / maxVolume;
                started = !started;
                if (started) {
                    // Is the sound loaded already?
                    if (loaded) {
                        handler.postDelayed(runner, Math.round(bpmInt));
                        btnGoStop.setText(R.string.action_stop);
                        txtBpm.setEnabled(false);
                        skbBpm.setEnabled(false);
                    }
                } else {
                    handler.removeCallbacks(runner);
                    metroAnim.stop();
                    txtBpm.setEnabled(true);
                    skbBpm.setEnabled(true);
                    btnGoStop.setText(R.string.action_go);
                }

            }
        });

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
        loaded = false;
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
            }
        });
        soundId = soundPool.load(this, R.raw.tick, 1);

        ImageView imgMetro = (ImageView)findViewById(R.id.imgMetro);
        //imgMetro.setBackgroundResource(R.drawable.metro_animation);
        //metroAnim = (AnimationDrawable)imgMetro.getBackground();
        //metroAnim = new AnimationDrawable();
        //metroAnim.setOneShot(false);
/*
        bpm = Integer.parseInt(txtBpm.getText().toString());
        bpmInt = 60/bpm * 1000;
        int iFrameDuration = (int)(bpmInt / 4 - 2);
        Resources resources = getResources();
        Drawable drawable = resources.getDrawable(R.drawable.metro_left);
        metroAnim.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_vert);
        metroAnim.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_right);
        metroAnim.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_vert);
        metroAnim.addFrame(drawable, iFrameDuration);
*/
        metroAnim = buildAnimation(txtBpm);
        imgMetro.setBackground(metroAnim);
    }

    @Override
    protected void onPause () {
        super.onPause();
        final Button btnGoStop = (Button)findViewById(R.id.btnGoStop);
        final EditText txtBpm = (EditText)findViewById(R.id.txtBpm);
        final SeekBar skbBpm = (SeekBar)findViewById(R.id.skbBpm);
        handler.removeCallbacks(runner);
        metroAnim.stop();
        btnGoStop.setText(R.string.action_go);
        txtBpm.setEnabled(true);
        skbBpm.setEnabled(true);
    }
    
    private AnimationDrawable buildAnimation(TextView txt) {
        AnimationDrawable animationDrawable = new AnimationDrawable();
        animationDrawable.setOneShot(false);
        bpm = Integer.parseInt(txt.getText().toString());
        bpmInt = 60/bpm * 1000;
        int iFrameDuration = (int)(bpmInt / 4 - 2);
        Resources resources = getResources();
        Drawable drawable = resources.getDrawable(R.drawable.metro_left);
        animationDrawable.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_vert);
        animationDrawable.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_right);
        animationDrawable.addFrame(drawable, iFrameDuration);
        drawable = resources.getDrawable(R.drawable.metro_vert);
        animationDrawable.addFrame(drawable, iFrameDuration);
        return animationDrawable;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
