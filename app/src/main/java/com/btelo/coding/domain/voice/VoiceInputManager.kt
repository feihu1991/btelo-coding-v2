package com.btelo.coding.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音输入状态
 */
sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Listening : VoiceInputState()
    object Processing : VoiceInputState()
    data class Result(val text: String) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

/**
 * 语音输入管理器
 * 使用 Android SpeechRecognizer API 实现语音转文字
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()
    
    private var isListening = false
    
    /**
     * 检查设备是否支持语音识别
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * 初始化语音识别器
     */
    private fun initSpeechRecognizer() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }
    }
    
    /**
     * 开始语音识别
     */
    fun startListening() {
        if (isListening) return
        
        if (!isSpeechRecognitionAvailable()) {
            _state.value = VoiceInputState.Error("设备不支持语音识别")
            return
        }
        
        initSpeechRecognizer()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            _state.value = VoiceInputState.Listening
        } catch (e: Exception) {
            _state.value = VoiceInputState.Error("启动语音识别失败: ${e.message}")
            isListening = false
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (!isListening) return
        
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // 忽略停止时的异常
        }
        isListening = false
    }
    
    /**
     * 销毁语音识别器
     */
    fun destroy() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // 忽略销毁时的异常
        }
        speechRecognizer = null
        _state.value = VoiceInputState.Idle
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        _state.value = VoiceInputState.Idle
    }
    
    /**
     * 创建语音识别监听器
     */
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceInputState.Listening
        }
        
        override fun onBeginningOfSpeech() {}
        
        override fun onRmsChanged(rmsdB: Float) {}
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            _state.value = VoiceInputState.Processing
            isListening = false
        }
        
        override fun onError(error: Int) {
            isListening = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "无语音输入"
                else -> "未知错误"
            }
            _state.value = VoiceInputState.Error(errorMessage)
        }
        
        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                _state.value = VoiceInputState.Result(bestMatch)
            } else {
                _state.value = VoiceInputState.Error("未识别到语音内容")
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {}
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
