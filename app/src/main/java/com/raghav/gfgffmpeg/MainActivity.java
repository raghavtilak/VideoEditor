package com.raghav.gfgffmpeg;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    private static final int WRITE_REQUEST_CODE = 111;
    private static final int SELECT_REQUEST_CODE = 222;

    private ImageButton reverse, slow, fast;
    private Button selectVideo, saveVideo;
    private TextView tvLeft, tvRight;
    private String input_video_uri;
    private VideoView videoView;
    private Runnable r;
    private RangeSeekBar rangeSeekBar;
    private double videoLength = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        tvLeft = findViewById(R.id.textleft);
        tvRight = findViewById(R.id.textright);
        slow = findViewById(R.id.slow);
        reverse = findViewById(R.id.reverse);
        fast = findViewById(R.id.fast);
        selectVideo = findViewById(R.id.selectVideo);
        saveVideo = findViewById(R.id.saveVideo);
        fast = findViewById(R.id.fast);
        videoView = findViewById(R.id.layout_movie_wrapper);

        //set up the onClickListeners
        saveVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (input_video_uri != null) {
                    createFile("VID-" + System.currentTimeMillis() / 1000);
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });
        selectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create an intent to retrieve the video file from the device storage
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                startActivityForResult(intent, SELECT_REQUEST_CODE);
            }
        });

        slow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                    check if the user has selected any video or not
                    In case a user hasen't selected any video and press the button,
                    we will show an warning, stating "Please upload the video"
                 */
                if (input_video_uri != null) {

                    slowMotion(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);

                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });
        fast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input_video_uri != null) {
                    fastForward(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input_video_uri != null) {
                    reverse(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });

//        // create an object of media controller
//        MediaController mediaController = new MediaController(this);
//        // set media controller object for a video view
//        videoView.setMediaController(mediaController);

        /*
            set up the VideoView.
            We will be using VideoView to view our video.
         */

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                //get the duration of the video
                int duration = mp.getDuration() / 1000;
                //initially set the left TextView to "00:00:00"
                tvLeft.setText("00:00:00");
                //initially set the right Text-View to the video length
                //the getTime() method returns a formatted string in hh:mm:ss
                tvRight.setText(getTime(mp.getDuration() / 1000));
                //this will run he ideo in loop i.e. the video won't stop
                //when it reaches its duration
                mp.setLooping(true);

                //set up the initial values of rangeSeekbar
                rangeSeekBar.setRangeValues(0, duration);
                rangeSeekBar.setSelectedMinValue(0);
                rangeSeekBar.setSelectedMaxValue(duration);
                rangeSeekBar.setEnabled(true);

                rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                        //we seek through the video when the user drags and adjusts the seekbar
                        videoView.seekTo((int) minValue * 1000);
                        //changing the left and right TextView according to the minValue and maxValue
                        tvLeft.setText(getTime((int) bar.getSelectedMinValue()));
                        tvRight.setText(getTime((int) bar.getSelectedMaxValue()));

                    }
                });

                //this method changes the right TextView every 1 second as the video is being played
                //It works same as a time counter we see in any Video Player
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


    /**
     * Method for creating fast motion video
     */
    private void fastForward(int startMs, int endMs) {
          /* startMs is the starting time, from where we have to apply the effect.
  	         endMs is the ending time, till where we have to apply effect.
   	         For example, we have a video of 5min and we only want to fast forward a part of video
  	         say, from 1:00 min to 2:00min, then our startMs will be 1000ms and endMs will be 2000ms.
		 */


        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.

        File folder = getCacheDir();
        File file = new File(folder, System.currentTimeMillis() + ".mp4");

        String exe = "-y -i " + input_video_uri + " -filter_complex [0:v]trim=0:" + startMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim="
                + startMs / 1000 + ":" + endMs / 1000 + ",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=" + (endMs / 1000) +
                ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + (startMs / 1000) + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + (startMs / 1000)
                + ":" + (endMs / 1000) + ",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=" + (endMs / 1000) +
                ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + file.getAbsolutePath();

        executeFfmpegCommand(exe, file.getAbsolutePath());
    }

    /**
     * Method for creating slow motion video for specific part of the video
     * The below code is same as above only the command in string "exe" is changed.
     */
    private void slowMotion(int startMs, int endMs) {

        File folder = getCacheDir();
        File file = new File(folder, System.currentTimeMillis() + ".mp4");

        String exe = "-y -i " + input_video_uri + " -filter_complex [0:v]trim=0:" + startMs / 1000 +
                ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 +
                ",setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=" + (endMs / 1000) +
                ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + (startMs / 1000) +
                ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + (startMs / 1000) + ":"
                + (endMs / 1000) + ",asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim="
                + (endMs / 1000) + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "
                + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + file.getAbsolutePath();

        executeFfmpegCommand(exe, file.getAbsolutePath());
    }

    /**
     * Method for reversing the video
     */
    /*
	The below code is same as above only the command is changed.
*/
    private void reverse(int startMs, int endMs) {

        File folder = getCacheDir();
        File file = new File(folder, System.currentTimeMillis() + ".mp4");

        String exe = "-y -i " + input_video_uri + " -filter_complex [0:v]trim=0:" + endMs / 1000 +
                ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 +
                ",reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=" + (startMs / 1000) +
                ",setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 " +
                "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + file.getAbsolutePath();

        executeFfmpegCommand(exe, file.getAbsolutePath());
    }

    //Overriding the method onActivityResult() to get the video Uri form intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (data != null) {

                Uri uri = data.getData();
                if (requestCode == WRITE_REQUEST_CODE) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    new AsyncTask(MainActivity.this) {
                        @Override
                        public void doInBackground() {
                            try {
                                OutputStream out = getContentResolver().openOutputStream(uri);
                                InputStream in = new FileInputStream(input_video_uri);

                                byte[] buffer = new byte[1024];
                                int read;
                                while ((read = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, read);
                                }
                                in.close();
                                // write the output file (You have now copied the file)
                                out.flush();
                                out.close();

                            } catch (IOException e) {
                                Log.d("TAG", "Error Occured" + e.getMessage());

                            }
                        }

                        @Override
                        public void onPostExecute() {

                        }
                    }.execute();

                } else {

                    try {

                        input_video_uri = FFmpegKitConfig.getSafParameterForRead(this, uri);

                        videoView.setVideoURI(uri);

                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        videoView.start();

                    } catch (Exception e) {
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                }

            }
        }
    }

    //This method returns the seconds in hh:mm:ss time format
    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }


    public void createFile(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // file type
        intent.setType("video/mp4");
        // file name
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    private void executeFfmpegCommand(String exe, String filePath) {

        //creating the progress dialog
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */

        FFmpegKit.executeAsync(exe, new ExecuteCallback() {
            @Override
            public void apply(Session session) {
                ReturnCode returnCode = session.getReturnCode();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (returnCode.isSuccess()) {


                            //after successful execution of ffmpeg command,
                            //again set up the video Uri in VideoView
                            videoView.setVideoPath(filePath);
                            //change the video_url to filePath, so that we could do more manipulations in the
                            //resultant video. By this we can apply as many effects as we want in a single video.
                            //Actually there are multiple videos being formed in storage but while using app it
                            //feels like we are doing manipulations in only one video
                            input_video_uri = filePath;
                            //play the result video in VideoView
                            videoView.start();

                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Filter Applied", Toast.LENGTH_SHORT).show();
                        } else {
                            progressDialog.dismiss();
                            Log.d("TAG", session.getAllLogsAsString());
                            Toast.makeText(MainActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }, new LogCallback() {
            @Override
            public void apply(com.arthenica.ffmpegkit.Log log) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        progressDialog.setMessage("Applying Filter..\n"+log.getMessage());

                    }
                });
            }
        }, new StatisticsCallback() {
            @Override
            public void apply(Statistics statistics) {

                android.util.Log.d("STATS", statistics.toString());

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!isChangingConfigurations()) {
            deleteTempFiles(getCacheDir());
        }
    }

    /*
    *
    * Function to delete all the temporary files made during the app session
    *
    * */
    private boolean deleteTempFiles(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteTempFiles(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        return file.delete();
    }

}
