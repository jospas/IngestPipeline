package com.aws.ingest.manifest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A manifest for uploaded assets
 */
public class Manifest
{
    private String createdDate = null;
    private String sourceSystem = null;

    private final List<ManifestEntry> manifestEntries = new ArrayList<>();

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
}
