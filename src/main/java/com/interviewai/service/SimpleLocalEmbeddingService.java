package com.interviewai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleLocalEmbeddingService implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(SimpleLocalEmbeddingService.class);
    private static final int DIM = 384;
    private static final int NGRAM_MIN = 2;
    private static final int NGRAM_MAX = 4;
    private static final int PROJECTIONS = 3;

    public SimpleLocalEmbeddingService() {
        log.info("SimpleLocalEmbeddingService 就绪: 纯 JDK n-gram 随机投影, 维度={}", DIM);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            float[] vec = embed(text);
            if (vec != null) embeddings.add(new Embedding(vec, index));
            index++;
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        if (document == null || document.getContent() == null) return null;
        return embed(document.getContent());
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) return minimalVector();
        float[] vec = new float[DIM];
        String s = text;
        int len = s.length();

        for (int n = NGRAM_MIN; n <= NGRAM_MAX && n <= len; n++) {
            for (int i = 0; i <= len - n; i++) {
                String ngram = s.substring(i, i + n);
                byte[] bytes = ngram.getBytes(StandardCharsets.UTF_8);
                long hash = hash64(bytes);

                for (int p = 0; p < PROJECTIONS; p++) {
                    long seed = hash ^ (0x9E3779B97F4A7C15L * (p + 1));
                    int idx = (int) (Long.remainderUnsigned(seed, DIM));
                    float sign = ((seed >>> 32) & 1) == 0 ? 1.0f : -1.0f;
                    vec[idx] += sign;
                }
            }
        }

        normalize(vec);
        return vec;
    }

    @Override
    public int dimensions() {
        return DIM;
    }

    private long hash64(byte[] data) {
        long h = 0x811C9DC5L;
        for (byte b : data) {
            h ^= (b & 0xFF);
            h *= 0x01000193L;
        }
        h ^= (h >>> 16);
        h *= 0x85EBCA6BL;
        h ^= (h >>> 13);
        h *= 0xC2B2AE35L;
        h ^= (h >>> 16);
        return h;
    }

    private void normalize(float[] vec) {
        float sumSq = 0f;
        for (float v : vec) sumSq += v * v;
        if (sumSq == 0) return;
        float norm = (float) Math.sqrt(sumSq);
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= norm;
        }
    }

    private float[] minimalVector() {
        float[] vec = new float[DIM];
        float val = 1.0f / (float) Math.sqrt(DIM);
        for (int i = 0; i < DIM; i++) {
            vec[i] = val;
        }
        return vec;
    }
}