import java.util.Arrays;
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

        else if (args[0].equals("mkdir")){
            if (args.length != 2){
                System.err.println("client: the \"mkdir\" command takes exactly 1 argument!");
            }
            return true;
        }

        else{ // all other commands are not supported 
            System.err.println("client: command not supported!");
            return false;
        }
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

    public boolean upload(String clientPath, String serverPath) throws IOException{
        File clientFile = new File(clientPath); // open the file to be uploaded 

        // check that the file is a file 
        if (!clientFile.isFile()){
            return false;
        }

        return true;
    }

    /**
     * Ask server for contents of a directory 
     * @param path location of the directory 
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO Streams 
     */
    public boolean dir(String path) throws IOException{
        this.outToServer.writeChars("dir\n"); // send command to server 
        this.outToServer.writeChars(path + "\n"); // send path to the server

        Boolean isValid = this.inFromServer.readBoolean(); // read whether the path is valid or not
        if (isValid){
            // print out the name of all the files 
            int numFiles = this.inFromServer.readInt(); // read the number of files 
            if (numFiles == 0){
                System.out.println("<Empty Directory>");
            }
            for (int i=0; i<numFiles; i++){
                System.out.println(readLine(inFromServer));
            }
            return true;
        }
        else{
            System.err.println("dir: no such directory");
            return false;
        }
    }

    /**
     * Ask the server to make a directory
     * @param path location of the new directory 
     * @return True if execution is successful, False otherwise 
     * @throws IOException IO streams
     */
    public boolean mkdir(String path) throws IOException{
        this.outToServer.writeChars("mkdir\n"); // send command to server
        this.outToServer.writeChars(path + "\n"); // send path to the server
        
        boolean success = this.inFromServer.readBoolean();
        if (success){
            System.out.println("mkdir: new directory created at " + path);
            return true;
        }
        else{
            System.out.println("mkdir: " + path + " is not a valid path or already exists");
            return false;
        }
    }

    public boolean rmdir(String path) throws IOException{

        boolean success = this.inFromServer.readBoolean();
        if (success){
            
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Close the Client's socket (Shutdown the Client)
     * @throws IOException
     */
    public void shutdownClient() throws IOException{
        this.serverConnection.close();
    }

    /**
     * Ask the Server to Shutdown
     * @return True if execution is successful, False otherwise 
     * @throws IOException
     */
    public boolean shutdownServer() throws IOException{
        this.outToServer.writeChars("shutdown\n"); // send command to server 
    
        // read the reply from the server 
        // server will reply with true if shutdown is successful and false otherwise 
        Boolean reply = this.inFromServer.readBoolean();
        if (reply){
            System.out.println("Server has Shutdown");
        }
        return reply;
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
                    success = (args.length == 1) ? myClient.dir("") : myClient.dir(args[1]); // if dir is called with no additional argument, pass in the current directory as the path
                    break;
                case "mkdir":
                    success = myClient.mkdir(args[1]);
                    break;
                case "upload":
                    success = myClient.upload(args[1], args[2]);
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




        /* 
        // upload command
        if (args[0].equals("upload")){


            try{
                Socket clientSocket = new Socket("Jacobs-Air", port);

                // send file to the server
                outToServer.writeLong(clientFile.length()); // let the server know the length of the file 

                int bytes = 0;
                FileInputStream fileInputStream = new FileInputStream(clientFile);

                // break file into chunks
                byte[] buffer = new byte[4*1024];
                while ((bytes=fileInputStream.read(buffer))!=-1){
                    outToServer.write(buffer,0,bytes);
                    outToServer.flush();
                }

                fileInputStream.close();


                // close the socket when done
                clientSocket.close();
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
        */
        System.exit(0);
    }
}