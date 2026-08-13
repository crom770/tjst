package com.tjst.multilangplayer

/**
 * 4개 언어 <-> 4개 폴더 <-> 파일명 숫자 접미사 매핑.
 *
 * folderName : 콘텐츠 루트 아래에 만들어지는 폴더 이름 (A/B/C/D)
 * digitSuffix: 파일명 끝에 붙는 숫자 (예: a1.mp4 -> 한국어, a2.mp4 -> English)
 * displayName: 언어 선택 버튼에 표시될 이름
 *
 * 예) 같은 콘텐츠 "a" 라면
 *   A/a1.mp4 (한국어), B/a2.mp4 (English), C/a3.mp4 (日本語), D/a4.mp4 (中文)
 */
enum class LanguageSlot(
    val folderName: String,
    val digitSuffix: String,
    val displayName: String
) {
    KO("A", "1", "한국어"),
    EN("B", "2", "English"),
    JA("C", "3", "日本語"),
    ZH("D", "4", "中文")
}
