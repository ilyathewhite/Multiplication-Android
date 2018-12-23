package ru.mathtasks.multiplicationtable

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*


enum class TaskType { Learn, Practice, Test }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false);

        ttvLearn.setOnClickListener {
            startActivity(Intent(this, ChooseMultiplicandActivity::class.java).apply {
                putExtra(ChooseMultiplicandActivity.PARAM_TASK_TYPE, TaskType.Learn)
            })
        }
        ttvPractice.setOnClickListener {
            startActivity(Intent(this, ChooseMultiplicandActivity::class.java).apply {
                putExtra(ChooseMultiplicandActivity.PARAM_TASK_TYPE, TaskType.Practice)
            })
        }
        ttvTest.setOnClickListener {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val requiredPermission = Manifest.permission.RECORD_AUDIO
                if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(arrayOf(requiredPermission), 101)
                }
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            }
        }

        mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    tvResult.text = "PARTIAL\r\n"
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.forEach { tvResult.text = tvResult.text.toString() + it + "\r\n" }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                }

                override fun onBeginningOfSpeech() {
                    tvResult.text = "BEGIN"
                    tvResult.setBackgroundCompat(ColorDrawable(Color.MAGENTA))
                }

                override fun onEndOfSpeech() {
                    tvResult.text = "END"
                    tvResult.setBackgroundCompat(ColorDrawable(Color.LTGRAY))
                }

                override fun onError(error: Int) {
                    tvResult.text = "ERROR"
                    tvResult.setBackgroundCompat(ColorDrawable(Color.RED))
                }

                override fun onResults(results: Bundle?) {
                    tvResult.text = "FULL\r\n"
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.forEach { tvResult.text = tvResult.text.toString() + it + "\r\n" }
                }

                override fun onReadyForSpeech(params: Bundle?) {
                    tvResult.text = "READY"
                    tvResult.setBackgroundCompat(ColorDrawable(Color.GREEN))
                }
            })
        }
    }

    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private var mSpeechRecognizerIntent: Intent? = null
    private val mIsListening: Boolean = false

    override fun onDestroy() {
        super.onDestroy()
        mSpeechRecognizer?.destroy();
    }
}