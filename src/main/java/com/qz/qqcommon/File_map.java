package com.qz.qqcommon;

import java.util.Objects;

/**
 * 服务器端存储的离线文件原名和本名之间的映射
 */
public class File_map {
    private String rootPath;//在服务器下的根路径
    private String dstPath;//保存的目标路径
    private String oriName;//原名
    private String genName;//生成的名字
    private String sender;//发送方
    private String getter;//接收方
    private String time;//发送时间

    public File_map() {
    }

    public String getDstPath() {
        return dstPath;
    }


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setDstPath(String dstPath) {
        this.dstPath = dstPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getOriName() {
        return oriName;
    }

    public void setOriName(String oriName) {
        this.oriName = oriName;
    }

    public String getGenName() {
        return genName;
    }

    public void setGenName(String genName) {
        this.genName = genName;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getGetter() {
        return getter;
    }

    public void setGetter(String getter) {
        this.getter = getter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File_map fileMap = (File_map) o;
        return Objects.equals(rootPath, fileMap.rootPath) && Objects.equals(dstPath, fileMap.dstPath) && Objects.equals(oriName, fileMap.oriName) && Objects.equals(genName, fileMap.genName) && Objects.equals(sender, fileMap.sender) && Objects.equals(getter, fileMap.getter) && Objects.equals(time, fileMap.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootPath, dstPath, oriName, genName, sender, getter, time);
    }

    @Override
    public String toString() {
        return "File_map{" +
                "rootPath='" + rootPath + '\'' +
                ", dstPath='" + dstPath + '\'' +
                ", oriName='" + oriName + '\'' +
                ", genName='" + genName + '\'' +
                ", sender='" + sender + '\'' +
                ", getter='" + getter + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
