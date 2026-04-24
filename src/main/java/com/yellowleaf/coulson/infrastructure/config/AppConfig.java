package com.yellowleaf.coulson.infrastructure.config;

import com.yellowleaf.coulson.application.usecase.ParticipantUseCase;
import com.yellowleaf.coulson.application.usecase.ProcessVideoUseCase;
import com.yellowleaf.coulson.application.usecase.TeamMemberUseCase;
import com.yellowleaf.coulson.application.usecase.TeamUseCase;
import com.yellowleaf.coulson.domain.port.AudioExtractorPort;
import com.yellowleaf.coulson.domain.port.AudioMergerPort;
import com.yellowleaf.coulson.domain.port.AudioSplitterPort;
import com.yellowleaf.coulson.domain.port.EmbeddingPort;
import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort;
import com.yellowleaf.coulson.domain.port.ParticipantRepositoryPort;
import com.yellowleaf.coulson.domain.port.StoragePort;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;
import com.yellowleaf.coulson.domain.port.SpeakerResolutionWebhookPort;
import com.yellowleaf.coulson.domain.port.TranscriptionWebhookPort;
import com.yellowleaf.coulson.domain.port.VideoSplitterPort;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import com.yellowleaf.coulson.domain.service.VideoSplitPlanService;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioExtractorAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioMergerAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegAudioSplitterAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.FfmpegVideoSplitterAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.HttpSpeakerResolutionWebhookAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.HttpTranscriptionWebhookAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.JpaMeetingRepositoryAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.JpaParticipantRepositoryAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.JpaTeamRepositoryAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.LocalVideoStorageAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.OpenAiEmbeddingAdapter;
import com.yellowleaf.coulson.infrastructure.adapter.SupabaseStorageAdapter;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringMeetingJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringParticipantJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTeamJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTeamMemberJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.VectorUpdateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties({WebhookProperties.class, VideoProperties.class, FfmpegProperties.class, SupabaseProperties.class})
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
    public SpeakerResolutionWebhookPort speakerResolutionWebhookPort(
            RestTemplate webhookRestTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        return new HttpSpeakerResolutionWebhookAdapter(webhookRestTemplate, webhookProperties, objectMapper);
    }

    @Bean
    public TranscriptionWebhookPort transcriptionWebhookPort(
            RestTemplate webhookRestTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        return new HttpTranscriptionWebhookAdapter(webhookRestTemplate, webhookProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.embedding", name = "enabled", havingValue = "true")
    public EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return new OpenAiEmbeddingModel(OpenAiApi.builder().apiKey(apiKey).build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai.embedding", name = "enabled", havingValue = "true")
    public EmbeddingPort embeddingPort(EmbeddingModel openAiEmbeddingModel) {
        return new OpenAiEmbeddingAdapter(openAiEmbeddingModel);
    }

    @Bean
    public Executor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("embedding-");
        executor.initialize();
        return executor;
    }

    @Bean
    public MeetingRepositoryPort meetingRepositoryPort(
            SpringMeetingJpaRepository jpaRepository,
            VectorUpdateRepository vectorRepository) {
        return new JpaMeetingRepositoryAdapter(jpaRepository, vectorRepository);
    }

    @Bean
    public RestTemplate storageRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
    }

    @Bean
    public StoragePort storagePort(RestTemplate storageRestTemplate, SupabaseProperties supabaseProperties) {
        return new SupabaseStorageAdapter(storageRestTemplate, supabaseProperties);
    }

    @Bean
    public ParticipantRepositoryPort participantRepositoryPort(SpringParticipantJpaRepository repository) {
        return new JpaParticipantRepositoryAdapter(repository);
    }

    @Bean
    public ParticipantUseCase participantUseCase(
            ParticipantRepositoryPort participantRepositoryPort,
            StoragePort storagePort,
            SupabaseProperties supabaseProperties) {
        return new ParticipantUseCase(
                participantRepositoryPort,
                storagePort,
                supabaseProperties.getStorage().getVoiceSamplesBucket());
    }

    @Bean
    public TeamRepositoryPort teamRepositoryPort(
            SpringTeamJpaRepository teamRepository,
            SpringTeamMemberJpaRepository memberRepository,
            SpringParticipantJpaRepository participantRepository) {
        return new JpaTeamRepositoryAdapter(teamRepository, memberRepository, participantRepository);
    }

    @Bean
    public TeamUseCase teamUseCase(TeamRepositoryPort teamRepositoryPort) {
        return new TeamUseCase(teamRepositoryPort);
    }

    @Bean
    public TeamMemberUseCase teamMemberUseCase(TeamRepositoryPort teamRepositoryPort) {
        return new TeamMemberUseCase(teamRepositoryPort);
    }

    @Bean
    public ProcessVideoUseCase processVideoUseCase(
            VideoStoragePort videoStoragePort,
            VideoSplitterPort videoSplitterPort,
            AudioExtractorPort audioExtractorPort,
            AudioMergerPort audioMergerPort,
            AudioSplitterPort audioSplitterPort,
            TranscriptionWebhookPort transcriptionWebhookPort,
            SpeakerResolutionWebhookPort speakerResolutionWebhookPort,
            MeetingRepositoryPort meetingRepositoryPort,
            ApplicationEventPublisher eventPublisher,
            ExecutorService processingExecutor,
            VideoProperties videoProperties) {
        return new ProcessVideoUseCase(
                videoStoragePort,
                videoSplitterPort,
                audioExtractorPort,
                audioMergerPort,
                audioSplitterPort,
                transcriptionWebhookPort,
                speakerResolutionWebhookPort,
                meetingRepositoryPort,
                eventPublisher,
                processingExecutor,
                videoProperties.getMaxAudioChunkSeconds());
    }
}
