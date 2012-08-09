package com.atlassian.jira.plugins.dvcs.ondemand;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class JsonFileBasedAccountsConfigProvider implements AccountsConfigProvider
{

    private static Logger log = LoggerFactory.getLogger(JsonFileBasedAccountsConfigProvider.class);

    private String absoluteConfigFilePath = "/data/jirastudio/home/ondemand.properties";

    public JsonFileBasedAccountsConfigProvider()
    {
        super();
    }

    @Override
    public AccountsConfig provideConfiguration()
    {

        try
        {
            File configFile = new File(absoluteConfigFilePath);
            if (!configFile.exists() || !configFile.canRead()) {
                throw new IllegalStateException(absoluteConfigFilePath + " file can not be read.");
            }
            
            AccountsConfig config = null;

            GsonBuilder builder = new GsonBuilder();
            builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
            Gson gson = builder.create();
            
            config = gson.fromJson(new InputStreamReader(new FileInputStream(configFile)), AccountsConfig.class);
            
            return config;
        }
        catch (JsonParseException json) {
            log.error("Failed to parse config file " + absoluteConfigFilePath, json);
            return null;
        }
        catch (Exception e)
        {
            log.debug("File not found, probably not ondemand instance or integrated account should be deleted. ", e);
            return null;
        }
    }

    @Override
    public boolean supportsIntegratedAccounts()
    {
       return true;
    }

    public void setAbsoluteConfigFilePath(String absoluteConfigFilePath)
    {
        this.absoluteConfigFilePath = absoluteConfigFilePath;
    }
    
}