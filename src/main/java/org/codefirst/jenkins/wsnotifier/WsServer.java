package org.codefirst.jenkins.wsnotifier;

import hudson.init.Initializer;
import hudson.init.InitMilestone;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import java.io.IOException;
import org.webbitserver.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.sf.json.JSONObject;
import hudson.model.*;

public class WsServer implements WebSocketHandler {
    private static WebServer webServer = null;
    private static CopyOnWriteArrayList<WebSocketConnection> connections =
        new CopyOnWriteArrayList<WebSocketConnection>();

    @Initializer(before=InitMilestone.COMPLETED)
    public static void init() {
        WsNotifier.DescriptorImpl desc =
            Hudson.getInstance().getDescriptorByType(WsNotifier.DescriptorImpl.class);
        if(desc != null) {
            reset(desc.port());
        }else{
            reset(8081);
        }
    }

    synchronized public static void reset(int port) {
        try {
            System.out.println("stopping web server");
            if(webServer != null){
                for(WebSocketConnection con : connections){
                    con.close();
                }
                connections.clear();
                webServer.stop();
            }
            System.out.println("start websocket server at " + port);
            webServer = WebServers.createWebServer(port)
                .add("/jenkins", new WsServer())
                .start();
        } catch( IOException ex){
            throw new RuntimeException(ex);
        }
    }
    static public void send(AbstractBuild build){
        String json = new JSONObject()
            .element("project", build.getProject().getName())
            .element("number" , new Integer(build.getNumber()))
            .element("result" , build.getResult().toString())
            .toString();

        for(WebSocketConnection con : connections){
            con.send(json);
        }
    }

    public void onOpen(WebSocketConnection connection) {
        System.out.println("on open");
        connections.add(connection);
    }

    public void onClose(WebSocketConnection connection) {
        System.out.println("on close");
        connections.remove(connection);

    }

    public void onPong(WebSocketConnection connection, String message) {
    }


    public void onMessage(WebSocketConnection connection, String message) {
    }

    public void onMessage(WebSocketConnection connection, byte[] message) {
    }
}
