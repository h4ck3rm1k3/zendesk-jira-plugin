package org.agilos.zendesk_jira_plugin.integrationtest.notifications;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Request;
import org.restlet.routing.Router;
import org.restlet.security.Guard;

public class NotificationListener extends Application {
	public static final LinkedBlockingQueue<Request> messageQueue = new LinkedBlockingQueue<Request>();
	private Logger log = Logger.getLogger(NotificationListener.class.getName());
	
	public NotificationListener() {
		createRoot();
	}
	
	@Override
    public synchronized Restlet createRoot() {
        // Create a router Restlet that routes each call to a
        // new instance of the NotificationHandler.
		
        Router router = new Router(getContext());

        // Defines only one route
        router.attachDefault(NotificationHandler.class);
        
        Guard guard = new Guard(getContext(), ChallengeScheme.HTTP_BASIC, "Tutorial");
		guard.getSecrets().put("mikis", "Zentril.2".toCharArray());		
		guard.setNext(router);
        
        return guard;
    }
	
	public String getNextRequest() {
		String response = null;
		try {
			response = messageQueue.poll(10l, TimeUnit.SECONDS).toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return response; 
	}
	

	private void setAuthetication(String user, String password) {
		// Create a Guard
		Guard guard = new Guard(getContext(), ChallengeScheme.HTTP_BASIC, "Tutorial");
		guard.getSecrets().put(user, password.toCharArray());
		
		guard.setNext(this);
	}
}
