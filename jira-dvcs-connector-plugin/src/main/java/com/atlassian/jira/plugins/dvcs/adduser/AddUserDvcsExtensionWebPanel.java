package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.templaterenderer.TemplateRenderer;

public class AddUserDvcsExtensionWebPanel implements WebPanel
{

	private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

	private final TemplateRenderer templateRenderer;

	private final OrganizationService organizationService;

	private final DvcsCommunicatorProvider communicatorProvider;

	public AddUserDvcsExtensionWebPanel(TemplateRenderer templateRenderer, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider)
	{
		this.templateRenderer = templateRenderer;
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
	}

	@Override
	public String getHtml(Map<String, Object> model)
	{

		StringWriter stringWriter = new StringWriter();

		try
		{

			addBitbucketOrganizations(model);
			templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);

		} catch (Exception e)
		{
			log.warn("Error while rendering DVCS extension fragment for add user form.", e);
			stringWriter = new StringWriter(); // reset writer so no broken
												// output goes out
		}

		return stringWriter.toString();
	}

	private List<Organization> addBitbucketOrganizations(Map<String, Object> model)
	{

		List<Organization> all = organizationService.getAll(false, BitbucketCommunicator.BITBUCKET);
		DvcsCommunicator communicator = communicatorProvider.getCommunicator(BitbucketCommunicator.BITBUCKET);
		for (Organization organization : all)
		{
			List<Group> groups = communicator.getGroupsForOrganization(organization);
			organization.setGroups(groups);
		}
		model.put("bbOrgaizations", all);
		return all;

	}

	@Override
	public void writeHtml(Writer writer, Map<String, Object> model) throws IOException
	{

	}

}
