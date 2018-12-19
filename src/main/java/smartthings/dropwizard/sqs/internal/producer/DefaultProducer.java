package smartthings.dropwizard.sqs.internal.producer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.inject.Inject;
import smartthings.dropwizard.sqs.Producer;

import java.util.Map;

/**
 * A Producer that can write a message to one queue, with an optional delay.
 */
public class DefaultProducer implements Producer {
    private final String queueUrl;
    private final AmazonSQS sqs;

    @Inject
    public DefaultProducer(String queueUrl, AmazonSQS sqs) {
        this.queueUrl = queueUrl;
        this.sqs = sqs;
    }

    @Override
    public SendMessageResult sendMessage(String messageBody) {
        return sendMessage(messageBody, null, null);
    }

    @Override
    public SendMessageResult sendMessage(String messageBody, Integer delaySeconds) {
        return sendMessage(messageBody, delaySeconds, null);
    }

    @Override
    public SendMessageResult sendMessage(String messageBody, Integer delaySeconds, Map<String, MessageAttributeValue> attributeValueMap) {
        SendMessageRequest request = new SendMessageRequest(queueUrl, messageBody);
        if (delaySeconds != null) {
            request.setDelaySeconds(delaySeconds);
        }
        if (attributeValueMap != null && attributeValueMap.size() > 0) {
            request.setMessageAttributes(attributeValueMap);
        }
        return sqs.sendMessage(request);
    }
}
