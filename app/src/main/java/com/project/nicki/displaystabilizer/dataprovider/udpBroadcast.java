package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.project.nicki.displaystabilizer.init;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.project.nicki.displaystabilizer.dataprovider.SensorCollection.currentOrientationProvider;

/**
 * Created by nicki on 1/10/2017.
 */

public class udpBroadcast implements Runnable {

    // UDP广播IP和PORT
    //public static final String SERVERIP = "255.255.255.255";
    public static String SERVERIP;
    public static int SERVERPORT;
    DatagramSocket socket = null;
    DatagramSocket Rsocket = null;


    public udpBroadcast(Context baseContext, String ipportVal) {
        try {
            SERVERIP = ipportVal.split(":")[0];
            //SERVERIP = "127.0.0.1";
            SERVERPORT = Integer.parseInt(ipportVal.split(":")[1]);
        } catch (Exception ex) {

        }

    }

    @Override
    public void run() {
        // 向局域网UDP广播信息：Hello, World!
        try {
            System.out.println("Client: Start connecting\n");
            socket = new DatagramSocket(SERVERPORT);
            Rsocket = new DatagramSocket(11111);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InetAddress serverAddress = null;
                    try {
                        serverAddress = InetAddress.getByName(SERVERIP);


                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    while (true) {
                        //String toSend = "Hello, World! \n";
                        try {
                            String toSend =
                                    String.valueOf(System.currentTimeMillis()) + ":" +
                                            String.valueOf(init.initglobalvariable.HoverVal[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.HoverVal[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.AccelerometerLinearVal[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.AccelerometerLinearVal[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.AccelerometerLinearVal[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.MagnetometerVal[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.MagnetometerVal[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.MagnetometerVal[2] + ":");


                            String toSend2 =
                                    String.valueOf(System.currentTimeMillis()) + ":" +
                                            String.valueOf(init.initglobalvariable.sHoverVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sHoverVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[2]) + ":" +
                                            "0" + ":" +
                                            "0" + ":" +
                                            "0" + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[3]);


                            String toSend3 =
                                    String.valueOf(System.currentTimeMillis()) + ":" +
                                            String.valueOf(init.initglobalvariable.sHoverVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sHoverVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[0] + ":" +
                                                    String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[1]) + ":" +
                                                    String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[2]) + ":" +
                                                    String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[3]) + ":" +
                                                    String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[0]) + ":" +
                                                    String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[1]) + ":" +
                                                    String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[2])) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[2]) + ":" +
                                            //  String.valueOf(init.initStabilize_v4.mstabilize_v3_func.prevStroke[0]) + ":" +
                                            //  String.valueOf(init.initStabilize_v4.mstabilize_v3_func.prevStroke[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sStaticVal.getLatestData().getValues()[0]);

                            String toSend4 =
                                    String.valueOf(System.currentTimeMillis()) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sQuaternionVal.getLatestData().getValues()[3]) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.mVelocity.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.mPosotion.getLatestData().getValues()[2])+":"+
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerVal.getLatestData().getValues()[2]) + ":" +
                                    String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[0]) + ":" +
                                    String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[1]) + ":" +
                                    String.valueOf(init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[0]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[1]) + ":" +
                                            String.valueOf(init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[2]) + ":" +
                                            String.valueOf(init.initglobalvariable.sStaticVal.getLatestData().getValues()[0]);
                            byte[] buf = toSend4.getBytes("UTF-8");
                            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                                    serverAddress, SERVERPORT);
                            //Log.d("Client: Sending ‘", SERVERIP + ":" + SERVERPORT + "　" + new String(buf) + "’\n");

                            socket.send(packet);
                            Thread.sleep(50);
                        } catch (Exception ex) {
                        }

                        /*
                        try {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                */
                                    byte[] recbuf = new byte[255];
                                    DatagramPacket recpacket = new DatagramPacket(recbuf,
                                            recbuf.length);
                                    try {
                                        Rsocket.setSoTimeout(10);
                                        Rsocket.receive(recpacket);


                                        byte[] data = new byte[recpacket.getLength()];
                                        System.arraycopy(recpacket.getData(), recpacket.getOffset(), data, 0, recpacket.getLength());

                                        //String packetAsString=new String(recbuf, 0, recpacket.getLength());
                                        String packetAAsString = new String(data, "UTF-8");



                                        try {
                                            String[] tokens = packetAAsString.split("/");
                                            for (String token : tokens) {
                                                String[] params = token.split(":");
                                                if (params[0].equals("sQuaternionVal")) {
                                                    init.initglobalvariable.sQuaternionVal.setFilterParam(params);
                                                }
                                                if (params[0].equals("sAccelerometerLinearVal")) {
                                                    init.initglobalvariable.sAccelerometerLinearVal.setFilterParam(params);
                                                }
                                                if (params[0].equals("sAccelerometerVal")) {
                                                    init.initglobalvariable.sAccelerometerVal.setFilterParam(params);
                                                }
                                                if (params[0].equals("sAccelerometerVal_world")) {
                                                    init.initglobalvariable.sAccelerometerVal_world.setFilterParam(params);
                                                }
                                                if (params[0].equals("sAccelerometerLinearVal_world")) {
                                                    init.initglobalvariable.sAccelerometerLinearVal_world.setFilterParam(params);
                                                }
                                                if (params[0].equals("RK4_Velocity_world")) {
                                                    init.initglobalvariable.mVelocity.setFilterParam(params);
                                                }
                                                if (params[0].equals("RK4_Position_world")) {
                                                    init.initglobalvariable.mPosotion.setFilterParam(params);
                                                }
                                                if (params[0].equals("re")) {
                                                    init.initglobalvariable.mPosotion.resetbuffer();
                                                    init.initglobalvariable.mVelocity.resetbuffer();

                                                }
                                            }
                                        }catch (Exception ex){
                                            Log.e("udp",String.valueOf(ex));
                                        }



                                        Log.d("Recievedudp", "Server: Message received: ‘" + new String(recpacket.getData()) + "’\n" + "   " + "Server: IP " + recpacket.getAddress() + "’\n");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }  /*
                                }
                            }).start();
                        } catch (Exception ex) {

                        }
`                       */
                    }

                }
            }).start();



            /*
            // 接收UDP广播，有的手机不支持
            while (true) {
                Log.d("hi", "ETST");
                byte[] recbuf = new byte[255];
                DatagramPacket recpacket = new DatagramPacket(recbuf,
                        recbuf.length);
                try {
                    socket.receive(recpacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("TAG", String.valueOf("Server: Message received: ‘"
                        + new String(recpacket.getData()) + "’\n" + "   " + "Server: IP " + recpacket.getAddress()
                        + "’\n"));
                System.out.println("Server: Message received: ‘"
                        + new String(recpacket.getData()) + "’\n");
                System.out.println("Server: IP " + recpacket.getAddress()
                        + "’\n");
            }
            */
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
