package com.aws.ingest.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.apache.log4j.Logger;

/**
 * Lambda function that ingests CSV files,
 * validates them and transforms them
 */
@SuppressWarnings("unused")
public class InputLambda implements RequestHandler<SQSEvent, Void>
{
    private static final Logger LOGGER = Logger.getLogger(InputLambda.class);

    @Override
    public Void handleRequest(SQSEvent event, Context context)
    {
        for (SQSMessage msg : event.getRecords())
        {
            System.out.println(new String(msg.getBody()));
        }
        return null;
    }
}
