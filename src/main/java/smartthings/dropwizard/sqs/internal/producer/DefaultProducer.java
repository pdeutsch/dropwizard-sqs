package smartthings.dropwizard.sqs.internal.producer;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import smartthings.dropwizard.sqs.Producer;
import smartthings.dropwizard.sqs.SqsService;

public class DefaultProducer implements Producer {
    private String queueUrl;
    private SqsService sqsService;

    public DefaultProducer(String queueUrl, SqsService sqsService) {
        this.queueUrl = queueUrl;
        this.sqsService = sqsService;
    }

    @Override
    public SendMessageResult sendMessage(String messageBody) {
        return sqsService.sendMessage(new SendMessageRequest(queueUrl, messageBody));
    }

    @Override
    public SendMessageResult sendMessage(String messageBody, int delaySeconds) {
        return sqsService.sendMessage(new SendMessageRequest(queueUrl, messageBody).withDelaySeconds(delaySeconds));
    }
}
