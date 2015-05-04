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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
    private SoundPool soundPool;
    private boolean loaded;
    private boolean started;
    private int soundIdTick;
    private int soundIdFinish;
    float actualVolume = 0f;
    float maxVolume = 0f;
    float volume = 0f;
    double bpm = 120;
    double bpmInt;
    int mDuration = 0;
    private static final int MIN_BPM = 1;
    private static final int MAX_BPM = 240;
    AnimationDrawable metroAnim;

    // This handler makes the tick sound, runs the metronome animation, and reposts itself to
    // run at the next interval required to match the specified beats per minute (60 bpm - 1 second,
    // 120 bpm - 500 milliseconds)

    Handler handler = new Handler();
    Runnable runner = new Runnable() {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            metroAnim.stop();
            soundPool.play(soundIdTick, volume, volume, 1, 0, 1f);
            metroAnim.start();
            //Log.e("Test", "Played sound");
            // post this handler again after the specified beat per minute interval,
            // less the time taken up by prior computation (playing sound and restarting the animation)
            handler.postDelayed(this, Math.round(bpmInt) - (System.currentTimeMillis()-startTime));
        }
    };

    // this handler stops the previous handler from repeating itself after the specified duration
    // if a duration is specified

    Handler handlerDuration = new Handler();
    Runnable runnerDuration = new Runnable() {
        @Override
        public void run() {
            finishMetro();
            soundPool.play(soundIdFinish, volume, volume, 1, 0, 1f);
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
                    ImageView imgMetro = (ImageView) findViewById(R.id.imgMetro);
                    imgMetro.setBackground(metroAnim);
                }
            }
        });

        final Spinner spnDuration = (Spinner)findViewById(R.id.spnDuration);
        ArrayAdapter<CharSequence> adapDuration = ArrayAdapter.createFromResource(this,
                R.array.arrDuration,android.R.layout.simple_spinner_item);
        adapDuration.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDuration.setAdapter(adapDuration);
        spnDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //position 0 - Manual
                //position 1... - multiples of 30 seconds
                mDuration = position * 30000;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mDuration = 0;
            }
        });

        final Button btnGoStop = (Button)findViewById(R.id.btnGoStop);
        btnGoStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpm = Integer.parseInt(txtBpm.getText().toString());
                bpmInt = 60 / bpm * 1000;
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
                        // schedule the ticks at specified beats per minute

                        handler.postDelayed(runner, Math.round(bpmInt));

                        // if a duration specified, schedule the duration runnable to stop everything
                        // after the specified duration
                        if (mDuration > 0) {
                            handlerDuration.postDelayed(runnerDuration,mDuration);
                        }
                        btnGoStop.setText(R.string.action_stop);
                        txtBpm.setEnabled(false);
                        skbBpm.setEnabled(false);
                        spnDuration.setEnabled(false);
                    }
                } else {
/*
                    handler.removeCallbacks(runner);
                    metroAnim.stop();
                    txtBpm.setEnabled(true);
                    skbBpm.setEnabled(true);
                    spnDuration.setEnabled(true);
*/
                    finishMetro();
                    //if stop button is clicked, cancel any pending duration runnable
                    handlerDuration.removeCallbacks(runnerDuration);
                }

            }
        });


        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC,0);
        loaded = false;
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                //TODO differentiate between loading the two different sounds
                loaded = true;
            }
        });
        soundIdTick = soundPool.load(this, R.raw.tick, 1);
        soundIdFinish = soundPool.load(this, R.raw.demonstrative, 1);

        ImageView imgMetro = (ImageView)findViewById(R.id.imgMetro);
         metroAnim = buildAnimation(txtBpm);
        imgMetro.setBackground(metroAnim);
    }

    @Override
    protected void onPause () {
        super.onPause();
        finishMetro();
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

    private void finishMetro()
    {
        // stop animation and repeated posting of ticking runnable
        handler.removeCallbacks(runner);
        metroAnim.stop();
        EditText txtBpm = (EditText)findViewById(R.id.txtBpm);
        txtBpm.setEnabled(true);
        SeekBar skbBpm = (SeekBar)findViewById(R.id.skbBpm);
        skbBpm.setEnabled(true);
        Spinner spnDuration = (Spinner)findViewById(R.id.spnDuration);
        spnDuration.setEnabled(true);
        Button btnGoStop = (Button)findViewById(R.id.btnGoStop);
        btnGoStop.setText(R.string.action_go);
        started = false;
    }
}
