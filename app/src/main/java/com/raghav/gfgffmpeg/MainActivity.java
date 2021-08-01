package com.raghav.gfgffmpeg;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_TAKE_GALLERY_VIDEO = 123;
    private ImageButton reverse,slow,fast;
    private Button selectVideo;
    private TextView tvLeft,tvRight;
    private ProgressDialog progressDialog;
    private String video_url;
    private VideoView videoView;
    private Runnable r;
    private RangeSeekBar rangeSeekBar;
    private static final String root = Environment.getExternalStorageDirectory().toString();
    private static final String app_folder = root + "/GFG/";

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
        fast = findViewById(R.id.fast);
        videoView = findViewById(R.id.layout_movie_wrapper);

        //creating the progress dialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        //set up the onClickListeners
        selectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create an intent to retrieve the video file from the device storage
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
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
                if (video_url != null) {
                    //a try-catch block to handle all necessary exceptions like File not found, IOException
                    try {
                        slowMotion(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });
        fast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (video_url != null) {

                    try {
                        fastForward(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (video_url != null) {
                    try {
                        reverse(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_LONG).show();
            }
        });

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

        //create a progress dialog and show it until this method executes.
        progressDialog.setMessage("Convert FastForward...");
        progressDialog.show();

        //creating a new file in storage
        final String filePath;
        String filePrefix = "fastforward";
        String fileExtn = ".mp4";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /*
            With introduction of scoped storage in Android Q the primitive method gives error
            So, it is recommended to use the below method to create a video file in storage.
             */
            ContentValues valuesVideos = new ContentValues();
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis());
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + fileExtn);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos);

            //get the path of the video file created in the storage.
            File file = FileUtils.getFileFromUri(this, uri);
            filePath = file.getAbsolutePath();

        } else {
            //This else statement will work for devices with Android version lower than 10
            //Here, "app_folder" is the path to your app's root directory in device storage
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            //check if the file name previously exist. Since we don't want to oerwrite the video files
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            //Get the filePath once the file is successfully created.
            filePath = dest.getAbsolutePath();
        }
        String exe;
        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        exe = "-y -i " + video_url + " -filter_complex [0:v]trim=0:" + startMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=" + (endMs / 1000) + ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + (startMs / 1000) + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + (startMs / 1000) + ":" + (endMs / 1000) + ",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=" + (endMs / 1000) + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath;

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */
        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    //after successful execution of ffmpeg command,
                    //again set up the video Uri in VideoView
                    videoView.setVideoURI(Uri.parse(filePath));
                    //change the video_url to filePath, so that we could do more manipulations in the
                    //resultant video. By this we can apply as many effects as we want in a single video.
                    //Actually there are multiple videos being formed in storage but while using app it
                    //feels like we are doing manipulations in only one video
                    video_url = filePath;
                    //play the result video in VideoView
                    videoView.start();

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
                progressDialog.dismiss();
            }
        });
    }

    /**
     * Method for creating slow motion video for specific part of the video
     * The below code is same as above only the command in string "exe" is changed.
     */
    private void slowMotion(int startMs, int endMs) throws Exception {

        progressDialog.setMessage("Convert slow motion...");
        progressDialog.show();

        final String filePath;
        String filePrefix = "slowmotion";
        String fileExtn = ".mp4";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues valuesVideos = new ContentValues();
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis());
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + fileExtn);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos);
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
        String exe;
        exe = "-y -i " + video_url + " -filter_complex [0:v]trim=0:" + startMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=" + (endMs / 1000) + ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + (startMs / 1000) + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + (startMs / 1000) + ":" + (endMs / 1000) + ",asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim=" + (endMs / 1000) + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath;

        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {

                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(TAG, "Execution cancelled by user.");
                } else {
                    Log.e(TAG, String.format("Execution failed returnCode=%d i=%s %s", returnCode, video_url, filePath));
                }
                progressDialog.dismiss();
            }
        });
    }

    /**
     * Method for reversing the video
     */
    /*
	The below code is same as above only the command is changed.
*/
    private void reverse(int startMs, int endMs) throws Exception {

        progressDialog.setMessage("Convert reverse...");
        progressDialog.show();
        String filePrefix = "reverse";
        String fileExtn = ".mp4";

        final String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues valuesVideos = new ContentValues();
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis());
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + fileExtn);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos);
            File file = FileUtils.getFileFromUri(this, uri);
            filePath = file.getAbsolutePath();

        } else {
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
                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
                progressDialog.dismiss();
            }
        });
    }

    //Overriding the method onActivityResult() to get the video Uri form intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {

                if (data != null) {
                    //get the video Uri
                    Uri uri = data.getData();
                    try {
                        String filemanagerstring = uri.getPath();
                        //get the file from the Uri using getFileFromUri() methid present in FileUils.java
                        //now set the video uri in the VideoView
                        videoView.setVideoURI(uri);
                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        videoView.start();
                        //get the absolute path of the video file. We will require this as an input argument in
                        //the ffmpeg command.
                        video_url = filemanagerstring;
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
}
