import java.net.Socket;
import java.io.*;

public class client{

    //---------- Methods to Check Arguments ----------

    /**
     * Check that a valid command was provided and that the correct amount of arguments were provided 
     * @param args Arguments from Main
     * @return True if the command is valid and the number of arguments is correct, Otherwise return False
     */
    private static boolean checkCommand(String[] args){
        // Check that a command was provided
        if (args.length == 0){
            System.err.println("client: no command was given!");
            return false;
        }

        if (args[0].equals("shutdown")){
            if (args.length > 1){
                System.err.println("client: the \"shutdown\" command does not take any arguments!");
                return false;
            }
            return true;
        }

        // client dir </path/existing_directory/on/server>
        else if (args[0].equals("dir")){ 
            if (args.length > 2){
                System.err.println("client: the \"dir\" command takes a maximum of 1 argument!");
                return false;
            }
            return true;
        }

        // client mkdir </path/new_directory/on/server>
        else if (args[0].equals("mkdir")){
            if (args.length != 2){
                System.err.println("client: the \"mkdir\" command takes exactly 1 argument!");
                return false;
            }
            return true;
        }

        // rmdir </path/existing_directory/on/server>
        else if (args[0].equals("rmdir")){
            if (args.length != 2){
                System.err.println("client: the \"rmdir\" command takes exactly 1 argument!");
                return false; 
            }
            return true;
        }

        // client upload <path_on_client> </path/filename/on/server> 
        else if (args[0].equals("upload")){
            if (args.length != 3){
                System.err.println("client: the \"upload\" command takes exactly 2 argument!");
                return false;
            }
            return true;
        }

        // download </path/existing_filename/on/server> <path_on_client> 
        else if (args[0].equals("download")){
            if (args.length != 3){
                System.err.println("client: the \"download\" command takes exactly 2 argument!");
                return false;
            }
            return true;
        }

        else{ // all other commands are not supported 
            System.err.println("client: command not supported!");
            return false;
        }
    }
    

    private static String sanitizePath(String path){
        if (path.charAt(0) != '/'){
            path = '/' + path;
        }
        return path;
    }
    //---------- Client Methods & Variables ----------

    private Socket serverConnection;
    private DataInputStream inFromServer;
    private DataOutputStream outToServer; 

    /**
     * Constructor sets up socket connection and IO streams 
     * @throws IOException Either the socket or one of the IO stream
     */
    public client(String host, int port) throws IOException{
        this.serverConnection = new Socket("Jacobs-Air", port);
        this.inFromServer = new DataInputStream(this.serverConnection.getInputStream());
        this.outToServer = new DataOutputStream(this.serverConnection.getOutputStream());
        // System.out.println("Connection with Server Established");
    }

    /**
     * Ask server for contents of a directory 
     * @param path full filename path to the file server
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO Streams 
     */
    public boolean dir(String path) throws IOException{
        this.outToServer.writeChars("dir\n"); // send command to server 
        this.outToServer.writeChars(path + "\n"); // send path to the server

        // server will reply with whether the operation succeeded or not 
        Boolean success = this.inFromServer.readBoolean(); 
        if (success){
            // print out the name of all the files 
            int numFiles = this.inFromServer.readInt(); // read the number of files 
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
        return success;
    }

    /**
     * Ask the server to make a directory
     * @param path full filename path to the file server
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean mkdir(String path) throws IOException{
        this.outToServer.writeChars("mkdir\n"); // send command to server
        this.outToServer.writeChars(path + "\n"); // send path to the server
        
        // server will reply with whether the operation succeeded or not 
        boolean success = this.inFromServer.readBoolean();
        if (success){
            System.out.println("mkdir: new directory created at " + path);
        }
        else{
            System.out.println("mkdir: " + path + " is not a valid path or already exists");
        }
        return success;
    }

    /**
     * Ask the server to remove a directory 
     * @param path full filename path to the file server
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean rmdir(String path) throws IOException{
        this.outToServer.writeChars("rmdir\n"); // send command to server
        this.outToServer.writeChars(path + "\n"); // send path to the server

        // server will reply with whether the operation succeeded or not 
        boolean success = this.inFromServer.readBoolean();
        if (success){
            System.out.println("rmdir: " + path + " is removed");
        }
        else{
            System.out.println("rmdir: " + path + " is not a valid path or directory does not exists");
        }
        return success;
    }

    /**
     * Ask the server to remove a file 
     * @param path full filename path to the file server
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean rm(String path) throws IOException{
        this.outToServer.writeChars("rm\n"); // send command to server
        this.outToServer.writeChars(path + "\n"); // send path to the server

        // server will reply with whether the operation succeeded or not 
        boolean success = this.inFromServer.readBoolean();
        if (success){
            System.out.println("rm: " + path + " is removed");
        }
        else{
            System.out.println("rm: " + path + " is not a valid path or file does not exists");
        }
        return success;
    }

    /**
     * Check that both paths are valid then send the file from the client to the server in blocks of 1024 bytes
     * @param clientPath full filename path to the client file 
     * @param serverPath full filename path to the file server
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean upload(String clientPath, String serverPath) throws IOException{
        File clientFile = new File("." + clientPath); // open the file to be uploaded 

        // check that the file is a file 
        if (!clientFile.isFile()){
            System.err.println("upload: client path " + clientPath + " is not a valid path or file does not exists");
            return false;
        }

        this.outToServer.writeChars("upload\n"); // send command to the server 
        this.outToServer.writeChars(serverPath + "\n"); // send server path to the server         

        // check that the server path is valid 
        boolean isValid = this.inFromServer.readBoolean();
        if (!isValid){
            System.err.println("upload: server path " + serverPath + " is not a valid path or file does not exists");
            return false;
        }

        outToServer.writeLong(clientFile.length()); // let the server know the size of the file 
        FileInputStream fileInputStream = new FileInputStream(clientFile);
        long bytesUploaded = 0; // the number of bytes the server has recieved 

        // check if we need to resume download
        boolean resumeDownload = this.inFromServer.readBoolean();
        if (resumeDownload){
            bytesUploaded = this.inFromServer.readLong();
            fileInputStream.skip(bytesUploaded);
        }

        // send file 1024 bytes at a time 
        int bytes = 0; // number of bytes that was read from the file 
        byte[] buffer = new byte[1024]; // buffer to hold the bytes that was read
        bytes = fileInputStream.read(buffer);
        while (bytesUploaded != clientFile.length()){
            
            this.outToServer.write(buffer,0,bytes); 
            this.outToServer.flush();
            
            // server comfirmation that the bytes were recieved 
            if (this.inFromServer.readBoolean()){
                bytesUploaded += bytes;
                System.out.println("upload: " + Long.toString(bytesUploaded) + " / " + Long.toString(clientFile.length())); // print the progress
                bytes = fileInputStream.read(buffer);
            }
        }

        fileInputStream.close();

        return true;
    }

    /**
     * Check that both paths are valid then ask the server to send the client the file in blocks of 1024 bytes 
     * @param serverPath full filename path to the file server
     * @param clientPath full filename path to the client file 
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean download(String serverPath, String clientPath) throws IOException{

        // check if the client path is valid // make sure that the parent directory 
        File clientFile = new File("." + clientPath);
        File directory = clientFile.getParentFile(); 
        if (!directory.exists() || clientFile.isDirectory()){
            System.err.println("download: client path " + clientPath + " is not a valid path or file does not exists");
            return false;
        }

        this.outToServer.writeChars("download\n"); // send command to the server 
        this.outToServer.writeChars(serverPath + "\n"); // send path to the server

        Boolean isValid = this.inFromServer.readBoolean();
        if (!isValid){
            System.err.println("download: server path " + serverPath + " is not a valid path or file does not exists");
            return false;
        }

        FileOutputStream fileOutputStream;
        long fileSize = this.inFromServer.readLong(); // get the file size from the server
        long bytesDownloaded = 0; // number of bytes that has been written to the file so far

        // check if we need to resume download 
        if (clientFile.exists() && clientFile.length() < fileSize){
            bytesDownloaded = clientFile.length();
            this.outToServer.writeBoolean(true); // tell the server to resume upload 
            this.outToServer.writeLong(bytesDownloaded); // tell the server how many bytes we have already 
            fileOutputStream = new FileOutputStream(clientFile, true); // write to end of the existing file
            System.out.println("download: resuming download");
        }
        else{
            fileOutputStream = new FileOutputStream(clientFile); // overwrite the existing file
        }

        byte[] buffer = new byte[1024]; // buffer to hold bytes from server
        int bytes = 0;
        while(bytesDownloaded != fileSize && (bytes = this.inFromServer.read(buffer)) > -1){
            fileOutputStream.write(buffer,0,bytes);
            bytesDownloaded += bytes;
            System.out.println("download: " + Long.toString(bytesDownloaded) + " / " + Long.toString(fileSize)); // print the progress
        } 
        fileOutputStream.close();
        
        return true;
    }

    /**
     * Close the Client's socket (Shutdown the Client)
     * @throws IOException socket 
     */
    public void shutdownClient() throws IOException{
        this.serverConnection.close();
    }

    /**
     * Ask the Server to Shutdown
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean shutdownServer() throws IOException{
        this.outToServer.writeChars("shutdown\n"); // send command to server 
    
        // read the reply from the server 
        // server will reply with true if shutdown is successful and false otherwise 
        Boolean success = this.inFromServer.readBoolean();
        if (success){
            System.out.println("shutdown: server has shutdown");
        }
        else{
            System.out.println("shutdown: server refuses to shutdown");
        }
        return success;
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

    //---------- Main Method ----------

    public static void main(String[] args) {

        // Check that the command is valid and the correct number of arguments are provided 
        if (!checkCommand(args)){
            System.exit(1);
        }

        if (System.getenv("PA1_SERVER") == null){
            System.err.println("client: need to export PA1_SERVER=<computername:portnumber>");
            System.exit(1);
        }

        // Retrieve server name and port number 
        String[] PA1_SERVER = System.getenv("PA1_SERVER").split(":");
        String host = PA1_SERVER[0];
        int port =  Integer.parseInt(PA1_SERVER[1]);

        try{
            client myClient = new client(host, port); // setup the client 

            // attempt to execute the command 
            boolean success = false;
            switch(args[0]){
                case "shutdown":
                    success = myClient.shutdownServer();
                    break;
                case "dir":
                    success = (args.length == 1) ? myClient.dir(".") : myClient.dir(sanitizePath(args[1])); // if dir is called with no additional argument, pass in the current directory as the path
                    break;
                case "mkdir":
                    success = myClient.mkdir(sanitizePath(args[1]));
                    break;
                case "rmdir":
                    success = myClient.rmdir(sanitizePath(args[1]));
                    break;
                case "upload":
                    success = myClient.upload(sanitizePath(args[1]), sanitizePath(args[2]));
                    break;
                case "download":
                    success = myClient.download(sanitizePath(args[1]), sanitizePath(args[2]));
                    break;
            }

            myClient.shutdownClient(); // close the socket 

            // check if the command successfully executed 
            if (!success){
                System.exit(1);
            }

        } catch (Exception e){
            System.out.println(e);
        }

        System.exit(0);
    }
}