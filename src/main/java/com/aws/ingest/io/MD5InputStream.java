package com.aws.ingest.io;

import com.amazonaws.util.Md5Utils;
import com.aws.ingest.exception.IngestException;
import org.apache.commons.codec.binary.Base64;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes MD5 sums of data read through an input stream
 */
public class MD5InputStream extends FilterInputStream
{
    private MessageDigest digest;

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public MD5InputStream(InputStream in)
    {
        super(in);
        resetDigest();
    }

    public byte [] getDigest()
    {
        return digest.digest();
    }

    public String getDigestBase64()
    {
        return Base64.encodeBase64String(digest.digest());
    }

    @Override
    public int read() throws IOException
    {
        int ch = super.read();

        if (ch != -1)
        {
            digest.update((byte)ch);
        }

        return ch;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int result = super.read(b, off, len);

        if (result != -1)
        {
            digest.update(b, off, result);
        }

        return result;
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

}
