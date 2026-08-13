package com.tjst.multilangplayer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tjst.multilangplayer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contentManager: ContentManager
    private lateinit var player: ExoPlayer

    private var scenes: List<Scene> = emptyList()
    private var currentLang: LanguageSlot = LanguageSlot.KO
    private var currentSceneIndex: Int = 0

    // 이미지 씬 재생 시간 관리용
    private val imageHandler = Handler(Looper.getMainLooper())
    private var imageAdvanceRunnable: Runnable? = null
    private var imageSceneElapsedAtStartMs: Long = 0L
    private var imageSceneStartedAtRealtimeMs: Long = 0L

    // 이미지 한 장당 재생 시간(밀리초). 필요에 맞게 조정.
    private val defaultImageDurationMs = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 키오스크/사이니지 용도 - 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        contentManager = ContentManager(this)
        contentManager.ensureFolders()

        binding.debugPathText.text = "콘텐츠 폴더: ${contentManager.rootDir.absolutePath}"

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    advanceToNextScene()
                }
            }
        })

        setupLanguageButtons()

        scenes = contentManager.scanScenes()
        if (scenes.isEmpty()) {
            Toast.makeText(
                this,
                "재생할 콘텐츠가 없습니다.\n${contentManager.rootDir.absolutePath} 아래 A/B/C/D 폴더에 파일을 넣어주세요.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        playScene(0, 0L)
    }

    private fun setupLanguageButtons() {
        val buttons: Map<Button, LanguageSlot> = mapOf(
            binding.btnKo to LanguageSlot.KO,
            binding.btnEn to LanguageSlot.EN,
            binding.btnJa to LanguageSlot.JA,
            binding.btnZh to LanguageSlot.ZH
        )
        buttons.forEach { (button, slot) ->
            button.setOnClickListener { switchLanguage(slot) }
        }
        highlightCurrentLanguageButton()
    }

    private fun highlightCurrentLanguageButton() {
        val map: Map<LanguageSlot, Button> = mapOf(
            LanguageSlot.KO to binding.btnKo,
            LanguageSlot.EN to binding.btnEn,
            LanguageSlot.JA to binding.btnJa,
            LanguageSlot.ZH to binding.btnZh
        )
        map.forEach { (slot, button) ->
            button.alpha = if (slot == currentLang) 1.0f else 0.55f
        }
    }

    /** 언어 버튼 클릭 시: 현재 재생 위치(초)를 구해서 새 언어의 같은 씬으로 이어서 재생 */
    private fun switchLanguage(newLang: LanguageSlot) {
        if (newLang == currentLang || scenes.isEmpty()) return
        val elapsedMs = getCurrentElapsedMs()
        currentLang = newLang
        highlightCurrentLanguageButton()
        playScene(currentSceneIndex, elapsedMs)
    }

    private fun getCurrentElapsedMs(): Long {
        val scene = scenes.getOrNull(currentSceneIndex) ?: return 0L
        val file = scene.filesBySlot[currentLang] ?: return 0L
        return when (contentManager.mediaTypeOf(file)) {
            MediaType.VIDEO -> player.currentPosition.coerceAtLeast(0L)
            MediaType.IMAGE -> imageSceneElapsedAtStartMs +
                (SystemClock.elapsedRealtime() - imageSceneStartedAtRealtimeMs)
            else -> 0L
        }
    }

    /** index 번째 씬을, seekMs 지점부터 현재 언어로 재생 */
    private fun playScene(index: Int, seekMs: Long) {
        cancelImageAdvance()
        if (scenes.isEmpty()) return

        val safeIndex = index % scenes.size
        currentSceneIndex = safeIndex
        val scene = scenes[safeIndex]

        val file = scene.filesBySlot[currentLang]
        if (file == null) {
            // 현재 언어에 해당하는 파일이 없으면 다음 씬으로 넘어간다.
            advanceToNextScene()
            return
        }

        when (contentManager.mediaTypeOf(file)) {
            MediaType.VIDEO -> showVideo(file, seekMs)
            MediaType.IMAGE -> showImage(file, seekMs)
            else -> advanceToNextScene()
        }
    }

    private fun showVideo(file: File, seekMs: Long) {
        binding.imageView.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE

        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.seekTo(seekMs.coerceAtLeast(0L))
        player.playWhenReady = true
    }

    private fun showImage(file: File, seekMs: Long) {
        player.playWhenReady = false
        binding.playerView.visibility = View.GONE
        binding.imageView.visibility = View.VISIBLE
        binding.imageView.setImageURI(Uri.fromFile(file))

        val duration = defaultImageDurationMs
        val startElapsed = seekMs.coerceIn(0L, duration)
        val remaining = (duration - startElapsed).coerceAtLeast(200L)

        imageSceneElapsedAtStartMs = startElapsed
        imageSceneStartedAtRealtimeMs = SystemClock.elapsedRealtime()

        val runnable = Runnable { advanceToNextScene() }
        imageAdvanceRunnable = runnable
        imageHandler.postDelayed(runnable, remaining)
    }

    private fun cancelImageAdvance() {
        imageAdvanceRunnable?.let { imageHandler.removeCallbacks(it) }
        imageAdvanceRunnable = null
    }

    private fun advanceToNextScene() {
        if (scenes.isEmpty()) return
        val nextIndex = (currentSceneIndex + 1) % scenes.size
        playScene(nextIndex, 0L)
    }

    override fun onPause() {
        super.onPause()
        player.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        val scene = scenes.getOrNull(currentSceneIndex)
        val file = scene?.filesBySlot?.get(currentLang)
        if (file != null && contentManager.mediaTypeOf(file) == MediaType.VIDEO) {
            player.playWhenReady = true
        }
    }

    override fun onDestroy() {
        cancelImageAdvance()
        player.release()
        super.onDestroy()
    }
}
