package com.aws.ingest.manifest;

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
     * The secure hash of the content
     */
    private String secureHash = null;

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

    public String getSecureHash()
    {
        return secureHash;
    }

    public void setSecureHash(String secureHash)
    {
        this.secureHash = secureHash;
    }
}
