package com.tjst.multilangplayer

import android.content.Context
import java.io.File

/** 씬(콘텐츠 조각) 파일의 종류 */
enum class MediaType { VIDEO, IMAGE }

/**
 * 하나의 "씬"(예: a, b, c ...). 각 언어(LanguageSlot)마다 대응하는 파일을 가질 수 있다.
 * 언어별 파일이 없는 경우도 있을 수 있으므로 Map 으로 보관한다.
 */
data class Scene(
    val id: String,
    val filesBySlot: Map<LanguageSlot, File>
)

/**
 * 콘텐츠 폴더(A/B/C/D)를 스캔해서 재생목록(Scene 리스트)을 만드는 클래스.
 *
 * 콘텐츠 루트 폴더 위치:
 *   context.getExternalFilesDir(null)  (예: /storage/emulated/0/Android/data/com.tjst.multilangplayer/files)
 *   앱 전용 저장소이므로 별도의 저장공간 권한(WRITE_EXTERNAL_STORAGE 등)이 필요 없다.
 *
 * 파일명 규칙: "<씬글자><언어숫자>.<확장자>"
 *   예: a1.mp4, b1.jpg, a2.mp4, b2.jpg ...
 *   씬글자(a, b, c ...)가 같으면 같은 콘텐츠의 언어별 버전으로 간주한다.
 */
class ContentManager(context: Context) {

    val rootDir: File = context.getExternalFilesDir(null) ?: context.filesDir

    private val videoExtensions = setOf("mp4", "mkv", "mov", "webm", "3gp", "m4v")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    // 파일명 패턴: 영문자(씬 id) + 숫자(언어 접미사) + 확장자
    private val sceneFilePattern = Regex("^([a-zA-Z]+)(\\d+)\\.([a-zA-Z0-9]+)$")

    /** A/B/C/D 폴더가 없으면 미리 만들어 둔다. (파일을 넣을 위치를 바로 알 수 있도록) */
    fun ensureFolders() {
        LanguageSlot.entries.forEach { slot ->
            File(rootDir, slot.folderName).mkdirs()
        }
    }

    fun mediaTypeOf(file: File): MediaType? {
        val ext = file.extension.lowercase()
        return when {
            ext in videoExtensions -> MediaType.VIDEO
            ext in imageExtensions -> MediaType.IMAGE
            else -> null
        }
    }

    /**
     * A/B/C/D 폴더를 모두 스캔하여 씬 id(글자) 기준으로 정렬된 재생목록을 만든다.
     * 언어별로 파일이 없는 씬은 해당 언어 항목이 비어 있는 채로 포함된다.
     */
    fun scanScenes(): List<Scene> {
        val bySceneId = sortedMapOf<String, MutableMap<LanguageSlot, File>>()

        LanguageSlot.entries.forEach { slot ->
            val folder = File(rootDir, slot.folderName)
            val files = folder.listFiles() ?: return@forEach
            files.forEach { file ->
                val match = sceneFilePattern.matchEntire(file.name) ?: return@forEach
                val sceneId = match.groupValues[1].lowercase()
                if (mediaTypeOf(file) != null) {
                    bySceneId.getOrPut(sceneId) { mutableMapOf() }[slot] = file
                }
            }
        }

        return bySceneId.map { (id, map) -> Scene(id, map) }
    }
}
