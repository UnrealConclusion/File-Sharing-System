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
                switch(this.command){
                    case "dir":
                        this.dir();
                        break;
                    case "mkdir":
                        this.mkdir();
                        break;
                }
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

            // check that the path is valid and directory does not already exist
            File directory = new File("." + path);
            if (directory.mkdir()){ // let the client know whether the operation succeeded or not
                this.outToClient.writeBoolean(true);
            }
            else{
                this.outToClient.writeBoolean(false);
            }


        }

    }

    public static void main(String[] args) {
        try{
            new server().start();;
        }catch(Exception e){
            System.out.println(e);
        }

        /* 
        // start command
        if (args[0].equals("start")){
            int port = 8000;
            try{

                // read the command from the client 
                String command = readLine(inFromClient);
                System.out.println(command);

                // read the server file path
                String serverPath = readLine(inFromClient);     
                
                System.out.println(serverPath);
       
                // read size of file 
                Long fileSize = inFromClient.readLong();
                System.out.println(fileSize);

                File myFile = new File("/Users/jacoblin/Desktop/File-Sharing-System/bin/server/testrun.txt");
                myFile.createNewFile();

                FileOutputStream fileOutputStream = new FileOutputStream(myFile);
                byte[] buffer = new byte[4*1024];
                int bytes = 0;

                while (fileSize > 0 && (bytes = inFromClient.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                    fileOutputStream.write(buffer,0,bytes);
                    fileSize -= bytes;      // read upto file size
                }

                fileOutputStream.close();

                // close the socket when done
                serverSocket.close();
            }
            catch(IOException e){
                System.out.println(e);
            }
        }


        // command not found
        else{
            System.out.println("Invalid Command");
        }
        */
    }
}
