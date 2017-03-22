/**
 * Copyright (C) 2014 Red Hat, Inc, and individual contributors.
 * Copyright (C) 2011-2012 VMware, Inc.
 */

package angular4J.sockjs.servlet;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import angular4J.sockjs.SockJsRequest;
import angular4J.sockjs.SockJsServer;
import angular4J.sockjs.Transport;

public class SockJsEndpoint extends Endpoint {

   private SockJsServer server;
   private String contextPath;
   private String prefix;

   private static final Logger log = Logger.getLogger(SockJsEndpoint.class.getName());

   public SockJsEndpoint(SockJsServer server, String contextPath, String prefix) {
      this.server = server;
      this.contextPath = contextPath;
      this.prefix = prefix;
   }

   @Override
   public void onOpen(Session session, EndpointConfig config) {
      log.log(Level.FINER, "onOpen");

      String sessionId = session.getPathParameters().get("session");
      Map<String, List<String>> headers = SockJsServlet.retrieveHeaders(sessionId);
      SockJsRequest req = new SockJsWebsocketRequest(session, contextPath, prefix, headers);
      Transport.registerNoSession(req, server, new WebsocketReceiver(session));
   }

   @Override
   public void onClose(Session session, CloseReason closeReason) {
      log.log(Level.FINER, "onClose {0}", closeReason);
   }

   @Override
   public void onError(Session session, Throwable thr) {
      log.log(Level.FINE, "Error in SockJS WebSocket endpoint", thr);
   }
}
