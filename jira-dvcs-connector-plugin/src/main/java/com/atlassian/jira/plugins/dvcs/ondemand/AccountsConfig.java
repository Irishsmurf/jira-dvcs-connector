package com.atlassian.jira.plugins.dvcs.ondemand;

import java.util.List;

/**
 * <pre>
 * {
 *   "sysadmin-application-links": [
 *       {
 *           "bitbucket": [
 *               {
 *                   "account": "mybucketbit",
 *                   "key": "verysecretkey",
 *                   "secret": "verysecretsecret"
 *               }
 *           ]
 *       }
 *   ]
 *  }
 * </pre>
 *
 */
public class AccountsConfig
{
    private List<Links> sysadminApplicationLinks;
    
    public AccountsConfig()
    {
        super();
    }

    public List<Links> getSysadminApplicationLinks()
    {
        return sysadminApplicationLinks;
    }

    public void setSysadminApplicationLinks(List<Links> sysadminApplicationLinks)
    {
        this.sysadminApplicationLinks = sysadminApplicationLinks;
    }
    
    public BitbucketAccountInfo getFirstBitbucketAccountConfig() {
        
        return sysadminApplicationLinks.get(0).getBitbucket().get(0);
        
    }
    
    
    public static class Links {
        
        private List<BitbucketAccountInfo> bitbucket;
        
        public Links()
        {
            super();
        }

        public List<BitbucketAccountInfo> getBitbucket()
        {
            return bitbucket;
        }

        public void setBitbucket(List<BitbucketAccountInfo> bitbucket)
        {
            this.bitbucket = bitbucket;
        }
        
    }
    
    public static class BitbucketAccountInfo {
        
        private String account;

        private String key;

        private String secret;
        
        public BitbucketAccountInfo()
        {
            super();
        }

        public String getAccount()
        {
            return account;
        }

        public void setAccount(String account)
        {
            this.account = account;
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getSecret()
        {
            return secret;
        }

        public void setSecret(String secret)
        {
            this.secret = secret;
        }
        
    }

}

