package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ClientUtils
{

	static Gson GSON = createGson();
	
	public static final String UTF8 = "UTF-8";

	private static final Gson createGson()
	{
		GsonBuilder builder = new GsonBuilder();
		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
		return builder.create();
	}

	public static final String toJson(Object object)
	{
		return GSON.toJson(object);
	}

	public static final <V> V fromJson(String json, Class<V> type)
	{
		return GSON.fromJson(json, type);
	}
	
	public static final <V> V fromJson(InputStream json, Type type)
	{
		try
		{
		
			return GSON.fromJson(new BufferedReader(new InputStreamReader(json, UTF8)), type);
		
		} catch (Exception e) {
			
			throw new BitbucketRequestException("Cannot parse input stream.", e);

		}
		
	}

}
