package com.aws.ingest.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A data type in the system
 */
public class DataType
{
    private String name = null;
    private boolean enabled = false;

    private final List<String> inputColumns = new ArrayList<>();
    private final List<String> outputColumns = new ArrayList<>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<String> getInputColumns()
    {
        return inputColumns;
    }

    public List<String> getOutputColumns()
    {
        return outputColumns;
    }
}
