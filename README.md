# INSTA PROMPT 🔴⚫

One-tap **ten-layer prompt engine** for Android, built from custom layer files.
Red / black / white, rounded everything, living ember glow, spark bursts,
haptics and a soft pop sound.

## The one button
**COPY PROMPT** (center) instantly picks one random line from each of the 10
layer files in `app/src/main/assets/` (500 unique lines total):

BACKGROUND · CAMERA ANGLE · FACE EXPRESSION · HAIRSTYLE · HALF BODY POSE ·
LIGHTING · MOOD · OUTFIT · PARIS LOCATION · SETTING

…and copies them joined with `, ` followed by the fixed ending:

> An extremely unremarkable, casual iPhone snapshot of [SUBJECT/SCENE]. Careless
> framing, accidental composition, mild overexposure, and natural uneven indoor
> lighting. Looks like an unedited camera roll photo.

Nothing is appended after that ending. Combination space: 97,656,250,000,000,000.

## Editing your layers
Edit any txt file in `app/src/main/assets/` and push — the app preloads every
line at launch, so picks stay instant at any line count.

## Build
Every push to `main` builds on a pinned **ubuntu-24.04** runner with manually
installed, version-pinned Android SDK + Gradle. The release APK is signed with
the committed `insta.p12` key (alias `insta`, password `instaprompt`) — no
GitHub Secrets required — verified with `apksigner verify`, and uploaded as
artifact **INSTA-PROMPT-signed-apk**.

## Install
Download the artifact → transfer to your phone → allow "install unknown apps"
→ install. Fully offline. minSdk 24.
