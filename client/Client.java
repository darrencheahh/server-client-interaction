import java.net.*;
import java.io.*;

public class Client
{
	public static void main( String[] args )
	{
		String hostName = "localhost";
		int portNum = 9100;

		if (args.length < 1){
			System.out.println("Command not found");
			return;
		}

		String command = args[0];
		String fileName = null;

		if("put".equals(command)) {
			if(args.length < 2) {
				System.out.println("File name is missing for put command");
				return;
			}
			fileName = args[1];
			File file = new File(fileName);
			//check if file exist
			if(!file.exists() || !file.isFile()) {
				System.out.println("Error: Cannot open local file " + fileName + " for reading");
				return;
			}
		}

		if("put".equals(command) && "list".equals(command)) {
			System.out.println("Error: Cannot recognise command, try again");
			return;
		}

		try(
			Socket sk = new Socket(hostName, portNum); //declaration for socket
			PrintWriter out = new PrintWriter(sk.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sk.getInputStream()))
		){
			//send command to server (list, put, etc.)
			out.println(command);

			if("list".equals(command)) {
				String summary = in.readLine();
				System.out.println(summary);

				String line;
				while((line = in.readLine()) != null) {
					System.out.println(line);
				}
			}

			if("put".equals(command) && args.length > 1){
				out.println(fileName);
				//read file and send contents to server
				try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
					String line;
					while ((line = fileReader.readLine()) != null) {
						out.println(line);
					}
					out.println("EOF");
				} catch (FileNotFoundException e) {
					System.err.println("File not found " + fileName);
				} catch (IOException e) {
					System.err.println("Error reading from file: " + fileName);
				}

				//read response from server and print it out
				String serverResponse = in.readLine();
				if("File uploaded successfully".equals(serverResponse)){
					System.out.println("Uploaded file " + fileName + " successfully");
				} else if("File already exist".equals(serverResponse)) {
					System.err.println("Error: File " + fileName + " already exists on the server.");
				} else {
					System.err.println(serverResponse);
				}
			}

		} catch(IOException e) { //for unreachable host, routing problems, etc
			System.err.println("Couldn't get I/O for the connection to " + hostName);
			System.exit(1);
		}
	}
}