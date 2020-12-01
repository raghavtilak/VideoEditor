package com.raghav.gfgffmpeg;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.util.Util;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class MainActivity extends AppCompatActivity {

    ImageButton reverse,slow,fast;
    ImageView showeffects;
    Button cancel;
    TextView tvLeft,tvRight;
    ProgressDialog progressDialog;
    public static final String root= Environment.getExternalStorageDirectory().toString();
    public static final String app_folder=root+"/GFG/";

    String video_url;
    LinearLayout timelinear;
    VideoView videoView;
    int choice, videoLength;
    private Runnable r;
    private RangeSeekBar rangeSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        rangeSeekBar = (RangeSeekBar) findViewById(R.id.rangeSeekBar);

        tvLeft = (TextView) findViewById(R.id.textleft);
        tvRight = (TextView) findViewById(R.id.textright);
        slow = (ImageButton) findViewById(R.id.slow);
        reverse = (ImageButton) findViewById(R.id.reverse);
        fast = (ImageButton) findViewById(R.id.fast);
        showeffects=(ImageView)findViewById(R.id.overlayimage);
        timelinear=(LinearLayout)findViewById(R.id.lineartime);

        cancel = (Button) findViewById(R.id.cancel_button);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Intent.ACTION_PICK,

                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                startActivityForResult(intent, 123);
            }
        });

        fast = (ImageButton) findViewById(R.id.fast);
        videoView=(VideoView) findViewById(R.id.layout_movie_wrapper);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

    slow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 7;
                if (video_url != null) {
                    try {
                        executeSlowApartofVideo(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_SHORT).show();
            }
        });
        fast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 6;
                if (video_url != null) {

                    try {
                        executeFastApartofVideo(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_SHORT).show();
            }
        });
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 6;
                if (video_url != null) {
                    try {
                        executeReverseApartCommand(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_SHORT).show();
            }
        });


        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {

                videoLength = mp.getDuration();
                int duration = mp.getDuration() / 1000;
                tvLeft.setText("00:00:00");

                tvRight.setText(getTime(mp.getDuration() / 1000));
                mp.setLooping(true);

                rangeSeekBar.setRangeValues(0, duration);
                rangeSeekBar.setSelectedMinValue(0);
                rangeSeekBar.setSelectedMaxValue(duration);
                rangeSeekBar.setEnabled(true);

                rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                        videoView.seekTo((int) minValue * 1000);

                        tvLeft.setText(getTime((int) bar.getSelectedMinValue()));

                        tvRight.setText(getTime((int) bar.getSelectedMaxValue()));

                    }
                });


                final Handler handler = new Handler();
                handler.postDelayed(r = new Runnable() {
                    @Override
                    public void run() {

                        if (videoView.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue() * 1000)
                            videoView.seekTo(rangeSeekBar.getSelectedMinValue().intValue() * 1000);
                        handler.postDelayed(r, 1000);
                    }
                }, 1000);

            }
        });
    }


    private void executeReverseApartCommand(int startMs,int endMs) throws Exception {
        progressDialog.show();
        String filePrefix = "reverse";
        String fileExtn = ".mp4";

        final String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            ContentValues valuesvideos = new ContentValues();
        valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

            }else{
             filePrefix = "reverse";
             fileExtn = ".mp4";
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();
        }

        long executionId = FFmpeg.executeAsync("-y -i " + video_url + " -filter_complex [0:v]trim=0:" + endMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=" + (startMs / 1000) + ",setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.i(Config.TAG, "Async command execution completed successfully.");
                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    /**
     * Command for creating fast motion video
     */
    private void executeFastApartofVideo(int startMs,int endMs) throws Exception {
        progressDialog.show();

        final String filePath;
        String filePrefix = "fastforward";
        String fileExtn = ".mp4";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else {

            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();
        }
        String exe;
            exe="-y -i " +video_url+" -filter_complex [0:v]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast "+filePath;

        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.i(Config.TAG, "Async command execution completed successfully.");
                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    /**
     * Command for creating slow motion video for specific part of the video
     */
    private void executeSlowApartofVideo(int startMs,int endMs) throws Exception {

        progressDialog.show();
        final String filePath;
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );

        String filePrefix = "slowmotion";
        String fileExtn = ".mp4";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else {

            File dest = new File(moviesDir, filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();
        }
        String exe;
            exe="-y -i " +video_url+" -filter_complex [0:v]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=2*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast "+filePath;

        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.i(Config.TAG, "Async command execution completed successfully.");
                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == 123) {

                if (data != null) {
                    Uri uri = data.getData();
                    try {
                        File video_file = FileUtils.getFileFromUri(this, uri);
                        videoView.setVideoURI(uri);
                        videoView.start();
                        video_url=video_file.getAbsolutePath();
                        Toast.makeText(this, video_url, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }


                }
            }
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
