package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;

import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

/**
 * Java client for Bitbucket's Repository links API: 
 *      http://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
public class RepositoryLinksService
{
    private final BitbucketClient bitbucketClient;
    
    public RepositoryLinksService(BitbucketClient bitbucketClient)
    {
        this.bitbucketClient = bitbucketClient;
    }

    /**
     * @param owner
     * @param slug
     * @return
     * @throws BitbucketClientException
     */
    @SuppressWarnings("unchecked")
    public List<RepositoryLink> getRepositoryLinks(String owner, String slug) throws BitbucketClientException
    {
        Type type = new TypeToken<List<RepositoryLink>>(){}.getType();
        String resourceUrl = "/repositories/"+owner+"/"+slug+"/links";
        return (List<RepositoryLink>) bitbucketClient.get(resourceUrl, type);
    }
 
    /**
     * @param owner
     * @param slug
     * @param id
     * @throws BitbucketClientException
     */
    public void removeRepositoryLink(String owner, String slug, int id) throws BitbucketClientException
    {
        bitbucketClient.delete("/repositories/"+owner+"/"+slug+"/links/"+String.valueOf(id));
    }
    
    /**
     * Configures a new Repository Link to the bitbucket repository
     * 
     * @param owner
     * @param slug
     * @param name
     * @param url
     * @param key
     * @return
     * @throws BitbucketClientException
     */
    public RepositoryLink addRepositoryLink(String owner, String slug, String name, String url, String key) throws BitbucketClientException
    {
        Type type = new TypeToken<RepositoryLink>(){}.getType();
        String resourceUrl = "/repositories/"+owner+"/"+slug+"/links";

        List<String> params = Lists.newArrayList();
        params.add("handler=" + name);
        params.add("link_url=" + url);
        params.add("link_key=" + key);
        
        return bitbucketClient.post(resourceUrl, params, type);
    }
    

    public static void main(String[] args) throws BitbucketClientException
    {
        BitbucketClient bitbucketClient = new BitbucketClient("https://bitbucket.org/!api/1.0");
        bitbucketClient.setAuthorisation("dusanhornik", "macicka");
        RepositoryLinksService repositoryLinksService = new RepositoryLinksService(bitbucketClient);
        
        // list
        List<RepositoryLink> repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
        System.out.println(repositoryLinks);
        
        //add
        RepositoryLink link1 = repositoryLinksService.addRepositoryLink("dusanhornik", "jira-bitbucket-connector", "jira", "http://localhost:1234/jira", "BBC");
//        System.out.println(link1);
        
        //add
        RepositoryLink link2 = repositoryLinksService.addRepositoryLink("dusanhornik", "jira-bitbucket-connector", "jira", "http://localhost:1234/jira", "ABC");
//        System.out.println(link2);
        
        //list
        repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
        System.out.println(repositoryLinks);
        
        //remove
        repositoryLinksService.removeRepositoryLink("dusanhornik", "jira-bitbucket-connector", link1.getId());

        //list
        repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
        System.out.println(repositoryLinks);
        
        //remove
        repositoryLinksService.removeRepositoryLink("dusanhornik", "jira-bitbucket-connector", link2.getId());

        //list
        repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
        System.out.println(repositoryLinks);
        
//        long start = System.currentTimeMillis();
//        List<RepositoryLink> links = Lists.newArrayList();
//        for (int i = 0; i < 100; i++)
//        {
//            RepositoryLink link = repositoryLinksService.addRepositoryLink("dusanhornik", "jira-bitbucket-connector", "jira", "http://localhost:1234/jira", "ABC"+i);
//            links.add(link);
//        }
//        System.out.println((System.currentTimeMillis()-start)/1000);
//
//        //list
//        repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
//        System.out.println(repositoryLinks);
//
//        start = System.currentTimeMillis();        
//        for (RepositoryLink repositoryLink : links)
//        {
//            repositoryLinksService.removeRepositoryLink("dusanhornik", "jira-bitbucket-connector", repositoryLink.id);
//        }
//        System.out.println((System.currentTimeMillis()-start)/1000);
//        //list
//        repositoryLinks = repositoryLinksService.getRepositoryLinks("dusanhornik", "jira-bitbucket-connector");
//        System.out.println(repositoryLinks);

    }
}
