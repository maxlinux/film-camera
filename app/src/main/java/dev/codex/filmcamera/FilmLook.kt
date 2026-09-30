package dev.codex.filmcamera

import androidx.compose.ui.graphics.Color

/** Original, analog-inspired looks; none emulate an official camera-maker profile. */
enum class FilmLook(
    val label: String,
    val subtitle: String,
    val wash: Color,
    val washAlpha: Float,
    val matrix: FloatArray,
) {
    ORIGINAL("Original", "Clean digital", Color.Transparent, 0f, floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )),
    VIVID_SLIDE("Vivid Slide", "Punchy color", Color(0xFFDE482E), .10f, floatArrayOf(
        1.20f, -.05f, -.05f, 0f, 5f,
        -.04f, 1.13f, -.04f, 0f, 3f,
        -.06f, -.05f, 1.16f, 0f, 7f,
        0f, 0f, 0f, 1f, 0f,
    )),
    DOCUMENTARY("Documentary", "Muted and grounded", Color(0xFF43554B), .16f, floatArrayOf(
        .72f, .14f, .14f, 0f, 14f,
        .14f, .72f, .14f, 0f, 14f,
        .14f, .14f, .72f, 0f, 14f,
        0f, 0f, 0f, 1f, 0f,
    )),
    PASTEL_NEGATIVE("Pastel Negative", "Soft candy light", Color(0xFFFFC5A5), .20f, floatArrayOf(
        .88f, .08f, .04f, 0f, 24f,
        .05f, .88f, .06f, 0f, 20f,
        .06f, .08f, .86f, 0f, 22f,
        0f, 0f, 0f, 1f, 0f,
    )),
    WARM_GRAIN("Warm Grain", "Amber and textured", Color(0xFFC77A35), .18f, floatArrayOf(
        1.05f, .04f, .02f, 0f, 14f,
        .03f, .96f, .03f, 0f, 5f,
        .01f, .04f, .91f, 0f, -2f,
        0f, 0f, 0f, 1f, 0f,
    )),
    MONO("Silver Mono", "Classic black and white", Color(0xFF19202A), .06f, floatArrayOf(
        .30f, .59f, .11f, 0f, 5f,
        .30f, .59f, .11f, 0f, 5f,
        .30f, .59f, .11f, 0f, 5f,
        0f, 0f, 0f, 1f, 0f,
    ));
}
