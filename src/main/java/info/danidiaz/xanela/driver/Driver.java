package info.danidiaz.xanela.driver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.SwingUtilities;

import org.msgpack.MessagePack;
import org.msgpack.MessagePackable;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.MessagePackUnpacker;
import org.msgpack.unpacker.Unpacker;

public class Driver implements Runnable
{
    
    // http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml
    private final static int DEFAULT_PORT = 26060;
    
    private final ServerSocket serverSocket;
    private final MessagePack messagePack;
    
    private int lastXanelaId = 0;
    private Xanela lastXanela; 
    
    // http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html
    public static void premain(String agentArgs) {
        System.out.println( "Hi, I'm the agent, started with options: " + agentArgs );
                
        try {
            int port = DEFAULT_PORT;
            if (agentArgs!=null && !agentArgs.isEmpty()) {
                port = Integer.decode(agentArgs);
            }
            
            final ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            MessagePack messagePack = new MessagePack(); 
                        
            Thread serverThread = new Thread(new Driver(serverSocket,messagePack));
            serverThread.setDaemon(true);
            serverThread.start();
            System.out.println("Xanela server started at port " + port);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }catch (IOException e) {       
            e.printStackTrace();
        }
            
    }

    public Driver(ServerSocket serverSocket, MessagePack messagePack) {
        super();
        this.serverSocket = serverSocket;
        this.messagePack = messagePack;
    }

    @Override
    public void run() {
        try {
            boolean shutdownServer = false;
            while (!shutdownServer) {
                Socket  clientSocket = serverSocket.accept();
                
                InputStream sistream =  new BufferedInputStream(clientSocket.getInputStream());
                Unpacker unpacker = new MessagePackUnpacker(messagePack,sistream);
                
                OutputStream sostream =  new BufferedOutputStream(clientSocket.getOutputStream());
                Packer packer = new MessagePackPacker(messagePack,sostream);
               
                try {
                    String methodName = unpacker.readString();                
                    if (methodName.equals("get")) {
                        lastXanelaId++;
                        Xanela xanela = new Xanela();
                        packer.write((int)0);
                        xanela.buildAndWrite(lastXanelaId,packer);
                        lastXanela = xanela;     
                    } else if (methodName.equals("click")) {
                        int xanelaId = unpacker.readInt();
                        int buttonId = unpacker.readInt();
                        lastXanela.click(buttonId);
                        packer.write((int)0);
                        packer.writeNil();
                            
                    } else if (methodName.equals("rightClick")) {
                        int xanelaId = unpacker.readInt();
                        int cId = unpacker.readInt();
                        lastXanela.rightClick(cId);
                        packer.write((int)0);
                        packer.writeNil();
                    }  else if (methodName.equals("setTextField")) {
                        int xanelaId = unpacker.readInt();
                        int buttonId = unpacker.readInt();
                        String text = unpacker.readString();
                        lastXanela.setTextField(buttonId,text);
                        packer.write((int)0);
                        packer.writeNil();
                    } else if (methodName.equals("shutdown")) {
                        shutdownServer = true;
                    }
                    sostream.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace();    
                } catch (MessageTypeException msgte) {                
                    msgte.printStackTrace();
                } finally {
                    sistream.close();
                    sostream.close();
                    clientSocket.close();
                }
            }
            serverSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();    
        }  
    } 
}
