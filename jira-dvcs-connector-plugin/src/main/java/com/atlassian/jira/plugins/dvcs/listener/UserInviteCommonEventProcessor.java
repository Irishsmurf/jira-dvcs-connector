package com.atlassian.jira.plugins.dvcs.listener;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;

public class UserInviteCommonEventProcessor
{

    private static Logger log = LoggerFactory.getLogger(UserInviteCommonEventProcessor.class);
    
    protected final GroupManager groupManager;

    protected final UserManager userManager;

    public UserInviteCommonEventProcessor(UserManager userManager, GroupManager groupManager)
    {
        super();
        this.userManager = userManager;
        this.groupManager = groupManager;
    }
    
    public void logInvite(String username, Collection<? extends Object> bitbucketGroups) {
        User user = userManager.getUser(username);
        logInvite(user, bitbucketGroups);
    }

    public void logInvite(User user,  Collection<? extends Object> bitbucketGroups) {
       
       if (log.isDebugEnabled()) {
       
           log.debug(" \n\t Inviting user " + user.getName() + 
                    ", \n\t is active = " + user.isActive() + 
                    ", \n\t member of groups " + groupManager.getGroupNamesForUser(user) + 
                    ", \n\t to Bitbucket groups " + bitbucketGroups);
           
       }
    }
}
