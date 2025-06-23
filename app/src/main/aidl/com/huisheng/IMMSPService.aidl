package com.huisheng;

interface IMMSPService {
    boolean start(String hostname,int port);
    boolean stop();
}