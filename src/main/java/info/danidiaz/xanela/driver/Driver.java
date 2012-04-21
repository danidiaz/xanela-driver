package info.danidiaz.xanela.driver;

import java.io.IOException;

import org.msgpack.rpc.Server;
import org.msgpack.rpc.loop.EventLoop;

public class Driver 
{
    public static void premain(String agentArgs) {
        System.out.println( "Hi, I'm the agent, started with options: " + agentArgs );
        
        try {            
            final EventLoop loop = EventLoop.defaultEventLoop();
            final Server svr = new Server();
            svr.serve(new Driver());
            // http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml
            svr.listen(26060);      
                        
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
        } catch (IOException e) {
            e.printStackTrace();
        }
            
    }
    
    public String hello(String msg, int a) {
        return "foo";
    }
}
