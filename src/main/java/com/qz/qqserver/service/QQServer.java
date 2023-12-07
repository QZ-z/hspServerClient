package com.qz.qqserver.service;

import com.qz.qqcommon.File_map;
import com.qz.qqcommon.Message;
import com.qz.qqcommon.MessageType;
import com.qz.qqcommon.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这是服务器, 在监听9999，等待客户端的连接，并保持通信
 */
public class QQServer {

    private ServerSocket ss = null;
    //创建一个集合，存放多个用户，如果是这些用户登录，就认为是合法
    //这里我们也可以使用 ConcurrentHashMap, 可以处理并发的集合，没有线程安全
    //HashMap 没有处理线程安全，因此在多线程情况下是不安全
    //ConcurrentHashMap 处理的线程安全,即线程同步处理, 在多线程情况下是安全
    private static ConcurrentHashMap<String, User> validUsers = new ConcurrentHashMap<>();
    //private static ConcurrentHashMap<String, ArrayList<Message>> offLineDb = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, User> getValidUsers(){return validUsers;}

    private static ConcurrentHashMap<String, List<Message>> offlineMsg = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, List<Message>> getOfflineMsg(){ return offlineMsg;}

    public static void setOfflineMsg(String userId, List<Message> msg_list){
        if(offlineMsg.get(userId) == null){
            //没有未处理的离线消息
            offlineMsg.put(userId,msg_list);
        }else{
            //有未处理的离线消息
            List<Message> old = offlineMsg.get(userId);
            old.addAll(msg_list);
        }
    }

    //离线文件集合，暂存到服务器中，保存在服务器中的文件路径和原文件名，格式：服务器路径，原文件名
    private static ConcurrentHashMap<String, List<File_map>> offLineFile = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, List<File_map>> getOffLineFile(){return offLineFile;}

    public static void setOffLineFile(String userId, List<File_map> offLineFile_map){
        if(offLineFile.get(userId) == null){
            offLineFile.put(userId,offLineFile_map);
        }else {
            List<File_map> old = offLineFile.get(userId);
            old.addAll(offLineFile_map);
        }
    }



    static { //在静态代码块，初始化 validUsers

        validUsers.put("100", new User("100", "123456"));
        validUsers.put("200", new User("200", "123456"));
        validUsers.put("300", new User("300", "123456"));
        validUsers.put("至尊宝", new User("至尊宝", "123456"));
        validUsers.put("紫霞仙子", new User("紫霞仙子", "123456"));
        validUsers.put("菩提老祖", new User("菩提老祖", "123456"));

    }

    //验证用户是否有效的方法
    private boolean checkUser(String userId, String passwd) {

        User user = validUsers.get(userId);
        //过关的验证方式
        if(user == null) {//说明userId没有存在validUsers 的key中
            return  false;
        }
        if(!user.getPasswd().equals(passwd)) {//userId正确，但是密码错误
            return false;
        }
        return  true;
    }

    public QQServer() {
        //注意：端口可以写在配置文件.
        try {
            System.out.println("服务端在9999端口监听...");
            //启动推送新闻的线程
            new Thread(new SendNewsToAllService()).start();
            ss = new ServerSocket(9999);

            while (true) { //当和某个客户端连接后，会继续监听, 因此while
                Socket socket = ss.accept(); // 如果没有客户端连接，就会阻塞在这里
                //得到socket关联的对象输入流
                ObjectInputStream ois =
                        new ObjectInputStream(socket.getInputStream());

                //得到socket关联的对象输出流
                ObjectOutputStream oos =
                        new ObjectOutputStream(socket.getOutputStream());
                User u = (User) ois.readObject();//读取客户端发送的User对象
                //创建一个Message对象，准备回复客户端
                Message message = new Message();
                //验证用户 方法
                boolean judge1 = checkUser(u.getUserId(), u.getPasswd());//验证账号密码是否正确
                boolean judge2 = ManageClientThreads.getServerConnectClientThread(u.getUserId()) == null;//是否已经登录
                if (judge1 && judge2) {//登录通过
                    message.setMesType(MessageType.MESSAGE_LOGIN_SUCCEED);
                    //将message对象回复客户端
                    oos.writeObject(message);

                    //查看是否有该用户的离线消息，如果有，通过message推送
                    if(offlineMsg.get(u.getUserId()) != null){
                        List<Message> offLineMsgs = offlineMsg.get(u.getUserId());
                        for(Message msg : offLineMsgs){
                            oos = new ObjectOutputStream(socket.getOutputStream());

                            oos.writeObject(msg);
                        }
                        //传完了在删除离线消息
                        offlineMsg.remove(u.getUserId());
                        System.out.println(u.getUserId()+"上线服务器转发完所有离线消息");
                    }

                    //查看是否有该用户的离线文件，如果有，推送
                    if(offLineFile.get(u.getUserId()) != null){
                        List<File_map> offLineFiles = offLineFile.get(u.getUserId());
                        for(File_map fm:offLineFiles){
                            FileInputStream fileInputStream = null;
                            File now_file = new File(fm.getRootPath()+File.separator+fm.getGenName());
                            byte[] fileBytes = new byte[(int)now_file.length()];
                            fileInputStream = new FileInputStream(fm.getRootPath()+File.separator+fm.getGenName());
                            fileInputStream.read(fileBytes);//读入服务器中的离线文件

                            Message offFileMsg = new Message();
                            //将文件对应的字节数组设置message
                            offFileMsg.setFileBytes(fileBytes);
                            offFileMsg.setMesType(MessageType.MESSAGE_OFFLINE_FILE);
                            offFileMsg.setSender(fm.getSender());
                            offFileMsg.setGetter(fm.getGetter());
                            offFileMsg.setFileName(fm.getOriName());
                            offFileMsg.setSendTime(fm.getTime());
                            offFileMsg.setDest(fm.getDstPath());


                            oos = new ObjectOutputStream(socket.getOutputStream());
                            oos.writeObject(offFileMsg);

                            fileInputStream.close();
                            //删除服务器端文件
                            boolean delete = now_file.delete();
                            if(delete){
                                System.out.println("服务器端临时离线文件删除成功");
                            }else{
                                System.out.println("服务器端临时离线文件删除失败");
                            }
                        }
                        //删除映射信息
                        offLineFile.remove(u.getUserId());
                        System.out.println("服务器转发完用户"+u.getUserId()+"的所有的离线文件");

                    }

                    //创建一个线程，和客户端保持通信, 该线程需要持有socket对象
                    ServerConnectClientThread serverConnectClientThread =
                            new ServerConnectClientThread(socket, u.getUserId());
                    //启动该线程
                    serverConnectClientThread.start();
                    //把该线程对象，放入到一个集合中，进行管理.
                    ManageClientThreads.addClientThread(u.getUserId(), serverConnectClientThread);

                } else if(!judge1){ // 登录失败
                    System.out.println("用户 id=" + u.getUserId() + " pwd=" + u.getPasswd() + " 验证失败");
                    message.setMesType(MessageType.MESSAGE_LOGIN_FAIL);
                    oos.writeObject(message);
                    //关闭socket
                    socket.close();
                }else{
                    System.out.println(("用户 id=" + u.getUserId() + "已经登录"));
                    message.setMesType(MessageType.MESSAGE_LOGIN_AGAIN);
                    oos.writeObject(message);
                    socket.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            //如果服务器退出了while，说明服务器端不在监听，因此关闭ServerSocket
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}