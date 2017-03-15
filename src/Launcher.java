import java.io.IOException;
import java.util.Arrays;
public class Launcher{
    public static void main(String[] args)throws InterruptedException, IOException{
        try{
            
            if(args.length>0){
                System.out.println(Arrays.asList(args));
                ProcessBuilder builder = new ProcessBuilder(args);
                builder.redirectErrorStream(true);
                builder.start();
                Thread.sleep(5000);
                System.exit(0);
            }else{
                System.exit(1);
            }
            
        }catch(Exception e){
            e.printStackTrace();

        }
    }

}
