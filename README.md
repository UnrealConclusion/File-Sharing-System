# File-Sharing-System

### The program assumes that
- Location of the Server's / Client's storage folder is the directory that it is being runned from 
    * the Server / Client treats the folder it is being runned from as its root directory "/"

### Note that
- the each client can only execute a single command at a time 
   * (i.e cannot do "client mkdir folder1 folder2" to create 2 folders with a single command)
- there is no support for case-sensitive 
   * (All input should be lowercase)
- download / upload will resume if the recieving party has a file of the same name with a smaller file size 
   * (a file with less bytes than the one that they will be recieving)

## Client Commands
   shut down the client
   ``` java -cp <path_to_pa1.jar> client shutdown ```
    - java -cp <path_to_pa1.jar> client dir 
    - java -cp <path_to_pa1.jar> client dir </path/existing_directory/on/server> 
    - java -cp <path_to_pa1.jar> client mkdir </path/new_directory/on/server> 
    - java -cp <path_to_pa1.jar> client rmdir </path/existing_directory/on/server>
    - java -cp <path_to_pa1.jar> client rm </path/existing_filename/on/server> 
    - java -cp <path_to_pa1.jar> client upload <path_on_client> </path/filename/on/server> 
    - java -cp <path_to_pa1.jar> client download </path/existing_filename/on/server> <path_on_client>

Server Commands:
    java -cp <path_to_pa1.jar> server start <port_number>

### Instructions for running the Server
1. starting from the directory where pa1.jar is located
2. make a new directory for the server to use as storage 
3. enter the new directory 
4. call the start command with the server's port number as an argument 

Example:
```
   mkdir server     
   cd server 
   java -cp ../pa1.jar server start 8000 
 ```

### Instructions for running the Client:
1. starting from the directory where pa1.jar is located
2. make a new directory for the client to use as storage 
3. enter the new directoy 
4. set environment variable PA1_SERVER
5. call commands from here 

Example:
```
   mkdir client
   cd client 
   export PA1_SERVER=localhost:8000
   java -cp ../pa1.jar client <command> <argument_1> <argument_2> 
```
