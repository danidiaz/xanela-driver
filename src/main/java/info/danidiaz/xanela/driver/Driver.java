package info.danidiaz.xanela.driver;

import java.io.IOException;

import org.msgpack.rpc.Server;
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
            svr.serve(new Driver());

            int port = DEFAULT_PORT;
            if (!agentArgs.isEmpty()) {
                port = Integer.decode(agentArgs);
            } 
            svr.listen(port);      
                        
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {           
                        loop.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                }
            }).start();
            System.out.println("Xanela server started at port " + port);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }catch (IOException e) {       
            e.printStackTrace();
        }
            
    }
    
    public String hello(String msg, int a) {
        return "foo";
    }
}
