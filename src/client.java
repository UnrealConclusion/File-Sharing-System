import java.net.Socket;
import java.io.*;

public class client{

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
        if (args[0].equals("shutdown")){
            if (args.length > 1){
                System.err.println("client: the \"shutdown\" command does not take any arguments!");
                return false;
            }
        }
        else if (args[0].equals("dir")){ 
            if (args.length > 2){
                System.err.println("client: the \"dir\" command takes a maximum of 1 argument!");
                return false;
            }
        }
        else if (args[0].equals("mkdir") || args[0].equals("rmdir") || args[0].equals("rm")){
            if (args.length != 2){
                System.err.println("client: the \"" + args[0]  +"\" command takes exactly 1 argument!");
                return false;
            }
        }
        else if (args[0].equals("upload") || args[0].equals("download")){
            if (args.length != 3){
                System.err.println("client: the \"" + args[0] + "\" command takes exactly 2 argument!");
                return false;
            }
        }
        else{ // all other commands are not supported 
            System.err.println("client: command not supported!");
            return false;
        }

        return true;
    }

    /**
     * Alter paths that start at the root directory (/) to start from the current working directory (./) instead
     * @param path path to be altered 
     * @return path that starts in the current working directory
     */
    private static String sanitizePath(String path){
        if (path.charAt(0) == File.separatorChar || path.charAt(0) == '/'){
            path = "." + path;
        }
        return path;
    }
    //-------------------------------------------------- Client Methods & Variables --------------------------------------------------

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

    private Socket serverConnection; 
    private DataInputStream inFromServer;
    private DataOutputStream outToServer; 

    /**
     * Setup the server conncetion and IO streams
     * @param host server's computer name 
     * @param port server's port number 
     */
    public client(String host, int port) {
        try{
            this.serverConnection = new Socket(host, port);
            this.inFromServer = new DataInputStream(this.serverConnection.getInputStream());
            this.outToServer = new DataOutputStream(this.serverConnection.getOutputStream());
            // System.out.println("client: connected to the server");
        } catch(IOException e){
            System.err.println("client: cannot connect to server (" + e + ")");
            System.exit(1);
        }
    }

    /**
     * Close the client's socket connections 
     */
    private void shutdownClient() {
        try {
            this.serverConnection.close();
        } catch (IOException e) {
            System.err.println("client: error shutting down client (" + e + ")");
            System.exit(1);
        }
    }

    /**
     * Ask the server to shutdown
     * @return true if successful, false otherwise 
     */
    private boolean shutdownServer() {
        Boolean OK = false;
        try {
            this.outToServer.writeChars("shutdown\n"); // send command to the server 
            
            // server will reply with whether it successfully executed the command
            OK = this.inFromServer.readBoolean();
            if (OK){
                System.out.println("shutdown: server has shutdown");
            }
            else{
                System.err.println("shutdown: server refuses to shutdown");
            }
        } catch (IOException e) {
            System.err.println("client: error shutting down server (" + e + ")");
        } 
        return OK;
    }

    /***
     * Asks server for the content of a directory 
     * @param path server's filepath to the directory 
     * @return true if successful, false otherwise
     */
    private boolean dir(String path) {
        Boolean OK = false; 

        try {
            this.outToServer.writeChars("dir\n"); // send command to server 
            this.outToServer.writeChars(path + "\n"); // send path to the server

            // server will reply with whether it successfully executed the command (file path is valid)
            OK = this.inFromServer.readBoolean(); 
            if (OK){
                int numFiles = this.inFromServer.readInt(); 
                if (numFiles == 0){
                    System.out.println("<Empty Directory>");
                }
                for (int i=0; i<numFiles; i++){
                    System.out.println(readLine(inFromServer));
                }
            }
            else{
                System.err.println("dir: no such directory");
            }
        } catch (IOException e) {
            System.err.println("client: dir error (" + e + ")");
        }
        return OK;
    }

    /**
     * Ask the server to make a directory
     * @param path server's filepath to the new directory
     * @return true if successful, false otherwise
     */
    private boolean mkdir(String path) {
        Boolean OK = false; 

        try {
            this.outToServer.writeChars("mkdir\n"); // send command to server
            this.outToServer.writeChars(path + "\n"); // send path to the server

            // server will reply with whether it successfully executed the command (file path is valid)
            OK = this.inFromServer.readBoolean(); 
            if (OK){
                System.out.println("mkdir: new directory created at " + path);
            }
            else{
                System.err.println("mkdir: " + path + " is an invalid path or directory already exists");
            }
        } catch (IOException e) {
            System.err.println("client: mkdir error (" + e + ")");
        }
        return OK;
    }

    /**
     * Ask server to remove a directory 
     * @param path server's filepath to the directory
     * @return true if successful, false otherwise
     */
    private boolean rmdir(String path) {
        Boolean OK = false;

        try {
            this.outToServer.writeChars("rmdir\n"); // send command to server
            this.outToServer.writeChars(path + "\n"); // send path to the server

            // server will reply with whether it successfully executed the command (file path is valid)
            OK = this.inFromServer.readBoolean(); 
            if (OK){
                System.out.println("rmdir: " + path + " is removed");
            }
            else{
                System.err.println("rmdir: " + path + " is an invalid path, not a directory, or is not empty");
            }
        } catch (IOException e) {
            System.err.println("client: rmdir error (" + e + ")");
        } 
        return OK;
    }

    /**
     * Ak the server to remove a file
     * @param path server's filepath 
     * @return true if successful, false otherwise
     */
    private boolean rm(String path) {
        Boolean OK = false;

        try {
            this.outToServer.writeChars("rm\n"); // send command to server
            this.outToServer.writeChars(path + "\n"); // send path to the server

            // server will reply with whether it successfully executed the command (file path is valid)
            OK = this.inFromServer.readBoolean();
            if (OK){
                System.out.println("rm: " + path + " is removed");
            }
            else{
                System.err.println("rm: " + path + " is an invalid path or not a file");
            }
        } catch (IOException e) {
            System.err.println("client: rm error (" + e + ")");
        }
        return OK;
    }

    /**
     * Upload a file to the server 
     * @param clientPath path to the client file 
     * @param serverPath path to the file server
     * @return true if successful, false otherwise
     */
    private boolean upload(String clientPath, String serverPath) {
        Boolean OK = false;

        // check that the client's filepath is valid 
        File clientFile = new File(clientPath);
        if (!clientFile.isFile()){
            System.err.println("upload: client path " + clientPath + " is invalid or not a file");
        }
        else{
            try {
                this.outToServer.writeChars("upload\n"); // send command to the server 
                this.outToServer.writeChars(serverPath + "\n"); // send server path to the server  
            
                // server will confirm whether the server path is valid 
                if (!this.inFromServer.readBoolean()){
                    System.err.println("upload: server path " + serverPath + " is invalid");
                    return OK;
                }

                outToServer.writeLong(clientFile.length()); // let the server know the size of the file 
                long bytesUploaded = 0; // the number of bytes the server has recieved 
                FileInputStream fileInputStream = new FileInputStream(clientFile); // IO Stream for reading in the file 

                // server will confirm whether it wants to resume upload
                if (this.inFromServer.readBoolean()){
                    bytesUploaded = this.inFromServer.readLong(); // number of bytes of the file the server already has
                    fileInputStream.skip(bytesUploaded); // skip the bytes the server already has
                    System.out.print("upload: resuming upload");
                }

                int bytes = 0; // number of bytes that was read from the file 
                byte[] buffer = new byte[1024]; // buffer to hold the bytes that was read
                
                bytes = fileInputStream.read(buffer); 
                System.out.println("upload: " + Long.toString(bytesUploaded) + " / " + Long.toString(clientFile.length())); // print the progress
                while (bytesUploaded != clientFile.length()){
                    this.outToServer.write(buffer,0,bytes); 
                    this.outToServer.flush();
                    
                    // server confirmation that the bytes were recieved 
                    if (this.inFromServer.readBoolean()){
                        bytesUploaded += bytes;
                        System.out.println("upload: " + Long.toString(bytesUploaded) + " / " + Long.toString(clientFile.length())); // print the progress
                        bytes = fileInputStream.read(buffer);
                    }
                }

                fileInputStream.close();
            } catch (IOException e) {
                System.err.println("client: upload error (" + e + ")");
            }      
        }
        return OK;
    }

    /**
     * Download a file from the server 
     * @param clientPath path to the client file 
     * @param serverPath path to the server file 
     * @return true if successful, false otherwise
     */
    private boolean download(String serverPath, String clientPath) {
        Boolean OK = false;

        // check that the client's filepath is valid 
        File clientFile = new File(clientPath);
        if (clientFile.getParentFile() != null && !clientFile.getParentFile().isDirectory()){
            System.err.println("download: client path " + clientPath + " is invalid");
        }
        else{
            try {
                this.outToServer.writeChars("download\n"); // send command to the server 
                this.outToServer.writeChars(serverPath + "\n"); // send path to the server

                // server will confirm whether the server path is valid 
                if (!this.inFromServer.readBoolean()){
                    System.err.println("download: server path " + serverPath + " is invalid");
                    return OK;
                }

                long fileSize = this.inFromServer.readLong(); // get the file size from the server
                long bytesDownloaded = 0; // number of bytes that has been written to the file so far
                FileOutputStream fileOutputStream;

                // check if we need to resume download 
                if (clientFile.exists() && clientFile.length() < fileSize){
                    bytesDownloaded = clientFile.length(); // set bytes downloaded to the amount of bytes we already have
                    this.outToServer.writeBoolean(true); // tell the server to resume upload 
                    this.outToServer.writeLong(bytesDownloaded); // let the server know how many bytes the client already has
                    fileOutputStream = new FileOutputStream(clientFile, true); // write to end of the existing file
                    System.out.println("download: resuming download");
                }
                else{
                    this.outToServer.writeBoolean(false);
                    fileOutputStream = new FileOutputStream(clientFile); // overwrite the existing file
                }

                int bytes = 0;
                byte[] buffer = new byte[1024]; // buffer to hold bytes from server
                
                System.out.println("download: " + Long.toString(bytesDownloaded) + " / " + Long.toString(fileSize));
                while(bytesDownloaded != fileSize && (bytes = this.inFromServer.read(buffer)) > -1){
                    fileOutputStream.write(buffer,0,bytes);
                    bytesDownloaded += bytes;
                    System.out.println("download: " + Long.toString(bytesDownloaded) + " / " + Long.toString(fileSize)); 
                } 

                fileOutputStream.close();
            } catch (IOException e) {
                System.err.println("client: download error (" + e + ")");
            } 
        }
        return OK;
    }

    //-------------------------------------------------- Main Method --------------------------------------------------
    public static void main(String[] args) {

        // check that the command is valid and the correct number of arguments are provided
        if (!checkCommand(args)){
            System.exit(1);
        }

        // check that environment variable PA1_SERVER is set 
        if (System.getenv("PA1_SERVER") == null){
            System.err.println("client: need to export PA1_SERVER=<computername:portnumber>");
            System.exit(1);
        }

        // obtain the server's host & port # from PA1_SERVER
        String[] PA1_SERVER = System.getenv("PA1_SERVER").split(":"); 
        String host = PA1_SERVER[0]; //
        int port =  Integer.parseInt(PA1_SERVER[1]);

        // setup the client 
        client myClient = new client(host, port);
        
        // attempt to execute the command 
        boolean success = false;
        switch(args[0]){
            case "shutdown":
                success = myClient.shutdownServer();
                break;
            case "dir":
                success = (args.length == 1) ? myClient.dir(sanitizePath("/")) : myClient.dir(sanitizePath(args[1])); 
                break;
            case "mkdir":
                success = myClient.mkdir(sanitizePath(args[1]));
                break;
            case "rmdir":
                success = myClient.rmdir(sanitizePath(args[1]));
                break;
            case "rm":
                success = myClient.rm(sanitizePath(args[1]));
                break;
            case "upload":
                success = myClient.upload(sanitizePath(args[1]), sanitizePath(args[2]));
                break;
            case "download":
                success = myClient.download(sanitizePath(args[1]), sanitizePath(args[2]));
                break;
        }

        // shut down the client
        myClient.shutdownClient();

        // check if the command was executed successfully 
        if (!success){
            System.exit(1);
        }
        else{
            System.exit(0);
        }
    }
}