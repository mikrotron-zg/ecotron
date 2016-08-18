# ecotron
IoT trashcan by http://www.DIYKits.eu/

For hardware and related instructions, see
https://hackaday.io/project/13111-internet-of-trash

Software is split in three parts:

1) arduino
Firmware that performs measurements, and sends the data to the sever.
Build and install with Arduino IDE.

2) server
Collection of Java servlets to receive the data from devices, and display the data to clients.
Build with 'ant war', then deploy ecotron-server.war to your app server.

3) webapp
A web application that reads the data from the server, and displays it on the map.
Build with 'mvn clean install', then deploy target/ecotronmap-demo.war to your app server.