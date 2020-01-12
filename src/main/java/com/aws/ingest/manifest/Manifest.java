package com.aws.ingest.manifest;

import com.aws.ingest.exception.IngestException;
import com.aws.ingest.io.MD5InputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

/**
 * A manifest for uploaded assets
 */
public class Manifest
{
    private static final Logger LOGGER = Logger.getLogger(Manifest.class);

    private String createdDate = null;
    private String sourceSystem = null;

    private final List<ManifestEntry> manifestEntries = new ArrayList<>();

    /**
     * Properties injected on load
     */
    private transient String bucket;
    private transient String key;
    private transient String parentKey;

    private String version;

    public String getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(String createdDate)
    {
        this.createdDate = createdDate;
    }

    public String getSourceSystem()
    {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem)
    {
        this.sourceSystem = sourceSystem;
    }

    public List<ManifestEntry> getManifestEntries()
    {
        return manifestEntries;
    }

    /**
     * Parses a manifest file from Json
     * @param json the json to parse
     * @return the parsed manifest
     */
    public static Manifest fromJSON(String json)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, Manifest.class);
    }

    /**
     * Converts a Manifest object to JSON
     * @param manifest the manifest
     * @return the manifest as JSON
     */
    public static String toJSON(Manifest manifest)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(manifest);
    }


    public void setKey(String key)
    {
        this.key = key;
        this.parentKey = new File(key).getParent() + "/";
    }

    public String getBucket()
    {
        return bucket;
    }

    public void setBucket(String bucket)
    {
        this.bucket = bucket;
    }

    public String getKey()
    {
        return key;
    }

    public String getKeyForEntry(ManifestEntry entry)
    {
        return parentKey + entry.getFileName();
    }

    /**
     * The client must compute the MD5 sum of the raw data read
     * @param publicKey the public key to verify with
     */
    public void verifySignature(PublicKey publicKey)
    {
        try
        {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            for (ManifestEntry entry: manifestEntries)
            {
                /**
                 * Require an MD5 for each entry
                 */
                if (StringUtils.isBlank(entry.getMd5()))
                {
                    throw new IngestException("MD5 sum was missing on: " + entry.getFileName());
                }

                /**
                 * Require signatures for each entry
                 */
                if (StringUtils.isBlank(entry.getSignature()))
                {
                    throw new IngestException("Signature was missing on: " + entry.getFileName());
                }

                signature.update(entry.getMd5().getBytes());

                if (!signature.verify(Base64.decodeBase64(entry.getSignature())))
                {
                    LOGGER.error("Signature verification failed");
                    throw new IngestException("Failed to verify signature for entry: " + entry.getFileName());
                }
                else
                {
                    LOGGER.info("Successfully verified manifest signatures");
                }
            }
        }
        catch (IngestException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new IngestException("Failed to verify manifest", t);
        }
    }

    /**
     * TODO implement signing manifests
     * @param privateKey the private key to sign with
     */
    public void signManifest(PrivateKey privateKey)
    {
        try
        {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            for (ManifestEntry entry: manifestEntries)
            {
                if (StringUtils.isBlank(entry.getMd5()))
                {
                    throw new IngestException("MD5 sum was missing from entry: " + entry.getFileName());
                }

                signature.update(entry.getMd5().getBytes());
                entry.setSignature(Base64.encodeBase64String(signature.sign()));
            }

            LOGGER.info("Successfully signed manifest entries");
        }
        catch (IngestException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new IngestException("Failed to sign message");
        }
    }

    public Manifest clone()
    {
        return Manifest.fromJSON(Manifest.toJSON(this));
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void computeMD5SumsFromFiles(File directory) throws IOException
    {
        for (ManifestEntry entry: manifestEntries)
        {
            FileInputStream fileIn = new FileInputStream(new File(directory, entry.getFileName()));
            MD5InputStream md5In = new MD5InputStream(fileIn);

            while (true)
            {
                int read = md5In.read();

                if (read == -1)
                {
                    break;
                }
            }

            entry.setMd5(md5In.getDigestBase64());

            LOGGER.info("Made MD5 sum: " + entry.getMd5() + " for entry: " + entry.getFileName());
        }
    }
}
