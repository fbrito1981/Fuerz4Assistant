package com.fuerz4.assistant.data.voice.wakeword

/**
 * Vosk limited-grammar JSON: constraining the recognizer to just the wake phrase (plus the
 * required `[unk]` catch-all bucket for everything else) keeps CPU/battery use low and accuracy
 * high compared to a general-vocabulary model — see CLAUDE.md.
 */
object WakeWordGrammar {
    const val PHRASE = "che fuerza"
    const val GRAMMAR_JSON = "[\"$PHRASE\", \"[unk]\"]"
}
