package com.atlassian.jira.plugins.bitbucket.links;

import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.project.Project;

import com.opensymphony.user.User;

public class ProjectSettings extends AbstractPluggableProjectOperation{

   public String getHtml(final Project project, final User user){

       String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");

       return "<strong>Bitbucket Connector: </strong> (<a href='" + baseURL + "/secure/admin/ConfigureBitbucketRepositories.jspa?projectKey=" + project.getKey() + "&mode=single'>Manage Repositories</a>)";

   }

   public boolean showOperation(final Project project, final User user)
   {
       return true;
   }
}
