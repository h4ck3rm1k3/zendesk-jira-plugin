package org.agilos.jira.zendesk;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.restlet.resource.ResourceException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;

public class ChangeMessageBuilder {
	private static Logger log = Logger.getLogger(ChangeMessageBuilder.class.getName());	

	private boolean publicComments = true;

	/**
	 * Generates a REST representation of the issue change
	 * @param issueEvent
	 * @return The REST representation of the issue change if any relevant changes are found, else null;
	 * @throws ParserConfigurationException 
	 * @throws ResourceException
	 * @throws IOException
	 * @throws NoSuchFieldException Throw in case of a changeEvent state, which the ChangeMessageBuilder is unable to handle.  
	 */
	public Document createChangeRepresentation(IssueEvent changeEvent) throws IOException, NoSuchFieldException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			log.error(e);
			return null;
		}
		DOMImplementation impl = builder.getDOMImplementation();

		Document document = impl.createDocument(null,null,null);

		Node commentRoot = document;

		if (changeEvent.getEventTypeId() == EventType.ISSUE_UPDATED_ID ||
				changeEvent.getEventTypeId() == EventType.ISSUE_MOVED_ID) {
			Element ticket = document.createElement("ticket");
			document.appendChild(ticket);

			if (changeEvent.getIssue().getSummary() != null) {
				Element subject = document.createElement("subject");
				subject.setTextContent(changeEvent.getIssue().getSummary());
				ticket.appendChild(subject);
			}

			if (changeEvent.getIssue().getDescription() != null) {
				Element description = document.createElement("description");
				description.setTextContent(changeEvent.getIssue().getDescription());
				ticket.appendChild(description);
			}

			if (changeEvent.getEventTypeId() == EventType.ISSUE_MOVED_ID) {
				Element newIssueKey = document.createElement("external-id");
				newIssueKey.setTextContent(changeEvent.getIssue().getKey());
				ticket.appendChild(newIssueKey);
			}

			if (changeEvent.getComment() != null) {
				Element comments = document.createElement("comments");
				comments.setAttribute("type", "array");
				ticket.appendChild(comments);
				commentRoot = comments;
			}
			// If no relevant changes have been added to the ticket root node, exit.
			if (ticket.getChildNodes().getLength() == 0 ) return null;
		}

		else if (changeEvent.getEventTypeId() == EventType.ISSUE_COMMENTED_ID) {
			commentRoot.appendChild(createComment(document, changeEvent));
		} 
		
		else if (changeEvent.getIssue().getStatusObject() != null &&
				 !changeEvent.getEventTypeId().equals(EventType.ISSUE_CREATED_ID)) { // Disregard newly created issues, this is not considered a status change
			commentRoot.appendChild(createStatusChange(document, changeEvent));
		}	
		
		else {
			//The logging done here, instead of at the exception handling to avoid composing the log message when debug logging is disabled.
			if (log.isDebugEnabled()) {
				StringBuffer logMessage = new StringBuffer();
				logMessage.append("No notification handle defined for event with ID "+changeEvent.getEventTypeId());
				if (changeEvent.getIssue().getStatusObject() != null) {
					logMessage.append(" and status"+changeEvent.getIssue().getStatusObject().getName()); 
				}
				log.debug(logMessage.toString());
			}
			throw new NoSuchFieldException();
		}
		return document;
	}
	

	public void setPublicComments(boolean publicComments) {
		this.publicComments = publicComments;
	}
	
	private Element createComment(Document document, IssueEvent changeEvent) {
		Element comment = document.createElement("comment");

		Element isPublic = document.createElement("is-public");
		if (publicComments) {
			isPublic.setTextContent("true");
		} else {
			isPublic.setTextContent("false");
		}
		comment.appendChild(isPublic);

		Element value = document.createElement("value");
		String commentText = changeEvent.getRemoteUser().getFullName()+" added a comment\n"+
							 changeEvent.getComment().getBody();
		value.setTextContent(commentText);
		comment.appendChild(value);   
		
		return comment;
	}
	
	private Element createStatusChange(Document document, IssueEvent changeEvent) {
		Element comment = document.createElement("comment");

		Element isPublic = document.createElement("is-public");
		isPublic.setTextContent("true");
		comment.appendChild(isPublic);
 
		Element value = document.createElement("value");
		String commentText = changeEvent.getRemoteUser().getFullName()+" changed status to "+changeEvent.getIssue().getStatusObject().getName();
		value.setTextContent(commentText);
		comment.appendChild(value);
		
		return comment;
	}
}
