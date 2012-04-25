package info.danidiaz.xanela.driver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.msgpack.MessagePackable;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.rpc.Request;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.dispatcher.Dispatcher;
import org.msgpack.rpc.loop.EventLoop;

public class Driver 
{
    
    // http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml
    private static int DEFAULT_PORT = 26060;
    
    // http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html
    public static void premain(String agentArgs) {
        System.out.println( "Hi, I'm the agent, started with options: " + agentArgs );
                
        try {            
            final EventLoop loop = EventLoop.defaultEventLoop();
            final Server svr = new Server();
            svr.serve(new XanelaDispatcher());

            int port = DEFAULT_PORT;
            if (agentArgs!=null && !agentArgs.isEmpty()) {
                port = Integer.decode(agentArgs);
            } 
            svr.listen(port);      
                        
            Thread serverThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {           
                        loop.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            System.out.println("Xanela server started at port " + port);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }catch (IOException e) {       
            e.printStackTrace();
        }
            
    }
    
    static class XanelaObject implements MessagePackable {

        @Override
        public void messagePack(final Packer packer) throws IOException {
            
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            packer.packArray(2);
                            packer.pack("fim fam fum");                            
                            packer.packArray(3);
                            packer.pack((int)1);
                            packer.pack((int)2);
                            packer.pack((int)3);                                                    
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }            
        }
        
    }
    
    static class XanelaDispatcher implements Dispatcher {

        @Override
        public void dispatch(Request request) throws Exception {
            request.sendResult(new XanelaObject());            
        }        
    }
    
}
