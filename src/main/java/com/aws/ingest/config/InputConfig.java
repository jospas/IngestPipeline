package com.aws.ingest.config;

import com.aws.ingest.exception.IngestException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for input processing
 */
public class InputConfig
{
    private String version = null;
    private String outputBucket = null;

    private final List<DataType> dataTypes = new ArrayList<>();

    public List<DataType> getDataTypes()
    {
        return dataTypes;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * Parses input config file from Json
     * @param json the json to parse
     * @return the parsed input config
     */
    public static InputConfig fromJSON(String json)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, InputConfig.class);
    }

    /**
     * Fetches a data type by name or throws
     * @param name the name of the data type
     * @return the named data type
     */
    public DataType getDataType(String name)
    {
        for (DataType dt: dataTypes)
        {
            if (dt.getName().equals(name))
            {
                return dt;
            }
        }

        throw new IngestException("Failed to locate data type for: " + name);
    }

    public String getOutputBucket()
    {
        return outputBucket;
    }

    public void setOutputBucket(String outputBucket)
    {
        this.outputBucket = outputBucket;
    }
}
