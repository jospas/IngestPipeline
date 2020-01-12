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
import com.aws.ingest.io.S3OutputStream;
import com.aws.ingest.manifest.Manifest;
import com.aws.ingest.manifest.ManifestEntry;
import com.aws.ingest.io.MD5InputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

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

        inputConfiguration = null;

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
         * Process the entries in the manifest
         */
        processManifest(getConfiguration(), manifest);
    }

    /**
     * Processed the manifest transforming each file
     * @param manifest the manifest to process
     */
    private void processManifest(InputConfig config, Manifest manifest)
    {
        LOGGER.log("Found: " + getConfiguration().getDataTypes().size() + " configured data types");

        String outputBucket = System.getenv("PROCESSED_BUCKET");

        if (StringUtils.isBlank(outputBucket))
        {
            throw new IngestException("Missing environment variable: PROCESSED_BUCKET");
        }

        String outputKMSKey = System.getenv("PROCESSED_KMS_ID");

        if (StringUtils.isBlank(outputKMSKey))
        {
            throw new IngestException("Missing environment variable: PROCESSED_KMS_ID");
        }

        LOGGER.log("Found processed KMS Key to use: " + outputKMSKey);

        Manifest outputManifest = new Manifest();

        Date now = new Date();
        outputManifest.setCreatedDate(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm'Z'").format(now));

        outputManifest.setBucket(outputBucket);
        outputManifest.setKey(manifest.getKey());
        outputManifest.setSourceSystem(manifest.getSourceSystem());
        outputManifest.setVersion("1.0.0");

        for (ManifestEntry entry: manifest.getManifestEntries())
        {
            processManifestEntry(config, manifest, entry, outputManifest, outputKMSKey);
        }

        String publicKeyString = System.getenv("PUBLIC_KEY");

        if (StringUtils.isBlank(publicKeyString))
        {
            throw new IngestException("Missing environment variable: PUBLIC_KEY use 'None' to disable");
        }

        if (!"None".equals(publicKeyString))
        {
            PublicKey publicKey = loadPublicKey(publicKeyString);
            manifest.verifySignature(publicKey);
        }
        else
        {
            LOGGER.log("Skipping signature verification");
        }

        String privateKeyString = System.getenv("PRIVATE_KEY");

        if (StringUtils.isBlank(privateKeyString))
        {
            throw new IngestException("Missing environment variable: PRIVATE_KEY use 'None' to disable");
        }

        if (!"None".equals(privateKeyString))
        {
            PrivateKey privateKey = loadPrivateKey(privateKeyString);
            outputManifest.signManifest(privateKey);
        }
        else
        {
            LOGGER.log("Skipping manifest signing");
        }

        saveManifest(s3, outputManifest, outputKMSKey);

        LOGGER.log("Manifest processing is complete");
    }

    private void saveManifest(AmazonS3 s3, Manifest outputManifest, String processedKMSId)
    {
        int tenMB = 10 * 1024 * 1024;

        try
        {
            S3OutputStream out = new S3OutputStream(s3, outputManifest.getBucket(),
                    outputManifest.getKey(), tenMB).withKmsKey(processedKMSId);
            String json = Manifest.toJSON(outputManifest);
            LOGGER.log("Generated processed manifest: " + Manifest.toJSON(outputManifest));
            out.write(json.getBytes());
            out.close();
        }
        catch (Throwable t)
        {
            LOGGER.log("Failed to save completed manifest, cause: " + t.toString());
            throw new IngestException("Failed to save manifest", t);
        }
    }

    /**
     * Processes a Manifest entry returning the manifets entry for the processed entry
     * @param inputManifest the input manifest
     * @param config the config
     * @param entry the entry to process
     * @param outputManifest the output manifest
     * @param outputKMSKey the output KMS key id
     */
    private void processManifestEntry(InputConfig config,
              Manifest inputManifest,
              ManifestEntry entry,
              Manifest outputManifest,
              String outputKMSKey)
    {
        DataType dataType = getConfiguration().getDataType(entry.getDataType());

        ManifestEntry outputEntry = entry.clone();
        outputEntry.reset();

        String inputEntryKey = inputManifest.getKeyForEntry(entry);
        String outputEntryKey = outputManifest.getKeyForEntry(entry);

        LOGGER.log(String.format("Processing entry from: s3://%s/%s to s3://%s/%s",
                inputManifest.getBucket(), inputEntryKey,
                outputManifest.getBucket(), outputEntryKey));

        /**
         * 10MB buffer size and part size
         */
        int bufferSize = 1024 * 1024 * 10;

        /**
         * A buffer for an output row
         */
        List<String> outputBuffer = new ArrayList<>();

        S3OutputStream s3Out = null;

        try
        (
            /**
             * Open an input stream to the incoming object in S3
             * Pipe the input stream from S3 through an MD5 sum
             * Read CSV data through the digest via a reader
             */
            S3Object s3Object = getS3().getObject(inputManifest.getBucket(), inputEntryKey);
            MD5InputStream digestIn = new MD5InputStream(s3Object.getObjectContent());
            BufferedReader s3Reader = new BufferedReader(new InputStreamReader(digestIn), bufferSize)
        )
        {
            s3Out = new S3OutputStream(s3, outputManifest.getBucket(),
                    outputEntryKey, bufferSize).withKmsKey(outputKMSKey);

            OutputStreamWriter s3Writer = new OutputStreamWriter(s3Out);

            CSVParser parser = new CSVParser(s3Reader,
                CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .withHeader(dataType.getInputColumns().toArray(new String [0])));
            CSVPrinter printer = new CSVPrinter(s3Writer,
                CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .withHeader(dataType.getOutputColumns().toArray(new String [0])));

            printer.printRecord(dataType.getOutputColumns().toArray());

            for (CSVRecord row : parser)
            {
                outputBuffer.clear();
                entry.processRow(dataType, row, outputBuffer);
                printer.printRecord(outputBuffer.toArray());
                outputEntry.incrementRowCount();
            }

            s3Writer.flush();
            s3Writer.close();

            entry.setMd5(digestIn.getDigestBase64());
            outputEntry.setMd5(s3Out.getDigestBase64());
            outputManifest.getManifestEntries().add(outputEntry);
        }
        catch (Throwable t)
        {
            LOGGER.log("[ERROR] Aborting upload due to failure processing entry: " + entry.getFileName() +
                    " cause: " + t.toString());

            s3Out.abort();

            throw new IngestException("Failed to process manifest entry: "
                    + entry.getFileName(), t);
        }
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
        Manifest manifest = Manifest.fromJSON(manifestString);

        /**
         * Inject the bucket and key into the manifest
         */
        manifest.setBucket(bucket);
        manifest.setKey(key);

        return manifest;
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

        LOGGER.log("Loaded configuration: " + configString);

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
        catch (Throwable t)
        {
            LOGGER.log("[ERROR] Failed to fetch S3 content: " + t.toString());
            throw new IngestException("Failed to fetch S3 content", t);
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

    private PublicKey loadPublicKey(String publicKeyString)
    {
        try
        {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyString));
            return kf.generatePublic(keySpecX509);
        }
        catch (Throwable t)
        {
            throw new IngestException("Failed to load public key", t);
        }
    }

    private PrivateKey loadPrivateKey(String privateKeyString)
    {
        try
        {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyString));
            return kf.generatePrivate(keySpecPKCS8);
        }
        catch (Throwable t)
        {
            throw new IngestException("Failed to load public key", t);
        }
    }


}
