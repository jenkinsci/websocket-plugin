package org.codefirst.jenkins.wsnotifier;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.json.JSONObject;

import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;

public class WsServer implements WebSocketHandler {
    private static WebServer webServer = null;
    private static CopyOnWriteArrayList<WebSocketConnection> connections =
        new CopyOnWriteArrayList<WebSocketConnection>();

    private static PingTimerThread pingTimer;
    private static boolean useStatusFormat;
    
    @Initializer(before=InitMilestone.COMPLETED)
    public static void init() {
        WsNotifier.DescriptorImpl desc =
            Hudson.getInstance().getDescriptorByType(WsNotifier.DescriptorImpl.class);
        if(desc != null) {
            reset(desc.port(), desc.keepalive() ? desc.pingInterval() : -1, desc.useStatusFormat());
        }else{
            reset(8081, 20, false);
        }
    }

    synchronized public static void reset(int port, int pingInterval, boolean useStatusFormat) {
        System.out.println("stopping web server");
        if(webServer != null){
            for(WebSocketConnection con : connections){
                con.close();
            }
            connections.clear();
            webServer.stop();
        }
        if(pingTimer != null) pingTimer.terminate();
        WsServer.useStatusFormat = useStatusFormat;
        System.out.println("start websocket server at " + port);
        webServer = WebServers.createWebServer(port)
            .add("/jenkins", new WsServer());
        webServer.start();
        if (pingInterval > 0) pingTimer = new PingTimerThread(pingInterval);
    }

    static public void send(AbstractBuild build){
        send(build, null);
    }

    static public void send(AbstractBuild build, String result){
        String statusName = useStatusFormat ? "status" : "result";
        if (result == null) {
            if (!useStatusFormat) return;
            result = build.getResult().toString();
        }
        String json = new JSONObject()
            .element("project", build.getProject().getName())
            .element("number" , new Integer(build.getNumber()))
            .element(statusName , result)
            .toString();

        for(WebSocketConnection con : connections){
            con.send(json);
        }
        
        // it's not necessary to send out a ping immediately or shortly after having send a client message.
        // reset the ping timer to wait its full interval again after sending
        if (pingTimer != null) pingTimer.interrupt();
    }
    
    static protected void ping(){
    	for (WebSocketConnection con : connections) {
    		con.ping("ping".getBytes());
    	}
    }

    public void onOpen(WebSocketConnection connection) {
        connections.add(connection);
    }

    public void onClose(WebSocketConnection connection) {
        connections.remove(connection);
    }

    public void onMessage(WebSocketConnection connection, String message) {
    }

    public void onMessage(WebSocketConnection connection, byte[] message) {
    }

    public void onPing(WebSocketConnection connection, byte[] message) throws Throwable {
    	connection.pong(message);
    }

    public void onPong(WebSocketConnection connection, byte[] message) throws Throwable {
    }
}
