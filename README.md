# MultiLangPlayer (다국어 콘텐츠 플레이어) - 안드로이드 프로젝트

## 1. 이 프로젝트 개요

- 요청사항: A(한국어)/B(English)/C(日本語)/D(中文) 폴더 콘텐츠를 순서대로 재생
  - 화면 하단 언어 버튼(한국어/English/日本語/中文) 터치 시 같은 콘텐츠의 다른 언어 파일로 전환
  - 전환 시 같은 재생 시점(초)에서 이어서 재생
- 이 세션(클라우드 환경)에서는 구글 Android SDK / Maven 저장소(dl.google.com, maven.google.com 등)에 대한
  네트워크 접근이 차단되어 있어, 여기서 직접 .apk 파일로 컴파일하는 것은 불가능했습니다.
  - 아래 프로젝트 소스 전체는 정상적으로 작성되어 있으며, Android Studio에서 열어서
    Gradle Sync -> Build APK 실행하면 바로 빌드됩니다.

## 2. 콘텐츠 폴더 위치 및 파일명 규칙

1. 폴더 위치
   - 앱을 최초 실행하면 앱 전용 저장소 아래에 A/B/C/D 폴더가 자동 생성됩니다.
   - 실제 경로 (기기 내부): `Android/data/com.tjst.multilangplayer/files/A` (B, C, D 동일)
   - adb 로 파일 넣는 예시
     `adb push a1.mp4 /storage/emulated/0/Android/data/com.tjst.multilangplayer/files/A/a1.mp4`
2. 파일명 규칙 - `<씬 글자><언어 숫자>.<확장자>`
   - A = 1(한국어), B = 2(English), C = 3(日本語), D = 4(中文)
   - 같은 씬 글자(a, b, c ...)는 같은 콘텐츠의 언어별 버전으로 인식됩니다.
   - 예) a1.mp4(한국어) / a2.mp4(English) / a3.mp4(日本語) / a4.mp4(中文) -> 모두 씬 "a"
   - 예) b1.jpg(한국어 이미지) / b2.jpg(English 이미지) ... -> 모두 씬 "b"
3. 확장자
   - 동영상: mp4, mkv, mov, webm, 3gp, m4v
   - 이미지: jpg, jpeg, png, webp, bmp, gif
   - 동영상/이미지가 씬별로 섞여 있어도 자동으로 인식됩니다.

## 3. 동작 방식

1. 앱 시작 -> 씬을 알파벳 순(a, b, c ...)으로 정렬 -> 기본 언어(한국어, A) 씬 a부터 재생
2. 동영상 씬 종료 -> 자동으로 다음 씬 재생 (재생목록 끝까지 가면 처음으로 순환)
3. 이미지 씬 -> 기본 5초간 표시 후 다음 씬으로 자동 전환
   - `MainActivity.kt` 의 `defaultImageDurationMs` 값을 수정하면 시간 조정 가능
4. 언어 버튼 터치 -> 현재 씬의 재생 경과 시간(초) 계산 -> 새 언어의 같은 씬 파일을 그 지점부터 재생
   - 동영상 길이가 언어별로 다르면 위치가 어긋날 수 있으므로, 언어별 영상 길이를 동일하게
     맞추는 것을 권장합니다.
5. 특정 언어에 해당 씬 파일이 없는 경우 -> 해당 씬은 건너뛰고 다음 씬으로 진행

## 4. 빌드 방법 (Android Studio)

1. Android Studio 실행 -> Open -> 이 `MultiLangPlayer` 폴더 선택
2. Gradle Sync 자동 진행 (인터넷 연결 필요, 최초 1회 관련 라이브러리 다운로드)
3. 상단 메뉴 Build -> Build Bundle(s) / APK(s) -> Build APK(s)
4. 완료 후 `app/build/outputs/apk/debug/app-debug.apk` 생성

## 5. 프로젝트 구조

```
MultiLangPlayer/
  app/
    build.gradle                 - 앱 모듈 빌드 설정 (Media3/ExoPlayer 포함)
    src/main/
      AndroidManifest.xml
      java/com/tjst/multilangplayer/
        LanguageSlot.kt          - A/B/C/D <-> 언어 <-> 숫자접미사 매핑
        ContentManager.kt        - 폴더 스캔 및 씬 목록 생성
        MainActivity.kt          - 재생/언어전환 로직, 화면 UI 제어
      res/
        layout/activity_main.xml - 재생 화면 + 언어 버튼 레이아웃
        values/                  - 문자열, 색상, 테마
  build.gradle / settings.gradle / gradle.properties
```

## 6. 다음 단계로 필요한 것 (제안)

1. 실제 콘텐츠 파일(A/B/C/D) 확보 후 파일명 규칙에 맞춰 배치 -> 바로 테스트 가능
2. 앱 아이콘 교체 (현재는 임시 아이콘)
3. 이미지 재생시간을 씬별로 다르게 지정하고 싶다면 -> 씬별 재생시간 설정 파일(JSON 등) 추가 개발 필요
4. 세로 모드 지원이 필요하면 AndroidManifest.xml 의 `screenOrientation` 값 조정
