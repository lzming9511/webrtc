package com.lzming.webrtc.Controller;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONObject;
import org.json.JSONArray;

@ServerEndpoint("/webrtc")
@Component
public class WebrtcController {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static CopyOnWriteArraySet<WebrtcController> webSocketSet = new CopyOnWriteArraySet<WebrtcController>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session){
        this.session = session;
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        try {
            this.sendMessageToSession(session,"{\"event\":\"_new_user\",\"userID\":\""+session.getId()+"\"}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(){
        String thisID=session.getId();
        String thisRoomID=session.getQueryString();
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());

        //群发消息
        for(WebrtcController item: webSocketSet){


            String itemRoomID=item.session.getQueryString();
            if (!thisRoomID.equals(itemRoomID)){
                continue;
            }

            try {
                item.sendMessage("{\"event\":\"_user_leave\",\"leaveUserId\":\""+session.getId()+"\"}");
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        String thisID=session.getId();
        String thisRoomID=session.getQueryString();
        System.out.println("rev服务端收到 客服端" +thisID+" 消息:" + message);
        JSONObject jsonObject=new JSONObject(message);

        try {
            String event=jsonObject.getString("event");
            if (event.equals("_room_users")){
                jsonObject=new JSONObject();
                jsonObject.put("event","_server_room_user");

                List<String> userIDs=new ArrayList<>();
                for(WebrtcController item: webSocketSet){
                    String theItemRoomID=item.session.getQueryString();
                    if (!thisRoomID.equals(theItemRoomID)){
                        continue;
                    }

                    userIDs.add(item.session.getId());
                }
                jsonObject.put("data",new JSONArray(userIDs));
                System.out.println("send服务器发送给 客服端"+session.getId()+" 消息:" + message);
                this.sendMessageToSession(session,jsonObject.toString());
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        //群发消息
        for(WebrtcController item: webSocketSet){
            if (thisID==item.session.getId()){
                continue;
            }
            String responID=jsonObject.getString("respon");
            if (!responID.equals(item.session.getId())){
                continue;
            }

            String itemRoomID=item.session.getQueryString();
            if (!thisRoomID.equals(itemRoomID)){
                continue;
            }

            try {
                System.out.println("send服务器发送给 客服端"+item.session.getId()+" 消息:" + message);
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error){
        System.out.println("发生错误");
        error.printStackTrace();

        String thisRoomID=session.getQueryString();
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());

        //群发消息
        for(WebrtcController item: webSocketSet){
            String itemRoomID=item.session.getQueryString();
            if (!thisRoomID.equals(itemRoomID)){
                continue;
            }
            try {
                item.sendMessage("{\"event\":\"_user_leave\",\"leaveUserId\":\""+session.getId()+"\"}");
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * @param message
     * @throws IOException
     */
    public synchronized void sendMessage(String message) throws IOException{
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }

    public synchronized void sendMessageToSession(Session session,String message) throws IOException{
        session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }


    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebrtcController.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebrtcController.onlineCount--;
    }
}
