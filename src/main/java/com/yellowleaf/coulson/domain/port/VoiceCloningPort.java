package com.yellowleaf.coulson.domain.port;

public interface VoiceCloningPort {

    String createVoiceClone(String name, byte[] audioBytes, String audioFileName);
}
