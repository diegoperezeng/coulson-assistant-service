package com.yellowleaf.coulson.infrastructure.config;

import com.yellowleaf.coulson.application.usecase.ProcessVideoUseCase;
import com.yellowleaf.coulson.domain.port.AudioExtractorPort;
import com.yellowleaf.coulson.domain.port.AudioMergerPort;
import com.yellowleaf.coulson.domain.port.AudioSplitterPort;
import com.yellowleaf.coulson.domain.port.SpeakerResolutionPort;
import com.yellowleaf.coulson.domain.port.TranscriptionWebhookPort;
import com.yellowleaf.coulson.domain.port.VideoSplitterPort;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import com.yellowleaf.coulson.domain.service.VideoSplitPlanService;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioExtractorAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioMergerAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioSplitterAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegVideoSplitterAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.HttpSpeakerResolutionAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.HttpTranscriptionWebhookAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.LocalVideoStorageAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties({WebhookProperties.class, VideoProperties.class, FfmpegProperties.class})
public class AppConfig {

    @Bean
    public RestTemplate webhookRestTemplate(WebhookProperties webhookProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(webhookProperties.getConnectTimeoutSeconds() * 1000);
        factory.setReadTimeout(webhookProperties.getReadTimeoutSeconds() * 1000);
        return new RestTemplate(factory);
    }

    @Bean
    public ExecutorService processingExecutor(WebhookProperties webhookProperties) {
        return Executors.newFixedThreadPool(webhookProperties.getTranscriptionParallelism());
    }

    @Bean
    public VideoSplitPlanService videoSplitPlanService() {
        return new VideoSplitPlanService();
    }

    @Bean
    public VideoStoragePort videoStoragePort(VideoProperties videoProperties) {
        return new LocalVideoStorageAdapter(videoProperties);
    }

    @Bean
    public VideoSplitterPort videoSplitterPort(
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties,
            VideoSplitPlanService videoSplitPlanService) {
        return new FfmpegVideoSplitterAdapter(ffmpegProperties, videoProperties, videoSplitPlanService);
    }

    @Bean
    public AudioExtractorPort audioExtractorPort(
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties) {
        return new FfmpegAudioExtractorAdapter(ffmpegProperties, videoProperties);
    }

    @Bean
    public AudioMergerPort audioMergerPort(
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties) {
        return new FfmpegAudioMergerAdapter(ffmpegProperties, videoProperties);
    }

    @Bean
    public AudioSplitterPort audioSplitterPort(
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties) {
        return new FfmpegAudioSplitterAdapter(ffmpegProperties, videoProperties);
    }

    @Bean
    public TranscriptionWebhookPort transcriptionWebhookPort(
            RestTemplate webhookRestTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        return new HttpTranscriptionWebhookAdapter(webhookRestTemplate, webhookProperties, objectMapper);
    }

    @Bean
    public SpeakerResolutionPort speakerResolutionPort(
            RestTemplate webhookRestTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        return new HttpSpeakerResolutionAdapter(webhookRestTemplate, webhookProperties, objectMapper);
    }

    @Bean
    public ProcessVideoUseCase processVideoUseCase(
            VideoStoragePort videoStoragePort,
            VideoSplitterPort videoSplitterPort,
            AudioExtractorPort audioExtractorPort,
            AudioMergerPort audioMergerPort,
            AudioSplitterPort audioSplitterPort,
            TranscriptionWebhookPort transcriptionWebhookPort,
            SpeakerResolutionPort speakerResolutionPort,
            ExecutorService processingExecutor,
            VideoProperties videoProperties) {
        return new ProcessVideoUseCase(
                videoStoragePort,
                videoSplitterPort,
                audioExtractorPort,
                audioMergerPort,
                audioSplitterPort,
                transcriptionWebhookPort,
                speakerResolutionPort,
                processingExecutor,
                videoProperties.getMaxAudioChunkSeconds());
    }
}
