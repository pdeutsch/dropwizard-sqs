package smartthings.dropwizard.sqs;

import com.amazonaws.services.sqs.model.SendMessageResult;

public interface Producer {
    public SendMessageResult sendMessage(String messageBody);

    public SendMessageResult sendMessage(String messageBody, int delaySeconds);
}
