package com.lifengqiang.videoeditor.model

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.lang.ref.WeakReference

interface IAudioPlayModel {
    interface Callback {
        fun onPrepared(durationSecond: Int)
        fun onProgress(durationSecond: Int, progressSecond: Int)
        fun onPause()
        fun onResume()
    }

    fun setCallback(view: Callback)
    fun prepare(): Boolean
    fun isPrepared(): Boolean
    fun resume()
    fun pause()
    fun toggle()
    fun seekTo(progressSecond: Int)
    fun release()
}

class AudioPlayModel(val path: String) : Runnable, IAudioPlayModel {
    private var callback: WeakReference<IAudioPlayModel.Callback>? = null
    private var thread: Thread? = null

    private var isPrepared = false
    private var isPause = true
    private val lock = Object()

    private var at: AudioTrack? = null
    private var me: MediaExtractor? = null
    private var codec: MediaCodec? = null

    private var durationSecond: Int = 0
    private var progressSecond: Int = 0

    @Volatile
    private var seekToProgressSecond: Int = -1

    override fun setCallback(callback: IAudioPlayModel.Callback) {
        this.callback = WeakReference(callback)
    }

    override fun prepare(): Boolean {
        try {
            this.me = MediaExtractor()
            val me = this.me!!
            me.setDataSource(path)
            var format: MediaFormat? = null
            var index = -1
            for (i in 0..me.trackCount) {
                val f = me.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio", true)) {
                    format = f
                    index = i
                    break
                }
            }
            if (index == -1) throw RuntimeException("找不到音频信息 $path")
            initAudioTrack(format!!)
            me.selectTrack(index)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec?.configure(format, null, null, 0)
            codec?.start()
            thread = Thread(this)
            thread?.start()
            durationSecond = (format.getLong(MediaFormat.KEY_DURATION) / 1000000).toInt()
            callback?.get()?.onPrepared(durationSecond)
            isPrepared = true
        } catch (e: Exception) {
            e.printStackTrace()
            isPrepared = false
        }
        return isPrepared
    }

    override fun isPrepared() = isPrepared

    override fun resume() {
        synchronized(lock) {
            isPause = false
            lock.notify()
        }
    }

    override fun pause() {
        synchronized(lock) {
            isPause = true
        }
    }

    override fun toggle() {
        synchronized(lock) {
            isPause = !isPause
            lock.notify()
        }
    }

    override fun seekTo(progressSecond: Int) {
        seekToProgressSecond = progressSecond
    }

    override fun release() {
        thread?.interrupt()
        try {
            thread?.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun run() {
        val codec = this.codec!!
        val me = this.me!!
        var info = MediaCodec.BufferInfo()
        callback?.get()?.onResume()
        while (!Thread.currentThread().isInterrupted) {
            synchronized(lock) {
                if (isPause) {
                    callback?.get()?.onPause()
                    try {
                        lock.wait()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    callback?.get()?.onResume()
                }
            }
            if (seekToProgressSecond >= 0) {
                info = MediaCodec.BufferInfo()
                codec.flush()
                me.seekTo(seekToProgressSecond * 1_000_000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                seekToProgressSecond = -1
            }
            val input = codec.dequeueInputBuffer(2000)
            if (input >= 0) {
                val buffer = codec.getInputBuffer(input)!!
                val sampleSize = me.readSampleData(buffer, 0)
                if (sampleSize > 0 && me.advance()) {
                    codec.queueInputBuffer(input, 0, sampleSize, me.sampleTime, 0)
                } else {
                    codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }
            val output = codec.dequeueOutputBuffer(info, 2000)
            if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                me.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                codec.flush()
                info = MediaCodec.BufferInfo()
                progressSecond = 0
            } else {
                if (output >= 0) {
                    val buffer = codec.getOutputBuffer(output)!!
                    val progress = (info.presentationTimeUs / 1000000).toInt()
                    if (progress != progressSecond) {
                        progressSecond = progress
                        callback?.get()?.onProgress(durationSecond, progressSecond)
                    }
                    at?.write(buffer, info.size, AudioTrack.WRITE_BLOCKING)
                    codec.releaseOutputBuffer(output, false)
                }
            }
        }
        me.release()
        codec.stop()
        codec.release()
        at?.stop()
        at?.release()
    }

    private fun initAudioTrack(format: MediaFormat) {
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        at = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setChannelMask(if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    if (channelCount >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
            .build()
        at?.play()
    }
}