import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

public class Cmdline {

	public static void main(String[] args) {
		String cmd;
		
		if (args.length > 0) {
			StringBuilder cmdBuffer = new StringBuilder();
			
			for (String word : args)
				cmdBuffer.append(word + " ");
			
			cmd = cmdBuffer.toString();
		}
		else
			cmd = "ping www.neti.ee ";
		
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line;
			
			while ((line = br.readLine()) != null)
				System.out.println(line);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}
