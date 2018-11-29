package com.android.mazhengyang.minichat.listener;

import android.util.Log;

import com.android.mazhengyang.minichat.util.Constant;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mazhengyang on 18-11-27.
 */

public abstract class UDPListener extends Listener {

    private static final String TAG = "MiniChat." + UDPListener.class.getSimpleName();

    protected boolean isOnline;
    //用于接收和发送数据的socket ，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket socket;
    private DatagramPacket packet;

    //TODO  由子类来赋初始值
    private int port = Constant.MESSAGE_PORT;
    private int default_bufferSize = 1024 * 2;
    private byte[] bufferData;//用来接收UDP发送的数据,考虑发送消息的类型来设置其大小

    private ExecutorService executorService;//用来发送消息

    /**
     * 建立连接前的一些初始化操作，端口，缓冲区大小
     */
    public abstract void init();

    /**
     * 通知上线
     */
    public abstract void noticeOnline();

    /**
     * 通知下线
     */
    public abstract void noticeOffline();

    /**
     * 端口有数据来时的回调方法
     */
    public abstract void handleReceivedMsg(byte[] data, DatagramPacket packet);

    /**
     * 发送消息失败
     */
    public abstract void sendMsg(String msg);

    @Override
    public void run() {
        while (isOnline) {
            try {
                if (socket != null) {
                    socket.receive(packet);//实时接收数据
                    if (packet.getLength() == 0) {
                        continue;
                    }
                    handleReceivedMsg(bufferData, packet);//处理接收的数据
                    //每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
                    packet.setLength(default_bufferSize);
                } else {
                    Log.d(TAG, "run: socket in null");
                }
            } catch (IOException e) {
                Log.e(TAG, "run: " + e);
            }
        }
    }

    /**
     * 设置绑定的端口号
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 设置缓冲区的大小
     *
     * @param bufferSize
     */
    public void setBufferSize(int bufferSize) {
        this.default_bufferSize = bufferSize;
    }

    /**
     * 发送UDP数据包
     *
     * @param msg      消息
     * @param destIp   目标地址
     * @param destPort 目标端口
     */
    protected void send(final String msg, final InetAddress destIp, final int destPort) {
        Log.d(TAG, "send: executorService=" + executorService);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: send");
                    DatagramPacket packet = new DatagramPacket(msg.getBytes(Constant.ENCOD),
                            msg.length(), destIp, destPort);
                    socket.send(packet);
                    if (!isOnline) {
                        Log.d(TAG, "run: close socket");
                        socket.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "run: " + e);
                    sendMsg("wifi unconnected");
                }
            }
        });
    }

    @Override
    public void open() {
        Log.d(TAG, "open: ");
        try {
            init();
            executorService = Executors.newFixedThreadPool(10);//用来发送消息
            socket = new MulticastSocket(port);
            bufferData = new byte[default_bufferSize];
            packet = new DatagramPacket(bufferData, bufferData.length);
            isOnline = true;
            setPriority(MAX_PRIORITY);
            this.start();
            noticeOnline();
        } catch (IOException e) {
            Log.e(TAG, "open: " + e);
        }
    }

    @Override
    public void close() {
        Log.d(TAG, "close: ");
        isOnline = false;
        interrupt();//如果在阻塞状态则打断;
        noticeOffline();
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

}
