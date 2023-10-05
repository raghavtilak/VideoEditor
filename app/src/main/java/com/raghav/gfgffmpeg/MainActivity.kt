package com.raghav.gfgffmpeg

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.raghav.gfgffmpeg.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private var input_video_uri: String? = null
    lateinit var binding: ActivityMainBinding
    val handler = Handler(Looper.getMainLooper())

    var videoMinVal = mutableFloatStateOf(0f)
    var videoMaxVal = mutableFloatStateOf(1f)
    var videoSelectedLowerVal = mutableFloatStateOf(0f)
    var videoSelectedUpperVal = mutableFloatStateOf(1f)

    //create an intent launcher to retrieve the video file from the device storage
    private val selectVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let {
                input_video_uri = FFmpegKitConfig.getSafParameterForRead(this, it)
                binding.videoView.setVideoURI(it)

                //after successful retrieval of the video and properly setting up the retried video uri in
                //VideoView, Start the VideoView to play that video
                binding.videoView.start()
            }
        }

    //create an intent launcher to save the video file in the device storage
    private val saveVideoLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) {
            it?.let {
                val out = contentResolver.openOutputStream(it)
                val ip: InputStream = FileInputStream(input_video_uri)

                //com.google.common.io.ByteStreams, also provides a direct method to copy
                // all bytes from the input stream to the output stream. Does not close or
                // flush either stream.
                // copy(ip,out!!)

                out?.let {
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (ip.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    ip.close()
                    // write the output file (You have now copied the file)
                    out.flush()
                    out.close()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeControlPanel.setContent {
            Column {
                ScrubberPanel(
                    lowerValue = videoSelectedLowerVal.floatValue,
                    upperValue = videoSelectedUpperVal.floatValue,
                    from = videoMinVal.floatValue,
                    to = videoMaxVal.floatValue
                ) { lower, upper ->
                    videoSelectedLowerVal.floatValue = lower
                    videoSelectedUpperVal.floatValue = upper
                    binding.videoView.seekTo(videoSelectedLowerVal.floatValue.toInt() * 1000)
                }
                ControlPanelButtons(
                    firstClick = {
                        if (input_video_uri != null) {
                            slowMotion(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    },
                    secondClick = {
                        if (input_video_uri != null) {
                            reverse(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    },
                    thirdClick = {
                        if (input_video_uri != null) {
                            fastForward(
                                videoSelectedLowerVal.floatValue.toInt() * 1000,
                                videoSelectedUpperVal.floatValue.toInt() * 1000
                            )
                        } else Toast.makeText(
                            this@MainActivity,
                            "Please upload video",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                )
            }
        }

        binding.saveVideo.setOnClickListener {
            if (input_video_uri != null) {
                //passing filename
                saveVideoLauncher.launch("VID-${System.currentTimeMillis() / 1000}")
            } else Toast.makeText(this@MainActivity, "Please upload video", Toast.LENGTH_LONG)
                .show()
        }
        binding.selectVideo.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            selectVideoLauncher.launch("video/*")
        }

        /*
            set up the VideoView.
            We will be using VideoView to view our video.
         */
        binding.videoView.setOnPreparedListener { mp ->
            val duration = mp.duration / 1000
            videoMinVal.floatValue = 0f
            videoMaxVal.floatValue = mp.duration / 1000f
            videoSelectedLowerVal.floatValue = 0f
            videoSelectedUpperVal.floatValue = mp.duration / 1000f
            mp.isLooping = true
            handler.postDelayed(object : Runnable {
                override fun run() {
                    val time: Int = abs(duration - binding.videoView.currentPosition) / 1000

                    if (binding.videoView.currentPosition >= videoSelectedUpperVal.floatValue.toInt() * 1000) {
                        binding.videoView.seekTo(videoSelectedLowerVal.floatValue.toInt() * 1000)
                    }
                    handler.postDelayed(this, 1000)
                }
            }, 0)
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


        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            ("-y -i $input_video_uri -filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim=${endMs / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startMs / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startMs / 1000}:${endMs / 1000},asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim=${endMs / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}")
        executeFfmpegCommand(exe, file.absolutePath)
    }

    /**
     * Method for creating slow motion video for specific part of the video
     * The below code is same as above only the command in string "exe" is changed.
     */
    private fun slowMotion(startMs: Int, endMs: Int) {
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            ("-y -i $input_video_uri -filter_complex [0:v]trim=0:${startMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},setpts=2*(PTS-STARTPTS)[v2];[0:v]trim=${endMs / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startMs / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startMs / 1000}:${endMs / 1000},asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim=${endMs / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}")
        executeFfmpegCommand(exe, file.absolutePath)
    }

    /**
     * Method for reversing the video
     * The below code is same as above only the command is changed.
     */
    private fun reverse(startMs: Int, endMs: Int) {
        val folder = cacheDir
        val file = File(folder, System.currentTimeMillis().toString() + ".mp4")
        val exe =
            "-y -i $input_video_uri -filter_complex [0:v]trim=0:${endMs / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startMs / 1000}:${endMs / 1000},reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=${startMs / 1000},setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 -b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast ${file.absolutePath}"
        executeFfmpegCommand(exe, file.absolutePath)
    }

    private fun executeFfmpegCommand(exe: String, filePath: String) {

        //creating the progress dialog
        val progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */
        FFmpegKit.executeAsync(exe, { session ->
            val returnCode = session.returnCode
            lifecycleScope.launch(Dispatchers.Main) {
                if (returnCode.isValueSuccess) {
                    //after successful execution of ffmpeg command,
                    //again set up the video Uri in VideoView
                    binding.videoView.setVideoPath(filePath)
                    //change the video_url to filePath, so that we could do more manipulations in the
                    //resultant video. By this we can apply as many effects as we want in a single video.
                    //Actually there are multiple videos being formed in storage but while using app it
                    //feels like we are doing manipulations in only one video
                    input_video_uri = filePath
                    //play the result video in VideoView
                    binding.videoView.start()
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Filter Applied", Toast.LENGTH_SHORT).show()
                } else {
                    progressDialog.dismiss()
                    Log.d("TAG", session.allLogsAsString)
                    Toast.makeText(this@MainActivity, "Something Went Wrong!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }, { log ->
            lifecycleScope.launch(Dispatchers.Main) {
                progressDialog.setMessage("Applying Filter..${log.message}")
            }
        }) { statistics -> Log.d("STATS", statistics.toString()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            deleteTempFiles(cacheDir)
        }
    }

    /*
    *
    * Function to delete all the temporary files made during the app session
    *
    * */
    private fun deleteTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    deleteTempFiles(it)
                } else {
                    it.delete()
                }
            }
        }
        return file.delete()
    }

}