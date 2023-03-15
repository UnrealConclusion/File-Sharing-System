import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class server{

    private ServerSocket serverSocket;

    public server() throws IOException{
        this.serverSocket = new ServerSocket(8000);
    }

    public void start() throws IOException, InterruptedException{

        // 
        ExecutorService exectuor = Executors.newFixedThreadPool(100);

        while(!exectuor.isShutdown()){

            // listen for connection
            Socket socketConnection = serverSocket.accept();

            System.out.println("New client connected");

            // setup IO
            DataInputStream inFromClient = new DataInputStream(socketConnection.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(socketConnection.getOutputStream());

            String command = this.readLine(inFromClient); // read the command from the Client 
            System.out.println(command);
            // if the command is to shutdown the server 
            if (command.equals("shutdown")){
                System.out.println("Beginning Shutdown Procedure");
                
                exectuor.shutdown(); // stop accepting tasks 

                System.out.println("Waiting for Remaining Tasks to Complete ...");
                exectuor.awaitTermination(10000, TimeUnit.SECONDS); // wait for remaining threads to finish 

                // let the client know if shutdown is successful 
                if (exectuor.isTerminated()){
                    outToClient.writeBoolean(true);
                }
                else {
                    outToClient.writeBoolean(false);
                }

            }
            else { // start a thread to execute the command 
                serverThread serviceThread = new serverThread(command, socketConnection, inFromClient, outToClient);
                exectuor.submit(serviceThread);
            }
        } 

        System.out.println("Server has Shutdown");
    }

    /**
    * Read from a DataInputStream until a newline character
    * @param dataInputStream the DataInputStream to read from
    * @return String of all characters up to a newline 
    */
    private String readLine(DataInputStream dataInputStream) throws IOException{
        String line = "";
        char nextChar = dataInputStream.readChar();
        while (nextChar != '\n'){
            line += nextChar;
            nextChar = dataInputStream.readChar();
        }
        return line;
    }

    private class serverThread implements Runnable{

        public void run(){
            try {
                // run the command
                switch(this.command){
                    case "dir":
                        this.dir();
                        break;
                    case "mkdir":
                        this.mkdir();
                        break;
                    case "rmdir":
                        this.rmdir();
                        break;
                    case "rm":
                        this.rm();
                        break;
                    case "upload":
                        this.upload();
                        break;
                    case "download":
                        this.download();
                        break;
                }

                this.clientConnection.close(); // close the connection 
            } catch (Exception e) {
                System.out.println(this.command + ": " + e);
            }
        }

        private String command;
        private Socket clientConnection;
        private DataInputStream inFromClient;
        private DataOutputStream outToClient; 

        public serverThread(String command, Socket socketConnection, DataInputStream inFromClient, DataOutputStream outToClient) throws IOException{
            this.command = command;
            this.clientConnection = socketConnection;
            this.inFromClient = inFromClient;
            this.outToClient = outToClient;
        }

        /**
         * Check if the path from the client is valid directory, 
         * let the client know whether the operation succeeded , 
         * and send back a list of the contents if it did
         * @throws IOException IO Streams
         */
        private void dir() throws IOException{
            String path = readLine(this.inFromClient); 
            File directory = new File("." + path); 
            System.out.println(path);
            // check that the path exists and is a directory 
            if (directory.exists() && directory.isDirectory()){
                this.outToClient.writeBoolean(true); // let the client know that the path is valid
                File[] content = directory.listFiles(); // get all the contents of the directory
                this.outToClient.writeInt(content.length); // let the client know the number of files they will be sent 
                for (int i=0; i<content.length; i++){ // send the names of all the file to client
                    this.outToClient.writeChars(content[i].getName() + "\n");
                }
            }
            else{
                this.outToClient.writeBoolean(false); // let the client know that the path is not valid 
            }
        }

        /**
         * Check if the path from the client is valid 
         * and let the client know whether the operation succeeded 
         * @throws IOException IO Stream
         */
        private void mkdir() throws IOException{
            String path = readLine(this.inFromClient); 

            // try to make the directory and let the client know whether the operation succeeded or not
            File directory = new File("." + path);
            if (directory.mkdir()){ 
                this.outToClient.writeBoolean(true);
            }
            else{
                this.outToClient.writeBoolean(false);
            }
        }

        private void rmdir() throws IOException{
            String path = readLine(this.inFromClient); 

            // try to delete the directory and let the client know whether the operation succeded or not
            File directory = new File("." + path);
            if (directory.delete()){
                this.outToClient.writeBoolean(true);
            }
            else{
                this.outToClient.writeBoolean(false);
            }
        }

        private void rm() throws IOException{
            String path = readLine(this.inFromClient); 

            // try to delete the file and let the client know whether the operation succeded or not
            File file = new File("." + path);
            if (file.delete()){
                this.outToClient.writeBoolean(true);
            }
            else{
                this.outToClient.writeBoolean(false);
            }
        }

        private void upload() throws IOException{
            String path = readLine(inFromClient);

            File file = new File("." + path);

            // check that the enclosing directory is valid 
            File directory = file.getParentFile();
            if (directory.exists()){ 
                this.outToClient.writeBoolean(true);
                long fileSize = this.inFromClient.readLong(); // read the file size from the client 
                long bytesDownloaded = 0; // number of bytes that has been written to the file so far
                FileOutputStream fileOutputStream; 

                // check if the file already exists and if the file is not as long as it should be
                if (file.exists() && file.length() < fileSize){
                    bytesDownloaded = file.length();
                    this.outToClient.writeBoolean(true); // tell the client to resume upload 
                    this.outToClient.writeLong(bytesDownloaded); // tell the client how many bytes we have already 
                    fileOutputStream = new FileOutputStream(file, true); // write to end of the existing file
                }
                else{
                    this.outToClient.writeBoolean(false); // tell the client to upload from the beginning 
                    fileOutputStream = new FileOutputStream(file); // create a new file 
                }

                byte[] buffer = new byte[1024]; // buffer to hold bytes from client 
                int bytes = 0;
                while(bytesDownloaded != fileSize && (bytes = this.inFromClient.read(buffer)) > -1){
                    fileOutputStream.write(buffer,0,bytes);
                    bytesDownloaded += bytes;
                    this.outToClient.writeBoolean(true);
                }
                fileOutputStream.close();
            }
            else{
                this.outToClient.writeBoolean(false);
            }
        }

        public void download() throws IOException{
            String path = readLine(this.inFromClient);
            File file = new File("." + path);

            // check if file exists and is not a directory 
            if (file.exists() && !file.isDirectory()){
                this.outToClient.writeBoolean(true); // let the client know that the file exists
                this.outToClient.writeLong(file.length()); // let the client know the file size

                FileInputStream fileInputStream = new FileInputStream(file);
                long bytesUploaded = 0; // number of bytes the client has recieved 

                // send file 1024 bytes at a time
                int bytes = 0; // number of bytes that was read from the file 
                byte[] buffer = new byte[1024]; // buffer to hold the bytes that was read
                while (bytesUploaded != file.length()){
                    bytes = fileInputStream.read(buffer);
                    outToClient.write(buffer,0,bytes); 
                    outToClient.flush();
                    bytesUploaded += bytes;
                }

                fileInputStream.close();
            }
            else{
                this.outToClient.writeBoolean(false);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0){
            System.err.println("server: no command was given!");
        }

        if (args[0].equals("start")){
            try{
                new server().start();;
            }catch(Exception e){
                System.out.println(e);
                e.printStackTrace();
                System.exit(1);
            }
        }
        else {
            System.err.println("server: command not supported!");
        }
        System.exit(0);
    }
}
