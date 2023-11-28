package com.qz.qqserver.qqframe;

import com.qz.qqserver.service.QQServer;

/**
 * 该类创建QQServer ,启动后台的服务
 */
public class QQFrame {
    public static void main(String[] args)  {
        new QQServer();
    }
}