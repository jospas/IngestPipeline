package com.aws.ingest.security;

import com.aws.ingest.exception.IngestException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Helper class to generate keys
 */
public class KenGenerator
{
    private static final Logger LOGGER = Logger.getLogger(KenGenerator.class);

    public static void main(String [] args)
    {
        generateKeys("input");
        generateKeys("processed");
    }

    private static void generateKeys(String name)
    {
        try
        {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.genKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            String publicKeyString = Base64.encodeBase64String(publicKey.getEncoded());
            String privateKeyString = Base64.encodeBase64String(privateKey.getEncoded());

            FileUtils.write(new File("data/" + name + "_public_key.txt"), publicKeyString, "UTF-8");
            FileUtils.write(new File("data/" + name + "_private_key.txt"), privateKeyString, "UTF-8");

        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to generate keys", t);
            throw new IngestException("Failed to generate keys", t);
        }
    }
}
