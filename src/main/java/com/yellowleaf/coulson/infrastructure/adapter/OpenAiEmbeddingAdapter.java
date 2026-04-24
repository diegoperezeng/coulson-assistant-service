package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.port.EmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;

public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingAdapter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
