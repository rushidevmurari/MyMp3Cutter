package com.easymp3cutter.mymp3cutter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.util.ArrayList;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class Player extends AppCompatActivity {
    RangeSeekBar mSeekBar;
    TextView songTitle;
    Button audiotrim;
    ProgressDialog progressDialog;
    String audio_url;
    private Runnable r;
    ArrayList<File> allSongs;
    static MediaPlayer mMediaPlayer;
    int position;
    TextView curTime;
    TextView totTime;
    ImageView playIcon;
    ImageView prevIcon;
    ImageView nextIcon;
    Intent playerData;
    Bundle bundle;

    ImageView curListIcon;

    private static final String root= Environment.getExternalStorageDirectory().toString();
    private static final String app_folder=root+"/Music/song/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mSeekBar = findViewById(R.id.mSeekBar);
        songTitle = findViewById(R.id.songTitle);
        curTime = findViewById(R.id.curTime);
        totTime = findViewById(R.id.totalTime);
        audiotrim = findViewById(R.id.audiotrim);
        playIcon = findViewById(R.id.playIcon);
        prevIcon = findViewById(R.id.prevIcon);
        nextIcon = findViewById(R.id.nextIcon);


        curListIcon = findViewById(R.id.curListIcon);

        progressDialog = new ProgressDialog(Player.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        Toast.makeText(Player.this,"onCreate",Toast.LENGTH_SHORT).show();
        audiotrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                try {
                    setAudiotrim(mSeekBar.getSelectedMinValue().intValue() * 1000, mSeekBar.getSelectedMaxValue().intValue() * 1000);

                    Toast.makeText(Player.this,"Audiotrim",Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(Player.this, e.toString(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(Player.this,"AudioTrim",Toast.LENGTH_SHORT).show();
                }

            }
        });


        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }

        playerData = getIntent();
        bundle = playerData.getExtras();

        allSongs = (ArrayList) bundle.getParcelableArrayList("songs");
        position = bundle.getInt("position", 0);
        initPlayer(position);

        curListIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent list = new Intent(getApplicationContext(),CurrentList.class);
                list.putExtra("songsList",allSongs);
                startActivity(list);

            }
        });


        playIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
        prevIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (position <= 0) {
                    position = allSongs.size() - 1;
                } else {
                    position--;
                }

                initPlayer(position);

            }
        });

        nextIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < allSongs.size() - 1) {
                    position++;
                } else {
                    position = 0;

                }
                initPlayer(position);
            }
        });

    }

    @SuppressLint("ObsoleteSdkInt")
    private void setAudiotrim(final int startMs, final int endMs) throws Exception {

        progressDialog.show();


        final String filePath;
        String filePrefix = "setAudiotrim";
        String fileExtn = ".mp3";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            ContentValues valuesaudio = new ContentValues();
            valuesaudio.put(MediaStore.Audio.Media.IS_MUSIC, "Music/" + "Folder");
            valuesaudio.put(MediaStore.Audio.Media.TITLE, filePrefix + System.currentTimeMillis());
            valuesaudio.put(MediaStore.Audio.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + fileExtn);
            valuesaudio.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
            valuesaudio.put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesaudio.put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, valuesaudio);

            File file = FileUtils.getFileFromUri(this, uri);
            filePath = file.getAbsolutePath();

        } else {

            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;

            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }

            filePath = dest.getAbsolutePath();
        }
        String cmd;

        cmd="-y -i " +allSongs+" -filter_complex [0:a]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -acodec mp3 -crf 0 -preset superfast "+filePath;
        Toast.makeText(Player.this,"setAudioTrim",Toast.LENGTH_SHORT).show();

        long executionId = FFmpeg.executeAsync(cmd, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {

                    mMediaPlayer.seekTo((int) endMs-1000);

                    Toast.makeText(Player.this,"FFmpeg_Called",Toast.LENGTH_SHORT).show();
                    audio_url = filePath;

                    mMediaPlayer.start();
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }




    private void initPlayer(final int position) {

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.reset();
        }

        String sname = allSongs.get(position).getName().replace(".mp3", "").replace(".m4a", "").replace(".wav", "").replace(".m4b", "");
        songTitle.setText(sname);
        Uri songResourceUri = Uri.parse(allSongs.get(position).toString());
        Toast.makeText(Player.this,"Initplayer",Toast.LENGTH_SHORT).show();
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), songResourceUri); // create and load mediaplayer with song resources
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                int duration = mp.getDuration() / 1000;
                //initially set the left TextView to "00:00:00"
                curTime.setText("00:00:00");
                //initially set the right Text-View to the video length
                //the getTime() method returns a formatted string in hh:mm:ss
                totTime.setText(getTime(mp.getDuration() / 1000));
                //this will run he idea in loop i.e. the video won't stop
                //when it reaches its duration

                Toast.makeText(Player.this,"OnPrepared",Toast.LENGTH_SHORT).show();

                mp.setLooping(true);

                mSeekBar.setRangeValues(0, duration);
                mSeekBar.setSelectedMinValue(0);
                mSeekBar.setSelectedMaxValue(duration);
                mSeekBar.setEnabled(true);

                mSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                        //we seek through the video when the user drags and adjusts the seekbar
                        mMediaPlayer.seekTo((int) minValue * 1000);
                        //changing the left and right TextView according to the minValue and maxValue
                        curTime.setText(getTime((int) bar.getSelectedMinValue()));
                        totTime.setText(getTime((int) bar.getSelectedMaxValue()));
                        Toast.makeText(Player.this,"SeekBar",Toast.LENGTH_SHORT).show();

                    }
                });
                final Handler handler = new Handler();
                handler.postDelayed(r = new Runnable() {
                    @Override
                    public void run() {

                        if (mMediaPlayer.getCurrentPosition() >= mSeekBar.getSelectedMaxValue().intValue() * 1000)
                            mMediaPlayer.seekTo(mSeekBar.getSelectedMinValue().intValue() * 1000);
                        handler.postDelayed(r, 1000);

                        Toast.makeText(Player.this,"Handler_called",Toast.LENGTH_SHORT).show();
                    }
                }, 1000);


                playIcon.setImageResource(R.drawable.ic_pause_black_24dp);

            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int curSongPoition = position;
                // code to repeat songs until the
                if (curSongPoition < allSongs.size() - 1) {
                    curSongPoition++;
                    initPlayer(curSongPoition);
                } else {
                    curSongPoition = 0;
                    initPlayer(curSongPoition);
                }

                //playIcon.setImageResource(R.drawable.ic_play_arrow_black_24dp);

            }
        });

        mSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {



                curTime.setText(getTime((int) bar.getSelectedMinValue()));
                totTime.setText(getTime((int) bar.getSelectedMaxValue()));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }


            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });




    }




    private void play() {

        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            playIcon.setImageResource(R.drawable.ic_pause_black_24dp);
        } else {
            pause();
        }

    }

    private void pause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            playIcon.setImageResource(R.drawable.ic_play_arrow_black_24dp);

        }

    }





    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }
}
