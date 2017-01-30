package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by nicki on 1/29/2017.
 */

public class udpRecieve implements Runnable {
    DatagramSocket Rsocket = null;

    @Override
    public void run() {
        try {
            Rsocket = new DatagramSocket(11000);
        } catch (SocketException e) {
            e.printStackTrace();
        }



        while (true) {
            byte[] recbuf = new byte[255];
            DatagramPacket recpacket = new DatagramPacket(recbuf,
                    recbuf.length);

            try {

                Rsocket.setSoTimeout(1000);
                Rsocket.receive(recpacket);
                recbuf = recpacket.getData();

              //  byte[] data = new byte[recpacket.getLength()];
              //  System.arraycopy(recpacket.getData(), recpacket.getOffset(), data, 0, recpacket.getLength());

                //String packetAsString=new String(recbuf, 0, recpacket.getLength());
               // String packetAsString=new String(data);

                //String[] tokens = packetAsString.split(":");
                Log.d("recievedata",new String(recbuf));
                Log.d("Recievedudp", String.valueOf("Server: Message received: ‘"
                        + new String(recpacket.getData()) + "’\n" + "   " + "Server: IP " + recpacket.getAddress() + "’\n"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Log.d("Recievedudp", String.valueOf("Server: Message received: ‘"
            //        + new String(recpacket.getData()) + "’\n" + "   " + "Server: IP " + recpacket.getAddress() + "’\n"));
        }


    }
}
