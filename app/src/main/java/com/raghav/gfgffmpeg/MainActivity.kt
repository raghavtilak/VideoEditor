package com.raghav.gfgffmpeg

import android.annotation.SuppressLint
import com.raghav.gfgffmpeg.FileUtils.getFileFromUri
import androidx.appcompat.app.AppCompatActivity
import android.app.ProgressDialog
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import kotlin.Throws
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.*
import com.raghav.gfgffmpeg.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog
    private var videoUrl: String? = null
    private var runnable: Runnable? = null

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //creating the progress dialog
        progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setMessage("Please wait..")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)

        binding.version.text = Build.SUPPORTED_ABIS.toList().toString()

        binding.selectVideo.setOnClickListener { //create an intent to retrieve the video file from the device storage
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO)
        }
        binding.slow.setOnClickListener {

            //check if the user has selected any video or not
            //In case a user hasn't selected any video and press the button,
            //we will show an warning, stating "Please upload the video"
            if (videoUrl != null) {
                //a try-catch block to handle all necessary exceptions like File not found, IOException
                try {
                    slowMotion(binding.rangeSeekBar.selectedMinValue.toInt() * 1000, binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG).show()
        }
        binding.fast.setOnClickListener {
            if (videoUrl != null) {
                try {
                    fastForward(binding.rangeSeekBar.selectedMinValue.toInt() * 1000, binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                }
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG).show()
        }
        binding.reverse.setOnClickListener {
            if (videoUrl != null) {
                try {
                    reverse(binding.rangeSeekBar.selectedMinValue.toInt() * 1000, binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                }
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG).show()
        }

        /*
            set up the VideoView.
            We will be using VideoView to view our video.
         */
        binding.videoView.setOnPreparedListener { mp -> //get the duration of the video
            val duration = mp.duration / 1000
            //initially set the left TextView to "00:00:00"
            binding.textleft.text = "00:00:00"
            //initially set the right Text-View to the video length
            //the getTime() method returns a formatted string in hh:mm:ss
            binding.textright.text = getTime(mp.duration / 1000)
            //this will run he ideo in loop i.e. the video won't stop
            //when it reaches its duration
            mp.isLooping = true

            //set up the initial values of rangeSeekbar
            binding.rangeSeekBar.setRangeValues(0, duration)
            binding.rangeSeekBar.selectedMinValue = 0
            binding.rangeSeekBar.selectedMaxValue = duration
            binding.rangeSeekBar.isEnabled = true
            binding.rangeSeekBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue -> //we seek through the video when the user drags and adjusts the seekbar
                binding.videoView.seekTo(minValue as Int * 1000)
                //changing the left and right TextView according to the minValue and maxValue
                binding.textleft.text = getTime(bar.selectedMinValue as Int)
                binding.textright.text = getTime(bar.selectedMaxValue as Int)
            }

            //this method changes the right TextView every 1 second as the video is being played
            //It works same as a time counter we see in any Video Player
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(Runnable {
                if (binding.videoView.currentPosition >= binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                    binding.videoView.seekTo(binding.rangeSeekBar.selectedMinValue.toInt() * 1000)
                handler.postDelayed(runnable!!, 1000)
            }.also { runnable = it }, 1000)
        }
    }

    /**
     * Method for creating fast motion video
     */
    private fun fastForward(startMs: Int, endMs: Int) {
        /* startMs is the starting time, from where we have to apply the effect.
  	         endMs is the ending time, till where we have to apply effect.
   	         For example, we have a video of 5min and we only want to fast forward a part of video
  	         say, from 1:00 min to 2:00min, then our startMs will be 1000ms and endMs will be 2000ms.
		 */

        //create a progress dialog and show it until this method executes.
        progressDialog.setMessage("Convert FastForward...")
        progressDialog.show()

        //creating a new file in storage
        val filePath = getFilePath("fastforward")

        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        val exe: String =
            "-y -i " + videoUrl + " -filter_complex [0:v]trim=0:" + startMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=" + endMs / 1000 + ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + startMs / 1000 + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + startMs / 1000 + ":" + endMs / 1000 + ",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=" + endMs / 1000 + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */FFmpegKit.executeAsync(exe) { session ->
            when {
                session.returnCode.isSuccess -> {
                    //after successful execution of ffmpeg command,
                    //again set up the video Uri in VideoView
                    binding.videoView.setVideoURI(Uri.parse(filePath))
                    //change the video_url to filePath, so that we could do more manipulations in the
                    //resultant video. By this we can apply as many effects as we want in a single video.
                    //Actually there are multiple videos being formed in storage but while using app it
                    //feels like we are doing manipulations in only one video
                    videoUrl = filePath
                    //play the result video in VideoView
                    binding.videoView.start()
                }
                session.returnCode.isCancel -> Log.i(TAG, "Async command execution cancelled by user.")
                else -> Log.i(TAG, String.format("Async command execution failed with returnCode=%d.", session.returnCode))

            }
            progressDialog.dismiss()
        }
    }

    /**
     * Method for creating slow motion video for specific part of the video
     * The below code is same as above only the command in string "exe" is changed.
     */
    @Throws(Exception::class)
    private fun slowMotion(startMs: Int, endMs: Int) {
        progressDialog.setMessage("Convert slow motion...")
        progressDialog.show()
        val filePath = getFilePath("slowmotion")

        val exe: String =
            "-y -i " + videoUrl + " -filter_complex [0:v]trim=0:" + startMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=" + endMs / 1000 + ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + startMs / 1000 + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + startMs / 1000 + ":" + endMs / 1000 + ",asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim=" + endMs / 1000 + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath
        FFmpegKit.executeAsync(exe) { session ->
            when {
                session.returnCode.isSuccess -> {
                    binding.videoView.setVideoURI(Uri.parse(filePath))
                    videoUrl = filePath
                    binding.videoView.start()
                }
                session.returnCode.isCancel -> Log.i(TAG, "Execution cancelled by user.")
                else -> Log.e(TAG, String.format("Execution failed returnCode=%d i=%s %s", session.returnCode, videoUrl, filePath))
            }
            progressDialog.dismiss()
        }
    }

    /**
     * Method for reversing the video
     */
    /*
	The below code is same as above only the command is changed.
*/
    @Throws(Exception::class)
    private fun reverse(startMs: Int, endMs: Int) {
        progressDialog.setMessage("Convert reverse...")
        progressDialog.show()
        val filePath = getFilePath("reverse")

        FFmpegKit.executeAsync("-y -i " + videoUrl + " -filter_complex [0:v]trim=0:" + endMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=" + startMs / 1000 + ",setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath) { session ->
            when {
                session.returnCode.isSuccess -> {
                    binding.videoView.setVideoURI(Uri.parse(filePath))
                    videoUrl = filePath
                    binding.videoView.start()
                }
                session.returnCode.isCancel -> Log.i(TAG, "Async command execution cancelled by user.")
                else -> Log.i(TAG, String.format("Async command execution failed with returnCode=%d.", session.returnCode))
            }
            progressDialog.dismiss()
        }
    }

    //Overriding the method onActivityResult() to get the video Uri form intent.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                if (data != null) {
                    //get the video Uri
                    val uri = data.data
                    try {
                        val filemanagerstring = uri!!.path
                        //get the file from the Uri using getFileFromUri() methid present in FileUils.java
                        //now set the video uri in the VideoView
                        binding.videoView.setVideoURI(uri)
                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        binding.videoView.start()
                        //get the absolute path of the video file. We will require this as an input argument in
                        //the ffmpeg command.
                        videoUrl = filemanagerstring
                    } catch (e: Exception) {
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    //This method returns the seconds in hh:mm:ss time format
    private fun getTime(seconds: Int): String {
        val hr = seconds / 3600
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec)
    }

    private fun getFilePath(filePrefix: String): String {
        var filePath = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valuesVideos = ContentValues()
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder")
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis())
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + MP4_EXTENSION)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos)

            val file = getFileFromUri(this, uri!!)
            filePath = file.absolutePath
        } else {
            var dest = File(File(app_folder), filePrefix + MP4_EXTENSION)
            var fileNo = 0
            while (dest.exists()) {
                fileNo++
                dest = File(File(app_folder), filePrefix + fileNo + MP4_EXTENSION)
            }
            filePath = dest.absolutePath
        }
        return filePath
    }

    companion object {
        private val root = Environment.getExternalStorageDirectory().toString()
        private val app_folder = "$root/GFG/"
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_TAKE_GALLERY_VIDEO = 123
        private const val MP4_EXTENSION = ".mp4"
    }
}
