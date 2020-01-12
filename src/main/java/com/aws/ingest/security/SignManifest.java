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
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Sample code to sign a manifest
 */
public class SignManifest
{
    private static final Logger LOGGER = Logger.getLogger(SignManifest.class);

    public static void main(String [] args) throws IOException
    {
        if (args.length != 2)
        {
            usage();
            throw new IngestException("Invalid parameters");
        }

        try {

            String manifestFilePath = args[0];
            String privateKeyPath = args[1];

            File manifestFile = new File(manifestFilePath);
            File privateKeyFile = new File(privateKeyPath);

            String json = FileUtils.readFileToString(manifestFile, "UTF-8");
            String privateKeyString = FileUtils.readFileToString(privateKeyFile, "UTF-8");

            Manifest manifest = Manifest.fromJSON(json);

            manifest.computeMD5SumsFromFiles(manifestFile.getParentFile());

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyString));
            PrivateKey privateKey = kf.generatePrivate(keySpecPKCS8);

            manifest.signManifest(privateKey);

            String signedJson = Manifest.toJSON(manifest);

            FileUtils.write(manifestFile, signedJson, "UTF-8");
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to sign manifest", t);
            throw new IngestException("Failed to sign a manifest", t);
        }

    }

    private static void usage()
    {
        LOGGER.info("Usage: SignManifest <manifest_file> <private_key_file>");
    }
}
