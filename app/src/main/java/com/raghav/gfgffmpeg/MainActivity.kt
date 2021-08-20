package com.raghav.gfgffmpeg

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.app.ProgressDialog
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import kotlin.Throws
import android.net.Uri
import android.os.*
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import com.raghav.gfgffmpeg.databinding.ActivityMainBinding
import timber.log.Timber
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
                    Timber.e(e)
                }
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG).show()
        }
        binding.fast.setOnClickListener {
            if (videoUrl != null) {
                try {
                    fastForward(binding.rangeSeekBar.selectedMinValue.toInt() * 1000, binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                } catch (e: Exception) {
                    Timber.e(e)
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                }
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG).show()
        }
        binding.reverse.setOnClickListener {
            if (videoUrl != null) {
                try {
                    reverse(binding.rangeSeekBar.selectedMinValue.toInt() * 1000, binding.rangeSeekBar.selectedMaxValue.toInt() * 1000)
                } catch (e: Exception) {
                    Timber.e(e)
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
            binding.textright.text = (mp.duration / 1000).getTime()
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
                binding.textleft.text = (bar.selectedMinValue as Int).getTime()
                binding.textright.text = (bar.selectedMaxValue as Int).getTime()
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

    /* startMs is the starting time, from where we have to apply the effect.
       endMs is the ending time, till where we have to apply effect.
       For example, we have a video of 5min and we only want to fast forward a part of video
       say, from 1:00 min to 2:00min, then our startMs will be 1000ms and endMs will be 2000ms.
    */
    private fun fastForward(startMs: Int, endMs: Int) {
        progressDialog.show()

        //creating a new file in storage
        val filePath = getFilePath("fastforward")

        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        val exe: String = "-y -i ${Uri.parse(videoUrl)} " +
                "-filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000},:${endMs / 1000},,setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=" + endMs / 1000 + ",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:" + startMs / 1000 + ",asetpts=PTS-STARTPTS[a1];[0:a]atrim=" + startMs / 1000 + ":" + endMs / 1000 + ",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=" + endMs / 1000 + ",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " +
                "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast $filePath"
        proceed(exe, "FastForward")
    }

    /**
     * Method for creating slow motion video for specific part of the video
     * The below code is same as above only the command in string "exe" is changed.
     */
    @Throws(Exception::class)
    private fun slowMotion(startMs: Int, endMs: Int) {
        progressDialog.show()
        val filePath = getFilePath("slowmotion")

        val exe: String = "-y -i ${Uri.parse(videoUrl)} " +
                "-filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=${endMs / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startMs / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startMs / 1000}:${endMs / 1000},asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim=${endMs / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 " +
                "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast $filePath"

        proceed(exe, "SlowMotion")
    }

    @Throws(Exception::class)
    private fun reverse(startMs: Int, endMs: Int) {
        progressDialog.show()
        val filePath = getFilePath("reverse")

        val exe = "-y -i ${Uri.parse(videoUrl)} " +
                "-filter_complex [0:v]trim=0:" + endMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=" + startMs / 1000 + ",setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath
        proceed(exe, "Reverse")
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
                        //get the file from the Uri using getFileFromUri() methid present in FileUils.java
                        //now set the video uri in the VideoView
                        binding.videoView.setVideoURI(uri)
                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        binding.videoView.start()
                        //get the absolute path of the video file. We will require this as an input argument in
                        //the ffmpeg command.
                        videoUrl = FileManager.getPath(this, uri!!)
                    } catch (e: Exception) {
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                        Timber.e(e)
                    }
                }
            }
        }
    }

    private fun getFilePath(filePrefix: String): String {
        val filePath: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valuesVideos = ContentValues()
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "ffmpeg")
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis())
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix + System.currentTimeMillis() + MP4_EXTENSION)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos)

            filePath = FileManager.getPath(this, uri!!)!!
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

    private fun proceed(exe: String, caption: String) {
        FFmpegKit.executeAsync(exe, { session ->
            when {
                session.returnCode.isSuccess -> {
                    progressDialog.dismiss()
                    videoUrl = session.arguments.last()
                    Timber.d("$videoUrl fileSize=${File(videoUrl).length()}")
                    binding.videoView.apply {
                        stopPlayback()
                        setVideoURI(Uri.parse(session.arguments.last()))
                        start()
                    }
                }
                session.returnCode.isError -> {
                    val text = exe.replace(" -", "\n -")
                    Timber.e("Execution error: $text")
                    val snackBar = Snackbar.make(
                        findViewById(android.R.id.content),
                        text.take(100),
                        Snackbar.LENGTH_LONG
                    )
                    snackBar.show()
                }
                session.returnCode.isCancel -> Timber.i("Execution cancelled by user.")
                else -> Timber.e("Execution failed returnCode=${session.returnCode} i=$videoUrl ${session.arguments.last()}")
            }
            progressDialog.dismiss()
        }, {
            if (it.level.name.startsWith("AV_LOG_ERROR")) {
                Timber.e("${it.level} ${it.message}")
                val snackBar = Snackbar.make(
                    findViewById(android.R.id.content),
                    it.message,
                    Snackbar.LENGTH_INDEFINITE
                )
                snackBar.setAction(android.R.string.ok) {
                    snackBar.dismiss()
                }
                snackBar.show()
            } else
                Timber.d("${it.level} ${it.message}")
        }, {
            Handler(Looper.getMainLooper()).post {
                progressDialog.setMessage("$caption ... #${it.videoFrameNumber} ${it.size.humanReadableByteCountSI()}")
            }
            Timber.v(it.toString())
        })
    }

    companion object {
        private val root = Environment.getExternalStorageDirectory().toString()
        private val app_folder = "$root/GFG/"
        private const val REQUEST_TAKE_GALLERY_VIDEO = 123
        private const val MP4_EXTENSION = ".mp4"
    }
}
