package com.prometheus.offlineai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var chatLog: TextView
    private lateinit var input: EditText
    private lateinit var loadButton: Button
    private lateinit var sendButton: Button

    private var model: LlamaModel? = null
    private val REQUEST_PICK_MODEL = 1001
    private val BUILTIN_MODEL_NAME = "model.gguf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        chatLog = findViewById(R.id.chatLog)
        input = findViewById(R.id.input)
        loadButton = findViewById(R.id.loadModel)
        sendButton = findViewById(R.id.send)

        loadButton.setOnClickListener { pickModel() }
        sendButton.setOnClickListener { sendPrompt() }

        // Auto-load built-in model on startup
        lifecycleScope.launch {
            ensureBuiltinModelLoaded()
        }
    }

    private suspend fun ensureBuiltinModelLoaded() {
        status.text = "Status: Preparing built-in model…"
        try {
            val modelFile = File(getExternalFilesDir("models"), BUILTIN_MODEL_NAME)
            if (!modelFile.exists()) {
                withContext(Dispatchers.IO) {
                    assets.open(BUILTIN_MODEL_NAME).use { input ->
                        modelFile.parentFile?.mkdirs()
                        FileOutputStream(modelFile).use { output -> input.copyTo(output) }
                    }
                }
            }
            val loaded = withContext(Dispatchers.IO) {
                model?.let { Llama.releaseModel(it) }
                Llama.loadModel(
                    modelFile.absolutePath,
                    LlamaConfig(contextSize = 2048, threads = 4)
                )
            }
            model = loaded
            status.text = "Status: Built-in model ready (offline)"
            sendButton.isEnabled = true
            appendChat("System: Built-in model loaded. All inference runs 100% offline.")
        } catch (e: Exception) {
            status.text = "Status: Built-in model failed. Use Load Model."
            appendChat("Note: Built-in model not available. Tap Load Model to select a GGUF file.")
        }
    }

    private fun pickModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_PICK_MODEL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_MODEL && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri -> copyAndLoadModel(uri) }
        }
    }

    private fun copyAndLoadModel(uri: Uri) {
        status.text = "Status: Loading selected model…"
        lifecycleScope.launch {
            try {
                val dest = File(getExternalFilesDir("models"), "custom.gguf")
                dest.parentFile?.mkdirs()
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                val loaded = withContext(Dispatchers.IO) {
                    model?.let { Llama.releaseModel(it) }
                    Llama.loadModel(
                        dest.absolutePath,
                        LlamaConfig(contextSize = 2048, threads = 4)
                    )
                }
                model = loaded
                status.text = "Status: Custom model loaded (offline)"
                sendButton.isEnabled = true
                appendChat("System: Custom model ready.")
            } catch (e: Exception) {
                status.text = "Status: Failed to load model"
                Toast.makeText(this@MainActivity, e.message ?: "Unknown error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendPrompt() {
        val prompt = input.text.toString().trim()
        val currentModel = model
        if (prompt.isEmpty() || currentModel == null) return

        appendChat("You: $prompt")
        input.text.clear()
        sendButton.isEnabled = false
        status.text = "Status: Generating…"

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Llama.complete(
                        currentModel,
                        prompt = prompt,
                        systemPrompt = "You are Prometheus, a helpful offline assistant. Give clear, practical answers. Prefer concise step-by-step guidance for survival, DIY, and home projects.",
                        maxTokens = 512
                    )
                }
                appendChat("Prometheus: ${result.text.trim()}\n(${"%.1f".format(result.tokensPerSecond)} tok/s)")
            } catch (e: Exception) {
                appendChat("Error: ${e.message}")
            } finally {
                status.text = "Status: Model ready (offline)"
                sendButton.isEnabled = true
            }
        }
    }

    private fun appendChat(line: String) {
        chatLog.append("\n\n$line")
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.let { Llama.releaseModel(it) }
    }
}
