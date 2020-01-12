package com.aws.ingest.security;

import com.aws.ingest.exception.IngestException;
import com.aws.ingest.manifest.Manifest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Sample code to verify a manifest
 */
public class VerifyManifest
{
    private static final Logger LOGGER = Logger.getLogger(VerifyManifest.class);

    public static void main(String [] args) throws IOException
    {
        if (args.length != 2)
        {
            usage();
            throw new IngestException("Invalid parameters");
        }

        try {

            String manifestFilePath = args[0];
            String publicKeyPath = args[1];

            File manifestFile = new File(manifestFilePath);
            File publicKeyFile = new File(publicKeyPath);

            String json = FileUtils.readFileToString(manifestFile, "UTF-8");
            String publicKeyString = FileUtils.readFileToString(publicKeyFile, "UTF-8");

            Manifest manifest = Manifest.fromJSON(json);

            manifest.computeMD5SumsFromFiles(manifestFile.getParentFile());

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyString));
            PublicKey publicKey = kf.generatePublic(keySpecX509);

            manifest.verifySignature(publicKey);
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to verify manifest", t);
            throw new IngestException("Failed to verify manifest", t);
        }

    }

    private static void usage()
    {
        LOGGER.info("Usage: VerifyManifest <manifest_file> <public_key_file>");
    }
}
