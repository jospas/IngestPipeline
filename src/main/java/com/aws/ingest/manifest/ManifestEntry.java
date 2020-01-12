package com.aws.ingest.manifest;

import com.aws.ingest.config.DataType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * An entry in the manifest representing an uploaded CSV file
 */
public class ManifestEntry
{
    /**
     * The type of data in the file
     */
    private String dataType = null;

    /**
     * The number of rows in the file
     */
    private long rowCount = 0L;

    /**
     * The name of the file
     */
    private String fileName = null;

    /**
     * The signed MD5 of this entry's contents
     */
    private String signature = null;

    /**
     * The MD5 of the file contents, never persisted
     */
    private transient String md5 = null;

    public String getDataType()
    {
        return dataType;
    }

    public void setDataType(String dataType)
    {
        this.dataType = dataType;
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(long rowCount)
    {
        this.rowCount = rowCount;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5(String md5)
    {
        this.md5 = md5;
    }

    public void processRow(DataType dataType, CSVRecord row, List<String> outputBuffer)
    {
        for (String outputColumn: dataType.getOutputColumns())
        {
            outputBuffer.add(row.get(outputColumn));
        }
    }

    /**
     * Clones via JSON
     * @return a clone of this
     */
    public ManifestEntry clone()
    {
        return ManifestEntry.fromJSON(ManifestEntry.toJSON(this));
    }

    /**
     * Parses a ManifestEntry from Json
     * @param json the json to parse
     * @return the parsed ManifestEntry
     */
    public static ManifestEntry fromJSON(String json)
    {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, ManifestEntry.class);
    }

    /**
     * Converts a ManifestEntry object to JSON
     * @param entry the ManifestEntry
     * @return the ManifestEntry as JSON
     */
    public static String toJSON(ManifestEntry entry)
    {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(entry);
    }

    public void reset()
    {
        rowCount = 0;
        signature = null;
        md5 = null;
    }

    public void incrementRowCount()
    {
        rowCount++;
    }
}

