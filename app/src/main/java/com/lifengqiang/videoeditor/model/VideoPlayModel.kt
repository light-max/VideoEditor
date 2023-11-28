package com.lifengqiang.videoeditor.model

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface
import java.lang.ref.WeakReference
import kotlin.concurrent.Volatile

interface IVideoPlayModel {
    interface Callback {
        fun onPrepared(durationSecond: Int, videoWidth: Int, videoHeight: Int, rotation: Int)
        fun onProgress(progressSecond: Int)
        fun onPause()
        fun onResume()
    }

    fun setCallback(view: Callback)
    fun prepare(surface: Surface): Boolean
    fun isPrepared(): Boolean
    fun resume()
    fun pause()
    fun toggle()
    fun seekTo(progressSecond: Int)
    fun release()
}

class VideoPlayModel(val path: String) : IVideoPlayModel {
    private var audio: AudioThread? = null
    private var video: VideoThread? = null
    private var _view: WeakReference<IVideoPlayModel.Callback>? = null

    private val audioCallback = object : AudioThread.Callback {
        override fun onPause() {
        }

        override fun onResume() {
        }

        override fun onFinish() {
        }
    }

    private val videoCallback = object : VideoThread.Callback {
        override fun onPrepared(durationSecond: Int, width: Int, height: Int, rotation: Int) {
            view()?.onPrepared(durationSecond, width, height, rotation)
        }

        override fun onProgress(progressSecond: Int) {
            view()?.onProgress(progressSecond)
        }

        override fun onPause() {
            view()?.onPause()
        }

        override fun onResume() {
            view()?.onResume()
        }

        override fun onFinish() {
        }
    }

    override fun setCallback(view: IVideoPlayModel.Callback) {
        _view = WeakReference(view)
    }

    private fun view(): IVideoPlayModel.Callback? = _view?.get()

    override fun prepare(surface: Surface): Boolean {
        if (audio == null && video == null) {
            var sync: Sync? = Sync()
            audio = newAudioThread(path, sync!!, audioCallback)
            audio?.start()
            if (audio == null) sync = null
            video = newVideoThread(path, sync, surface, videoCallback)
            video?.start()
        }
        return isPrepared()
    }

    override fun isPrepared(): Boolean {
        return video != null
    }

    override fun resume() {
        audio?.play()
        video?.play()
    }

    override fun pause() {
        audio?.pause()
        video?.pause()
    }

    override fun toggle() {
        audio?.toggle()
        video?.toggle()
    }

    override fun seekTo(progressSecond: Int) {
        audio?.seekTo(progressSecond)
        video?.seekTo(progressSecond)
    }

    override fun release() {
        audio?.interrupt()
        audio = null
        video?.interrupt()
        video = null
    }
}

private class Sync {
    @Volatile
    var playTime: Long = 0L

    fun diff(playTime: Long): Long {
        return playTime - this.playTime
    }
}

private class AudioThread(
    val codec: MediaCodec,
    val me: MediaExtractor,
    val sync: Sync,
    val track: AudioTrack,
    val callback: Callback
) : Thread() {
    interface Callback {
        fun onPause()
        fun onResume()
        fun onFinish()
    }

    private val threadLock = java.lang.Object()
    private var isPause = false
    private var seekToProgressSecond: Int = -1

    override fun run() {
        var info = MediaCodec.BufferInfo()
        var isStreamEnd = false
        codec.start()
        track.play()
        callback.onResume()
        while (!isInterrupted) {
            val isP = synchronized(threadLock) {
                if (isPause) {
                    callback.onPause()
                    try {
                        threadLock.wait()
                    } catch (e: Exception) {
                    }
                    callback.onResume()
                    true
                } else {
                    false
                }
            }
            if (isP) continue

            if (seekToProgressSecond >= 0) {
                me.seekTo(seekToProgressSecond * 1000000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                info = MediaCodec.BufferInfo()
                codec.flush()
                isStreamEnd = false
                seekToProgressSecond = -1
                continue
            }

            val input = codec.dequeueInputBuffer(20000)
            if (input >= 0) {
                if (isStreamEnd) {
                    codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    val buffer = codec.getInputBuffer(input)!!
                    val sampleSize = me.readSampleData(buffer, 0)
                    codec.queueInputBuffer(input, 0, sampleSize, me.sampleTime, 0)
                }
                isStreamEnd = !me.advance()
            }
            val output = codec.dequeueOutputBuffer(info, 20000)
            if (output >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    me.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    info = MediaCodec.BufferInfo()
                    codec.flush()
                    isStreamEnd = false
                    callback.onFinish()
                } else {
                    sync.playTime = info.presentationTimeUs / 1000
                    val buffer = codec.getOutputBuffer(output)!!
                    track.write(buffer, info.size, AudioTrack.WRITE_BLOCKING)
                    codec.releaseOutputBuffer(output, false)
                }
            }
        }
        codec.stop()
        codec.release()
        me.release()
    }

    fun pause() = synchronized(threadLock) {
        isPause = true
    }

    fun play() = synchronized(threadLock) {
        isPause = false
        threadLock.notify()
    }

    fun toggle() = synchronized(threadLock) {
        isPause = !isPause
        threadLock.notify()
    }

    fun seekTo(progressSecond: Int) {
        seekToProgressSecond = progressSecond
        synchronized(threadLock) {
            threadLock.notify()
        }
    }
}

private class VideoThread(
    val codec: MediaCodec,
    val me: MediaExtractor,
    val externalSync: Sync?,
    val callback: Callback
) : Thread() {
    interface Callback {
        fun onPrepared(durationSecond: Int, width: Int, height: Int, rotation: Int)
        fun onProgress(progressSecond: Int)
        fun onPause()
        fun onResume()
        fun onFinish()
    }

    private val threadLock = java.lang.Object()
    private var isPause = false
    private var seekToProgressSecond: Int = -1
    private var progressSecond: Int = 0

    private var isStreamEnd = false
    private var startTime: Long = 0
    private var info = MediaCodec.BufferInfo()

    override fun run() {
        codec.start()
        callback.onResume()
        startTime = System.currentTimeMillis()
        while (!isInterrupted) {
            val isP = synchronized(threadLock) {
                if (isPause) {
                    callback.onResume()
                    val time = System.currentTimeMillis()
                    try {
                        threadLock.wait()
                    } catch (e: Exception) {
                    }
                    startTime += System.currentTimeMillis() - time
                    callback.onPause()
                    true
                } else {
                    false
                }
            }
            if (isP) continue

            if (seekToProgressSecond >= 0) {
                resumeDecodeState(seekToProgressSecond)
                while (!isStreamEnd) {
                    queueData()
                    val output = codec.dequeueOutputBuffer(info, 20000)
                    if (output >= 0) {
                        if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            resumeDecodeState(0)
                            break
                        } else {
                            if (info.presentationTimeUs >= seekToProgressSecond * 1000000) {
                                val pstm = info.presentationTimeUs / 1000
                                startTime = System.currentTimeMillis() - pstm
                                progressSecond = (pstm / 1000).toInt()
                                callback.onProgress(progressSecond)
                                codec.releaseOutputBuffer(output, true)
                                break
                            } else {
                                codec.releaseOutputBuffer(output, false)
                            }
                        }
                    }
                }
                seekToProgressSecond = -1
                continue
            }

            queueData()
            val output = codec.dequeueOutputBuffer(info, 20000)
            if (output >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    resumeDecodeState(0)
                    callback.onFinish()
                } else {
                    codec.releaseOutputBuffer(output, true)
                    val ps = (info.presentationTimeUs / 1000000).toInt()
                    if (progressSecond != ps) {
                        progressSecond = ps
                        callback.onProgress(progressSecond)
                    }

                    val clock = System.currentTimeMillis() - startTime
                    val diff = info.presentationTimeUs / 1000 - clock
                    if (diff > 0) {
                        SystemClock.sleep(diff)
                    }

                    externalSync?.apply {
                        val externalDiff = diff(info.presentationTimeUs / 1000)
                        if (externalDiff > 200) {
                            startTime += 20
                        } else if (externalDiff < -200) {
                            startTime -= 20
                        }
                    }
                }
            }
        }
        codec.stop()
        codec.release()
        me.release()
    }

    private fun queueData() {
        val input = codec.dequeueInputBuffer(20000)
        if (input >= 0) {
            if (isStreamEnd) {
                codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                val buffer = codec.getInputBuffer(input)!!
                val sampleSize = me.readSampleData(buffer, 0)
                codec.queueInputBuffer(input, 0, sampleSize, me.sampleTime, 0)
            }
            isStreamEnd = !me.advance()
        }
    }

    private fun resumeDecodeState(progressSecond: Int) {
        startTime = System.currentTimeMillis() - progressSecond * 1000
        me.seekTo(progressSecond * 1000000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        info = MediaCodec.BufferInfo()
        codec.flush()
        isStreamEnd = false
    }

    fun pause() = synchronized(threadLock) {
        isPause = true
    }

    fun play() = synchronized(threadLock) {
        isPause = false
        threadLock.notify()
    }

    fun toggle() = synchronized(threadLock) {
        isPause = !isPause
        threadLock.notify()
    }

    fun seekTo(progressSecond: Int) {
        seekToProgressSecond = progressSecond
        synchronized(threadLock) {
            threadLock.notify()
        }
    }
}

private fun newAudioThread(
    path: String,
    sync: Sync,
    callback: AudioThread.Callback
): AudioThread? {
    var index = -1
    var format: MediaFormat? = null
    var mime: String? = null
    var me: MediaExtractor? = null
    var codec: MediaCodec? = null
    var at: AudioTrack? = null
    try {
        me = MediaExtractor().apply {
            setDataSource(path)
            for (i in 0 until trackCount) {
                val f = getTrackFormat(i)
                mime = f.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("audio")) {
                    index = i
                    format = f
                    break
                }
            }
            if (index == -1) throw RuntimeException("$path 找不到音频轨道")
            selectTrack(index)
        }
        codec = MediaCodec.createDecoderByType(mime!!).apply {
            configure(format, null, null, 0)
        }
        at = createAudioTrack(format!!)
        return AudioThread(codec, me, sync, at, callback)
    } catch (e: Exception) {
        e.printStackTrace()
        me?.release()
        codec?.stop()
        codec?.release()
        at?.stop()
        at?.release()
        return null
    }
}

private fun newVideoThread(
    path: String,
    sync: Sync?,
    surface: Surface,
    callback: VideoThread.Callback
): VideoThread? {
    var index = -1
    var format: MediaFormat? = null
    var mime: String? = null
    var me: MediaExtractor? = null
    var codec: MediaCodec? = null
    try {
        var durationSecond = 0
        var videoWidth = 0
        var videoHeight = 0
        var videoRotation = 0
        me = MediaExtractor().apply {
            setDataSource(path)
            for (i in 0 until trackCount) {
                val f = getTrackFormat(i)
                mime = f.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("video")) {
                    index = i
                    format = f
                    break
                }
            }
            if (index == -1) throw RuntimeException("$path 找不到视频轨道")
            selectTrack(index)
            format?.apply {
                durationSecond = (getLong(MediaFormat.KEY_DURATION) / 1000000L).toInt()
                videoWidth = getInteger(MediaFormat.KEY_WIDTH)
                videoHeight = getInteger(MediaFormat.KEY_HEIGHT)
                videoRotation = getInteger(MediaFormat.KEY_ROTATION)
            }
        }
        codec = MediaCodec.createDecoderByType(mime!!).apply {
            configure(format, surface, null, 0)
        }
        callback.onPrepared(durationSecond, videoWidth, videoHeight, videoRotation)
        return VideoThread(codec, me, sync, callback)
    } catch (e: Exception) {
        e.printStackTrace()
        me?.release()
        codec?.stop()
        codec?.release()
        return null
    }
}

private fun createAudioTrack(format: MediaFormat): AudioTrack {
    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    return AudioTrack.Builder()
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
        ).build()
}