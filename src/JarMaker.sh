javac client.java server.java;
jar cf pa1.jar client.class server.class server\$serverThread.class;
rm client.class server.class server\$serverThread.class;
