package com.cometchat.pro.uikit.ui_components.messages.media_view

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.uikit.R
import com.cometchat.pro.uikit.ui_resources.constants.UIKitConstants
import com.cometchat.pro.uikit.ui_resources.utils.Utils

class CometChatMediaViewActivity : AppCompatActivity() {
    private lateinit var imageMessage: ImageView
    private lateinit var videoMessage: VideoView
    private lateinit var toolbar: Toolbar
    private var senderName: String? = null
    private var sentAt: Long = 0
    private var mediaUrl: String? = null
    private var mediaType: String? = null

    private var Id: String? = null
    private var type: String? = null
    private var allowCaption = false
    private var mSize = 0
    private lateinit var playBtn: ImageView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSize: TextView
    private lateinit var audioMessage: RelativeLayout
    private val TAG: String = CometChatMediaViewActivity::class.java.name
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comet_chat_media_view)
        handleIntent()
        mediaPlayer = MediaPlayer()
        toolbar = findViewById(R.id.toolbar)
        toolbar.getNavigationIcon()!!.setTint(resources.getColor(R.color.textColorWhite))
        toolbar.setTitle(senderName)
        if (sentAt != 0L) toolbar.setSubtitle(
            Utils.getLastMessageDate(
                sentAt
            )
        )
        imageMessage = findViewById(R.id.image_message)
        videoMessage = findViewById(R.id.video_message)
        audioMessage = findViewById(R.id.audio_message)
        mediaSize = findViewById(R.id.media_size_tv)

        playBtn = findViewById(R.id.playBtn)
        when (mediaType) {
            CometChatConstants.MESSAGE_TYPE_IMAGE -> {
                Glide.with(this).asBitmap().load(mediaUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(imageMessage)
                imageMessage.setVisibility(View.VISIBLE)
            }

            CometChatConstants.MESSAGE_TYPE_VIDEO -> {
                val mediacontroller = MediaController(this, true)
                mediacontroller.setAnchorView(videoMessage)
                videoMessage.setMediaController(mediacontroller)
                videoMessage.setVideoURI(Uri.parse(mediaUrl))
                videoMessage.setOnPreparedListener { mediaPlayer ->
                    mediacontroller.show(0)
                    mediaPlayer.start()
                }
                videoMessage.setVisibility(View.VISIBLE)
            }

            CometChatConstants.MESSAGE_TYPE_AUDIO -> {
                mediaPlayer.reset()
                mediaSize.text = Utils.getFileSize(mSize)
                playBtn.setOnClickListener {
                    try {
                        mediaPlayer.setDataSource(mediaUrl)
                        mediaPlayer.prepare()
                        mediaPlayer.setOnCompletionListener { playBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp) }
                    } catch (e: Exception) {
                        Log.e(TAG, "MediaPlayerError: " + e.message)
                    }
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                        playBtn.setImageResource(R.drawable.ic_pause_24dp)
                    } else {
                        mediaPlayer.pause()
                        playBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp)
                    }
                }
                audioMessage.visibility = View.VISIBLE
            }
        }
        setSupportActionBar(toolbar)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleIntent() {
        if (intent.hasExtra(UIKitConstants.IntentStrings.ID)) Id =
            intent.getStringExtra(UIKitConstants.IntentStrings.ID)
        if (intent.hasExtra(UIKitConstants.IntentStrings.TYPE)) type =
            intent.getStringExtra(UIKitConstants.IntentStrings.TYPE)
        if (intent.hasExtra(UIKitConstants.IntentStrings.ALLOW_CAPTION)) allowCaption =
            intent.getBooleanExtra(UIKitConstants.IntentStrings.ALLOW_CAPTION, false)
        if (intent.hasExtra(UIKitConstants.IntentStrings.MEDIA_SIZE)) mSize =
            intent.getIntExtra(UIKitConstants.IntentStrings.MEDIA_SIZE, 0)
        if (intent.hasExtra(UIKitConstants.IntentStrings.NAME)) senderName =
            intent.getStringExtra(UIKitConstants.IntentStrings.NAME)
        if (intent.hasExtra(UIKitConstants.IntentStrings.SENTAT)) sentAt =
            intent.getLongExtra(UIKitConstants.IntentStrings.SENTAT, 0)
        if (intent.hasExtra(UIKitConstants.IntentStrings.INTENT_MEDIA_MESSAGE)) mediaUrl =
            intent.getStringExtra(UIKitConstants.IntentStrings.INTENT_MEDIA_MESSAGE)
        if (intent.hasExtra(UIKitConstants.IntentStrings.MESSAGE_TYPE)) mediaType =
            intent.getStringExtra(UIKitConstants.IntentStrings.MESSAGE_TYPE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.reset()
    }
}