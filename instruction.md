# This a project java_filehandler.

# They are 2 java binaries. java_filehandler sends files from client to server.

# Use necessary classes. for logging use log4j.

Create folders for client and for server.
Create subfolders for each o them:
- lib contents all jar files
- config contents config file
- logs contents log files
- script contents compilation and startup scripts



# Client part

- standalone java binary showing running status on terminal and logging details into log.
- it pools local directories defined in config file for files to send.
- then create hash of file and send it to server
- ip and port of server are in config file
- when file sent, it remove it from file source path
- compress file before transfer
- encrypt compressed file by key stored in config file
- config file includes label of client

# Server part

- listen on port defined in config file
- decrypt a file with key stored in config file according to client's label
- uncompress file and store in path defined in config file based on client's label
- calculate hash 
- all details including start transfer, and finish transfer and transfer duration logged, and file name, and client label, and file hash in log file
- it shows current activity on terminal


# Notes
- create both cleint and server as standalone binaries running on diffrent ip addresses
- create java compilation shell scripts
- create shell startup scripts for both binaries
- client and server print out environment paths, java version, variables from config files at the start
- provide example initial working setup to test application

