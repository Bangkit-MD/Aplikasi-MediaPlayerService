package com.kevintekno.mymediaplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.kevintekno.mymediaplayer.databinding.ActivityMainBinding
import java.io.IOException
import java.lang.ref.WeakReference

class MediaService : Service(), MediaPlayerCallback {
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var binding: ActivityMainBinding
    private var isReady = false
    private lateinit var btnPlay:ImageButton
    companion object{
        const val ACTION_CREATE = "com.kevintekno.mymediaplayer.MediaService.create"
        const val ACTION_DESTROY = "com.kevintekno.mymediaplayer.MediaService.destroy"
        const val TAG = "MediaService"
        const val PLAY = 0
        const val STOP = 1
        const val CHANNEL_DEFAULT_IMPORTANCE = "Channel_Test"
        const val ONGOING_NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        btnPlay = binding.btnPlay

    }
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG,"onBind: ")
        return mMesengger.binder
    }
    private val mMesengger = Messenger(IncomingHandler(this))

    internal class IncomingHandler(playerCallback: MediaPlayerCallback):Handler(
        Looper.getMainLooper()){

        private val mediaPlayerCallbackWeakReference: WeakReference<MediaPlayerCallback> = WeakReference(playerCallback)

        override fun handleMessage(msg: Message) {
            when(msg.what){
                PLAY -> mediaPlayerCallbackWeakReference.get()?.onPlay()
                STOP -> mediaPlayerCallbackWeakReference.get()?.onStop()
                else -> super.handleMessage(msg)
            }
        }
    }
    private fun showNotif(){
        val notificatioinIntent = Intent(this, MainActivity::class.java)
        notificatioinIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT

        val pendingIntent = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            PendingIntent.getActivity(this, 0 , notificatioinIntent, PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(this, 0, notificatioinIntent, 0)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentTitle("Music")
            .setContentText("Play Japanese music")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Test")
            .build()
        createChannel(CHANNEL_DEFAULT_IMPORTANCE)
        startForeground(ONGOING_NOTIFICATION_ID,notification)

    }

    private fun createChannel(CHANNEL_ID:String){
        val mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel= NotificationChannel(CHANNEL_ID, "Battery", NotificationManager.IMPORTANCE_DEFAULT)
            channel.setShowBadge(false)
            channel.setSound(null, null)
            mNotificationManager.createNotificationChannel(channel)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action != null){
            when(action){
                ACTION_CREATE -> if (mMediaPlayer == null){
                    init()
                }
                ACTION_DESTROY -> if(mMediaPlayer?.isPlaying as Boolean){
                    stopSelf()
                }
                else -> {
                    init()
                }
            }
        }
        Log.d(TAG, "onStartCommand: ")
        return flags
    }
    private fun init(){
        mMediaPlayer = MediaPlayer()
        val attribute = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        mMediaPlayer?.setAudioAttributes(attribute)
        val afd = applicationContext.resources.openRawResourceFd(R.raw.song_orange)
        try{
            mMediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }catch (e: IOException){
            e.printStackTrace()
        }
        mMediaPlayer?.setOnPreparedListener{
            isReady = true
            mMediaPlayer?.start()
            showNotif()

        }
        mMediaPlayer?.setOnErrorListener{_,_,_, -> false}
    }

    override fun onPlay() {
        if (!isReady){
            mMediaPlayer?.prepareAsync()
        }else{
            if (mMediaPlayer?.isPlaying as Boolean){
                mMediaPlayer?.pause()
                btnPlay.setImageResource(R.drawable.baseline_play_24)

            }else{
                mMediaPlayer?.start()
                btnPlay.setImageResource(R.drawable.baseline_pause_24)
                showNotif()
            }
        }
    }
    private fun stopNotif() {
        stopForeground(true)
    }
    override fun onStop() {
        if (mMediaPlayer?.isPlaying as Boolean || isReady){
            mMediaPlayer?.stop()
            stopNotif()
            isReady = false
        }
    }

}