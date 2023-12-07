package com.qz.qqserver.service;

import com.qz.qqcommon.File_map;
import com.qz.qqcommon.Message;
import com.qz.qqcommon.MessageType;
import com.qz.qqcommon.User;
import com.qz.qqserver.utils.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 该类的一个对象和某个客户端保持通信
 */
public class ServerConnectClientThread extends Thread {

    private Socket socket;
    private String userId;// 连接到服务端的用户id

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() { // 这里线程处于run的状态，可以发送/接收消息
        // 读数据和写数据需要通过 socket
        while (true) {
            try {
                System.out.println("服务端和客户端" + userId + " 保持通信，读取数据...");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();
                // 后面会使用message, 根据message的类型，做相应的业务处理
                if (message.getMesType().equals(MessageType.MESSAGE_GET_ONLINE_FRIEND)) {
                    // 客户端要在线用户列表
                    /*
                        在线用户列表形式 100  200  紫霞仙子
                     */
                    System.out.println(message.getSender() + " 要在线用户列表");
                    String onlineUser = ManageClientThreads.getOnlineUser();
                    // 返回message
                    // 构建一个Message 对象，返回给客户端
                    Message message2 = new Message();
                    message2.setMesType(MessageType.MESSAGE_RET_ONLINE_FRIEND);
                    message2.setContent(onlineUser);
                    message2.setGetter(message.getSender());
                    //返回给客户端
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message2);

                }
                else if (message.getMesType().equals(MessageType.MESSAGE_COMM_MES)) {
                    //判断是否在线
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
                    if(hm.get(message.getGetter()) == null){
                        //不在线
                        ConcurrentHashMap<String, User> validUsers = QQServer.getValidUsers();
                        if(validUsers.get(message.getGetter()) == null){
                            //非法用户，进行提醒
                            System.out.println(message.getSender()+"向非法用户"+message.getGetter()+"发送离线消息");
                            Message illMsg = new Message();
                            illMsg.setGetter(message.getSender());
                            illMsg.setMesType(MessageType.MESSAGE_ILLUSER);
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            oos.writeObject(illMsg);
                        }else{
                            System.out.println(message.getSender()+"向用户"+message.getGetter()+"发送离线消息");
                            //合法，保存离线消息
                            List<Message> offLineMsg_list = new ArrayList<>();
                            message.setMesType(MessageType.MESSAGE_OFF_LINE);//变更发送消息类型为离线消息
                            offLineMsg_list.add(message);
                            QQServer.setOfflineMsg(message.getGetter(),offLineMsg_list);
                        }
                    }else{
                        //在线
                        //根据message获取getter id, 然后在得到对应先线程
                        ServerConnectClientThread serverConnectClientThread =
                                ManageClientThreads.getServerConnectClientThread(message.getGetter());
                        //得到对应socket的对象输出流，将message对象转发给指定的客户端
                        ObjectOutputStream oos =
                                new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                        oos.writeObject(message);//转发，提示如果客户不在线，可以保存到数据库，这样就可以实现离线留言
                    }


                }
                else if (message.getMesType().equals(MessageType.MESSAGE_TO_ALL_MES)) {
                    //需要遍历 管理线程的集合，把所有的线程的socket得到，然后把message进行转发即可
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();

                    Iterator<String> iterator = hm.keySet().iterator();
                    while (iterator.hasNext()) {

                        //取出在线用户id
                        String onLineUserId = iterator.next().toString();

                        if (!onLineUserId.equals(message.getSender())) {//排除群发消息的这个用户

                            //进行转发message
                            ObjectOutputStream oos =
                                    new ObjectOutputStream(hm.get(onLineUserId).getSocket().getOutputStream());
                            oos.writeObject(message);
                        }

                    }

                }
                else if (message.getMesType().equals(MessageType.MESSAGE_FILE_MES)) {
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
                    if(hm.get(message.getGetter()) == null){
                        //不在线
                        ConcurrentHashMap<String, User> validUsers = QQServer.getValidUsers();
                        if(validUsers.get(message.getGetter()) == null){
                            //非法用户，进行提醒
                            System.out.println(message.getSender()+"向非法用户"+message.getGetter()+"发送离线文件");
                            Message illMsg = new Message();
                            illMsg.setGetter(message.getSender());
                            illMsg.setMesType(MessageType.MESSAGE_ILLUSER);
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            oos.writeObject(illMsg);
                        }else{
                            System.out.println(message.getSender()+"向用户"+message.getGetter()+"发送离线文件");
                            //合法
                            //保存映射信息
                            File_map fm = new File_map();
                            fm.setRootPath("tempFiles");
                            fm.setOriName(message.getFileName());
                            fm.setDstPath(message.getDest());
                            fm.setGenName(Utility.getRandomName(message.getFileName()));
                            fm.setSender(message.getSender());
                            fm.setGetter(message.getGetter());
                            fm.setTime(message.getSendTime());
                            //保存离线文件到服务器
                            FileOutputStream fileOutputStream = new FileOutputStream(fm.getRootPath()+ File.separator+fm.getGenName(), true);
                            fileOutputStream.write(message.getFileBytes());
                            fileOutputStream.close();
                            System.out.println("\n 服务器存入离线文件");

                            List<File_map> file_maps = new ArrayList<>();
                            message.setMesType(MessageType.MESSAGE_OFFLINE_FILE);//变更发送消息类型为离线消息
                            file_maps.add(fm);
                            QQServer.setOffLineFile(message.getGetter(),file_maps);
                        }
                    }else{
                        //在线
                        //根据message获取getter id, 然后在得到对应线程
                        ServerConnectClientThread serverConnectClientThread =
                                ManageClientThreads.getServerConnectClientThread(message.getGetter());
                        //得到对应socket的对象输出流，将message对象转发给指定的客户端
                        ObjectOutputStream oos =
                                new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                        oos.writeObject(message);//转发，提示如果客户不在线，可以保存到数据库，这样就可以实现离线留言
                    }

                }
                else if (message.getMesType().equals(MessageType.MESSAGE_CLIENT_EXIT)) {//客户端退出

                    System.out.println(message.getSender() + " 退出");
                    //向客户端发送信息，表明用户已在服务端成功注销
                    Message exitMsg = new Message();
                    exitMsg.setMesType(MessageType.MESSAGE_SUCCESS_EXIT);
                    exitMsg.setGetter(message.getSender());
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(exitMsg);
                    //将这个客户端对应线程，从集合删除.
                    ManageClientThreads.removeServerConnectClientThread(message.getSender());
                    socket.close();//关闭连接
                    //退出线程
                    break;

                }
                else {
                    System.out.println("其他类型的message , 暂时不处理");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}