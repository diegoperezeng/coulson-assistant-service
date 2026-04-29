package com.yellowleaf.coulson.domain.port;

public interface TextToSpeechPort {

    byte[] generateSpeech(String voiceId, String text);
}
