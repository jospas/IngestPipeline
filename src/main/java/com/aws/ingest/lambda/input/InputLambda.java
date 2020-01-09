package com.aws.ingest.lambda.input;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.aws.ingest.config.DataType;
import com.aws.ingest.config.InputConfig;
import com.aws.ingest.exception.IngestException;
import com.aws.ingest.manifest.Manifest;
import com.aws.ingest.manifest.ManifestEntry;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Lambda function that ingests CSV files,
 * validates them and transforms them
 */
@SuppressWarnings("unused")
public class InputLambda implements RequestHandler<SQSEvent, Void>
{
    private static LambdaLogger LOGGER = null;

    /**
     * Static lazily loaded input configuration
     */
    private static InputConfig inputConfiguration = null;

    /**
     * Static reusable S3 client
     */
    private static AmazonS3 s3 = null;

    /**
     * Input event handler function that receives the SQS event
     * @param event the event
     * @param context the Lambda context
     * @return returns null
     */
    @Override
    public Void handleRequest(SQSEvent event, Context context)
    {
        LOGGER = context.getLogger();

        for (SQSMessage msg : event.getRecords())
        {
            LOGGER.log("Received request message: " + msg.getBody());

            processEvent(msg.getBody());
        }

        LOGGER.log("Processing is complete");
        return null;
    }

    /**
     * Processes an SQS message string which should
     * contain a serialised S3EventNotification
     * @param message the message to process
     */
    private void processEvent(String message)
    {
        S3EventNotification s3EventNotification = S3EventNotification.parseJson(message);

        for (S3EventNotification.S3EventNotificationRecord record: s3EventNotification.getRecords())
        {
            processRecord(record);
        }
    }

    /**
     * Processes an S3 event record
     * @param record the record to process
     */
    private void processRecord(S3EventNotification.S3EventNotificationRecord record)
    {
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();

        /**
         * Only process manifest.json objects
         */
        if (!key.matches("^.*manifest.json$"))
        {
            LOGGER.log("Skipping non-manifest object: " + key);
            return;
        }

        LOGGER.log("Found manifest object to process: " + key);

        Manifest manifest = loadManifest(bucket, key);

        LOGGER.log("Processing input data from source system: " + manifest.getSourceSystem() +
            " created: " + manifest.getCreatedDate());

        /**
         * Validate the manifest
         */
        validateManifest(manifest);

        /**
         * Process the entries in the manifest
         */
        processManifest(manifest);
    }

    /**
     * Validates the manifest
     * @param manifest the manifest to validate
     */
    private void validateManifest(Manifest manifest)
    {
        // TODO implement validation
        LOGGER.log("Validation is not yet implemented");
    }

    /**
     * Processed the manifest transforming each file
     * @param manifest the manifest to process
     */
    private void processManifest(Manifest manifest)
    {
        LOGGER.log("Found: " + getConfiguration().getDataTypes().size() + " configured data types");

        for (ManifestEntry entry: manifest.getManifestEntries())
        {
            processManifestEntry(entry);
        }

        LOGGER.log("Manifest processing is complete");
    }

    private void processManifestEntry(ManifestEntry entry)
    {
        DataType dataType = getConfiguration().getDataType(entry.getDataType());

        LOGGER.log("Processing entry: " + entry.getFileName());
    }

    /**
     * Loads the manifest from S3
     * @param bucket the bucket
     * @param key the key
     * @return the loaded manifest
     */
    private Manifest loadManifest(String bucket, String key)
    {
        String manifestString = loadS3Text(bucket, key);
        LOGGER.log("Loaded manifest: " + manifestString);
        return Manifest.fromJSON(manifestString);
    }

    /**
     * Lazily load configuration if not already loaded
     * @return the loaded input configuration
     */
    private InputConfig getConfiguration()
    {
        if (inputConfiguration != null)
        {
            return inputConfiguration;
        }

        String configString = loadS3Text(System.getenv("CONFIG_BUCKET"), "config/input_config.json");

        LOGGER.log("Cold start loaded config: " + configString);

        inputConfiguration = InputConfig.fromJSON(configString);

        return inputConfiguration;
    }

    /**
     * Loads text from S3
     * @param bucket the S3 bucket name
     * @param key the S3 key
     * @return the loaded string
     */
    private String loadS3Text(String bucket, String key) throws IngestException
    {
        try (S3Object s3Object = getS3().getObject(bucket, key))
        {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(s3Object.getObjectContent(), bytesOut);
            return bytesOut.toString("UTF-8");
        }
        catch (IOException io)
        {
            LOGGER.log("Failed to fetch S3 content: " + io.toString());
            throw new IngestException("Failed to fetch S3 content", io);
        }
    }

    /**
     * Lazily create an S3 client if required
     * @return an S3 client
     */
    private AmazonS3 getS3()
    {
        if (s3 != null)
        {
            return s3;
        }

        s3 = AmazonS3ClientBuilder.standard().build();

        return s3;
    }


}
