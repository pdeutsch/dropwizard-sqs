package smartthings.dropwizard.sqs.internal.consumer;

import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartthings.dropwizard.sqs.*;
import smartthings.dropwizard.sqs.internal.producer.DefaultProducer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SqsManager implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(SqsManager.class);

    private final Map<String, SqsService> sqsConsumerMap = new ConcurrentHashMap<>();
    private final Map<String, Producer> sqsProducerMap = new ConcurrentHashMap<>();
    private final SqsModule.Config config;
    private final AmazonSQSProvider sqsProvider;

    @Inject
    public SqsManager(SqsModule.Config config, AmazonSQSProvider sqsProvider) {
        this.config = config;
        this.sqsProvider = sqsProvider;
    }

    @Override
    public void start() {
        if (config.isEnabled()) {
            LOG.debug("Starting up SqsManager...");
            config.getConsumers().stream()
                    .filter(SqsModule.ConsumerConfig::isEnabled)
                    .map(SqsModule.ConsumerConfig::getEndpoints)
                    .flatMap(Collection::stream)
                    .forEach(this::createConsumer);

            config.getProducers().entrySet().stream()
                    .forEach(entry -> {
                        // reuse service if it already exists
                        String producerName = entry.getKey();
                        SqsModule.EndpointConfig endpointConfig = entry.getValue();
                        String consumerKey = getCacheKey(endpointConfig);
                        SqsService service = sqsConsumerMap.containsKey(consumerKey) ?
                                sqsConsumerMap.get(consumerKey):
                                createService(entry.getValue());
                        if (service != null) {
                            GetQueueUrlResult result = service.getQueueUrl(endpointConfig.getQueueName());
                            sqsProducerMap.put(producerName,
                                    new DefaultProducer(result.getQueueUrl(), sqsProvider.get(endpointConfig)));
                        }
                    });
        } else {
            LOG.debug("Skipping start up of SqsManager...");
        }
    }

    @Override
    public void stop() {
        LOG.debug("Shutting down SqsManager...");
    }

    public Producer getProducer(String producerEndpointName) {
        Producer producer = sqsProducerMap.get(producerEndpointName);
        if (producer == null) {
            LOG.error("No SQS Producer exists for name={}", producerEndpointName);
            throw new IllegalStateException("Unable to resolve SQS Producer for name: " + producerEndpointName);
        }
        return producer;
    }

    public SqsService get(SqsModule.EndpointConfig config) {
        SqsService sqs = sqsConsumerMap.get(getCacheKey(config));
        if (sqs == null) {
            LOG.error(
                "No SQS client exists for region={} endpoint={}",
                config.getRegionName(), config.endpoint().orElse("none")
            );
            throw new IllegalStateException("Unable to resolve SQS client for Endpoint");
        }
        return sqs;
    }

    private SqsService createProducer(SqsModule.EndpointConfig config) {
        return null;
    }

    private SqsService createConsumer(SqsModule.EndpointConfig config) {
        String cacheKey = getCacheKey(config);
        if (sqsConsumerMap.containsKey(cacheKey)) {
            return sqsConsumerMap.get(cacheKey);
        }
        LOG.debug("Creating SqsService for endpoint={}", cacheKey);
        SqsService sqsService = createService(config);
        sqsConsumerMap.put(cacheKey, sqsService);
        return sqsService;
    }

    private SqsService createService(SqsModule.EndpointConfig config) {
        if (config.getRegionName() == null) {
            throw new IllegalArgumentException("Consumer endpoint config requires a valid configured AWS Region.");
        }
        SqsService sqsService = new DefaultSqsService(sqsProvider.get(config));
        return sqsService;
    }

    private String getCacheKey(SqsModule.EndpointConfig config) {
        return config.getRegionName() + ":" + config.endpoint().orElse("none");
    }

}
