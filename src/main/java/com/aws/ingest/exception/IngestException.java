package com.aws.ingest.exception;

/**
 * Runtime exception
 */
public class IngestException extends RuntimeException
{
    public IngestException(String message)
    {
        super(message);
    }

    public IngestException(String message, Throwable t)
    {
        super(message, t);
    }
}
