import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Server {

	private static final String serverFileDir = "serverFiles";
	private static final String logFile = "log.txt";

	public static void main( String[] args ) {
		int serverPortNo = 9100;
		int size = 20;

		//executor to manage fixed thread pool
		ExecutorService threadPool = Executors.newFixedThreadPool(size);

		try (ServerSocket ssk = new ServerSocket(serverPortNo)) {
			System.out.println("Server is listening on port" + serverPortNo);

			//server running continously
			while (true) {
				try {
					//accept new connection from client
					Socket clientSk = ssk.accept();
					System.out.println("New client connected");

					threadPool.execute(() -> handleClient(clientSk));
				} catch (IOException e) {
					System.err.println("Exception in client connection: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			//handling exception for the ServerSocket
			System.err.println("Server exception: " + e.getMessage());
		} finally {
			threadPool.shutdown();
		}
	}

	private static void handleClient(Socket clientSk) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSk.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSk.getOutputStream()));

			//read command from client
			String clientCmd = reader.readLine();

			//handle client commands
			switch (clientCmd) {
				case "list":
					//list all files
					listFiles(writer);
					break;
				case "put":
					//receive files from client
					receiveFiles(reader, writer);
					break;
				default:
					writer.write("Invalid command");
					writer.newLine();
					writer.flush();
					break;
				}
				logClientRequest(clientSk.getInetAddress().getHostAddress(), clientCmd);

			} catch (IOException e) {
				System.err.println("Error Handling Request: " + e.getMessage());
			} finally {
				try {
					clientSk.close();
				} catch(IOException e) {
					System.err.println("Error Handling Request: " + e.getMessage());
				}
			}
		}

	private static void listFiles(BufferedWriter writer) throws IOException {
		File dir = new File(serverFileDir);
		File[] files = dir.listFiles();

		if (files != null) {
			System.out.println("Sending file count: " + files.length); // Log the action
			writer.write("Listing " + files.length + " file(s):");
			writer.newLine();

			for(int i = 0; i < files.length; i++){
				File file = files[i];
				if(file.isFile()) {
					System.out.println("Sending file name: " + file.getName()); // Log each file name
					writer.write(file.getName());
					writer.newLine();
				}
			}
			System.out.println("Finished sending files.");
		} else {
			// Send a message if no files are found or if the directory doesn't exist
			writer.write("No files found.");
			writer.newLine();
		}
		writer.flush();
	}

	private static void receiveFiles(BufferedReader reader, BufferedWriter writer) throws IOException {
		String fileName = reader.readLine();
		File file = new File(serverFileDir, fileName);

		if (file.exists()){
			writer.write("File already exist");
			writer.newLine();
			writer.flush();
			return;
		}
		try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file))) {
			String inputLine;
			while (!(inputLine = reader.readLine()).equals("EOF")) {
				fileOut.write(inputLine.getBytes());
				fileOut.write(System.lineSeparator().getBytes());
			}
			fileOut.flush();
		} catch (EOFException e) {
			System.err.println("Error while writing this file: " + e.getMessage());
		}

		//verification if file is put into serverFiles properly
		if(file.exists() && file.length() > 0){
			writer.write("File uploaded successfully");
			writer.newLine();
			writer.flush();
			System.out.println("Upload verification: " + fileName + " exists and is not empty. Size: " + file.length() + " bytes.");
		} else if (file.exists()) {
			writer.write("File uploaded but appears to be empty");
			writer.newLine();
			writer.flush();
			System.err.println("Upload verification warning: " + fileName + " exists but is empty.");
		} else {
			writer.write("File upload failed");
			writer.newLine();
			writer.flush();
			System.err.println("Upload verification failed: " + fileName + " does not exist.");
		}
	}

	private static void logClientRequest(String clientIP, String clientReq){
		//create formatter for datatime
		DateTimeFormatter dTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		//timestamp
		String timeStamp = dTF.format(LocalDateTime.now());

		//log entry formatting
		String logEntry = String.format("%s - %s - %s%n", timeStamp, clientIP, clientReq);

		try(BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
			//write log entry
			writer.write(logEntry);
		} catch (IOException e){
			System.err.println("Error writing to log file: " + e.getMessage());
		}
	}
}
