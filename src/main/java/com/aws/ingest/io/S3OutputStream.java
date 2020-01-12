package com.aws.ingest.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.Md5Utils;
import com.aws.ingest.exception.IngestException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * An OutputStream that buffers data and flushes to S3 using multipart put
 * while computing an MD5 hash of the overall upload
 */
public class S3OutputStream extends OutputStream
{
    private final Logger LOGGER = Logger.getLogger(S3OutputStream.class);

    private static final int FIVE_MB = 5 * 1024 * 1024;

    private final AmazonS3 s3;
    private final String bucket;
    private final String key;
    private String kmsKeyId = null;
    private final int partSize;

    private final ByteArrayOutputStream outputBuffer;
    private MessageDigest digest;

    private long length = 0L;
    private long totalLength = 0L;

    private String uploadId = null;
    private int partNumber = 1;
    private String etag = null;
    private List<PartETag> partETags = new ArrayList<>();

    /**
     * Creates an output stream to S3
     * @param s3 the S3 client
     * @param bucket the bucket
     * @param key the key
     * @param partSize the maximum part size
     */
    public S3OutputStream(AmazonS3 s3, String bucket, String key, int partSize)
    {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
        this.partSize = partSize;

        resetDigest();

        if (partSize < FIVE_MB)
        {
            throw new IllegalArgumentException("Part size must be > 5MB");
        }

        outputBuffer = new ByteArrayOutputStream(partSize);
    }


    /**
     * Enables a KMS key
     * @param kmsKeyId the key id
     * @return the output stream
     */
    public S3OutputStream withKmsKey(String kmsKeyId)
    {
        this.kmsKeyId = kmsKeyId;
        return this;
    }

    /**
     * Aborts the upload
     */
    public void abort()
    {
        if (uploadId != null)
        {
            AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucket, key, uploadId);
            s3.abortMultipartUpload(request);
            uploadId = null;
            LOGGER.info("Aborted multipart upload to: " + getOutputPath());
        }
    }

    /**
     * Writes a byte to the buffer
     * @param b the byte to write
     * @throws IOException thrown on failure to write
     */
    @Override
    public void write(int b) throws IOException
    {
        outputBuffer.write(b);
        length++;
        totalLength++;

        if (length == partSize)
        {
            flushPart(false);
        }
    }

    /**
     * Flushes a part
     * @param isLastPart true if this is the last part
     */
    private void flushPart(boolean isLastPart)
    {
        if (length == 0)
        {
            return;
        }

        if (uploadId == null)
        {
            startMultipart();
        }

        byte [] outputData = outputBuffer.toByteArray();

        outputBuffer.reset();
        length = 0;

        ByteArrayInputStream bytesIn = new ByteArrayInputStream(outputData);

        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                .withBucketName(bucket)
                .withKey(key)
                .withPartNumber(partNumber)
                .withPartSize(outputData.length)
                .withUploadId(uploadId)
                .withLastPart(isLastPart)
                .withMD5Digest(Md5Utils.md5AsBase64(outputData))
                .withInputStream(bytesIn);

        UploadPartResult response = s3.uploadPart(uploadPartRequest);
        partETags.add(new PartETag(partNumber, response.getETag()));

        digest.update(outputData);

        LOGGER.info("Part uploaded: " + partNumber + " to: " + getOutputPath());

        partNumber++;
    }

    /**
     * Starts a multipart upload
     */
    private void startMultipart()
    {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key)
            .withCannedACL(CannedAccessControlList.BucketOwnerFullControl);

        if (kmsKeyId != null)
        {
            request.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams().withAwsKmsKeyId(kmsKeyId));
        }

        InitiateMultipartUploadResult result = s3.initiateMultipartUpload(request);
        uploadId = result.getUploadId();

        LOGGER.info("Commenced upload to: " + getOutputPath());
    }

    /**
     * Completes a multipart upload
     */
    private void completeUpload()
    {
        CompleteMultipartUploadRequest request = 
                new CompleteMultipartUploadRequest(bucket, key, uploadId, partETags);

        CompleteMultipartUploadResult result = s3.completeMultipartUpload(request);

        etag = result.getETag();

        uploadId = null;

        LOGGER.info("Upload completed to: " + getOutputPath() +
            " ETag: " + etag + " MD5: " + getDigestBase64());
    }

    @Override
    public void close() throws IOException
    {
        flushPart(true);

        if (uploadId != null)
        {
            completeUpload();
            LOGGER.info("Stream closed to: " + getOutputPath());
        }
        else
        {
            LOGGER.warn("Stream is closed, ignoring");
        }


    }

    public byte [] getDigest()
    {
        return digest.digest();
    }

    public String getDigestBase64()
    {
        return Base64.encodeBase64String(digest.digest());
    }

    public String getETag()
    {
        return etag;
    }

    public String getOutputPath()
    {
        return String.format("s3://%s/%s", bucket, key);
    }

    private void resetDigest()
    {
        try
        {
            digest = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IngestException("Failed to reset digest", e);
        }
    }

    public long getTotalLength()
    {
        return totalLength;
    }
}
