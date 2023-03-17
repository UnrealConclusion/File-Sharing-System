import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class server{

    //-------------------------------------------------- Methods to Check Arguments --------------------------------------------------

    /**
     * Check that the command is valid and that the correct amount of arguments was provided 
     * note that file paths are not validated here 
     * @param args arguements from main 
     * @return true if the command is valid and the number of arguments is correct, otherwise return false
     */
    private static boolean checkCommand(String[] args) {

        // check that a command was provided 
        if (args.length == 0){
            System.err.println("client: no command was given!");
            return false;
        }

        // check that the command is supported and the correct number of arguments was provided 
        if (args[0].equals("start")){
            if (args.length != 2){
                System.err.println("server: the \"" + args[0]  +"\" command takes exactly 1 argument!");
                return false;
            }
        }
        else{
            System.err.println("server: command not supported!");
            return false;
        }
        return true;
    }

    //-------------------------------------------------- Server Methods & Variables --------------------------------------------------

    /**
    * Read from a DataInputStream until a newline character
    * @param dataInputStream the DataInputStream to read from
    * @return string of all characters up to a newline 
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

    private ServerSocket serverSocket;

    /**
     * Setup the ServerSocket
     * @param port server's port number 
     */
    public server(int port){
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("server: error setting up ServerSocket (" + e + ")");
            System.exit(1);
        }
    }

    /**
     * Start running the server
     */
    private void start() {
        System.out.println("server is running");

        ExecutorService exectuor = Executors.newFixedThreadPool(100); // use an executor service to manage server threads 

        // server will run as long as the executor service is running 
        while(!exectuor.isShutdown()){
            try {
                Socket socketConnection = serverSocket.accept();  // wait for connection

                // setup IO
                DataInputStream inFromClient = new DataInputStream(socketConnection.getInputStream());
                DataOutputStream outToClient = new DataOutputStream(socketConnection.getOutputStream());
            
                String command = this.readLine(inFromClient); // read the command from the Client 
                
                if (command.equals("shutdown")){ // shutdown the server
                    System.out.println("shutdown: beginning shutdown procedure");
                    exectuor.shutdown(); // stop accepting tasks 
                    System.out.println("shutdown: waiting for remaining tasks to complete ...");
                    exectuor.awaitTermination(120, TimeUnit.SECONDS); // wait 2 minutes for remaining threads to finish 

                    // let the client know if shutdown is successful 
                    if (exectuor.isTerminated()){
                        outToClient.writeBoolean(true);
                        System.out.println("shutdown: all tasks have been completed");
                    }
                    else {
                        outToClient.writeBoolean(false);
                        System.out.println("shutdown: there are still incomplete tasks");
                    }
                }
                else{ // start a thread to execute the command 
                    serverThread serviceThread = new serverThread(command, socketConnection, inFromClient, outToClient);
                    exectuor.submit(serviceThread);
                }
            } catch (IOException e) {
                System.err.println("server: error connecting to client (" + e + ")");
            } catch (InterruptedException e) {
                System.err.println("shutdown: timeout waiting for threads to finish (" + e + ")");
                System.exit(1);
            }
        }

        System.out.println("server has shutdown");
    }

    //-------------------------------------------------- Server Thread Methods & Variables --------------------------------------------------
    public class serverThread implements Runnable{

        private String command;
        private Socket clientConnection;
        private DataInputStream inFromClient;
        private DataOutputStream outToClient; 

        public serverThread(String command, Socket socketConnection, DataInputStream inFromClient, DataOutputStream outToClient) {
            this.command = command;
            this.clientConnection = socketConnection;
            this.inFromClient = inFromClient;
            this.outToClient = outToClient;
        }

        /**
         * Execute the command, then close the connection 
         */
        @Override
        public void run() {
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

            // close the connection with the client 
            try {
                this.clientConnection.close();
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": error closing connection (" + e + ")");
            }
        }

        /**
         * Send the client a list of the contents of a directory
         */
        private void dir() {
            try {
                String path = readLine(this.inFromClient); // read the server's file path from the client 

                // check that the path exists and is a directory 
                File directory = new File(path);
                if (directory.exists() && directory.isDirectory()){
                    this.outToClient.writeBoolean(true); // let the client know that the command will be executed 
                    File[] content = directory.listFiles(); // get all the contents of the directory
                    this.outToClient.writeInt(content.length); // let the client know the number of files they will be sent 

                    // send the names of all the file to client
                    for (int i=0; i<content.length; i++){ 
                        this.outToClient.writeChars(content[i].getName() + "\n");
                    }
                }
                else{
                    this.outToClient.writeBoolean(false); // let the client know that the command will not be executed 
                }
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": dir error (" + e + ")");
            }  
        }

        /**
         * Make a new directory
         */
        private void mkdir() {
            try {
                String path = readLine(this.inFromClient); // read the server's file path from the client 

                // try to make the directory and let the client know whether the operation succeeded or not
                File directory = new File(path);
                if (directory.mkdir()){ 
                    this.outToClient.writeBoolean(true);
                }
                else{
                    this.outToClient.writeBoolean(false);
                }
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": mkdir error (" + e + ")");
            } 

        }

        /**
         * Remove a directory
         */
        private void rmdir() {
            try {
                String path = readLine(this.inFromClient); // read the server's file path from the client 

                // try to delete the directory and let the client know whether the operation succeded or not
                File directory = new File(path);
                if (directory.isDirectory() && directory.delete()){
                    this.outToClient.writeBoolean(true);
                }
                else{
                    this.outToClient.writeBoolean(false);
                }                
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": rmdir error (" + e + ")");
            } 
        }

        /**
         * Remove a file
         */
        private void rm() {
            try {
                String path = readLine(this.inFromClient); // read the server's file path from the client 

                // try to delete the file and let the client know whether the operation succeded or not
                File file = new File(path);
                if (file.isFile() && file.delete()){
                    this.outToClient.writeBoolean(true);
                }
                else{
                    this.outToClient.writeBoolean(false);
                }             
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": rm error (" + e + ")");
            }          
        }

        /**
         * Upload a file
         */
        private void upload() {
            try {
                String path = readLine(inFromClient);
                
                // check that the client's filepath is valid 
                File file = new File(path);
                if ((file.getParentFile() == null || file.getParentFile().isDirectory()) && !file.isDirectory()){
                    this.outToClient.writeBoolean(true); // let the client know that the command will be executed 
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

                    int bytes = 0;
                    byte[] buffer = new byte[1024]; // buffer to hold bytes from client 
                    
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

            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": upload error (" + e + ")");
            }
        }

        /**
         * Download a file
         */
        private void download() {
            try {
                String path = readLine(inFromClient);

                // check that the client's filepath is valid 
                File file = new File(path);
                if (file.isFile()){
                    this.outToClient.writeBoolean(true);  // let the client know that the command will be executed 
                    this.outToClient.writeLong(file.length()); // let the client know the file size

                    long bytesUploaded = 0; // the number of bytes the client has recieved 
                    FileInputStream fileInputStream = new FileInputStream(file); // IO Stream for reading in the file 

                    // client will confirm whether it wants to resume download
                    if (this.inFromClient.readBoolean()){
                        bytesUploaded = this.inFromClient.readLong(); // number of bytes of the file the server already has
                        fileInputStream.skip(bytesUploaded); // skip the bytes the server already has
                    }

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
            } catch (IOException e) {
                System.err.println("server thread "+ Thread.currentThread().getId() + ": download error (" + e + ")");
            }
        }
    
    }

    //-------------------------------------------------- Main Method --------------------------------------------------
    public static void main(String[] args) {

        // check that the command is valid and the correct number of arguments are provided
        if (!checkCommand(args)){
            System.exit(1);
        }

        // start running the server
        new server(Integer.parseInt(args[1])).start();
        System.exit(0);
    }
}
