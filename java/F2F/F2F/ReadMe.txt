Compiling the F2F client.
-------------------------
1. Make sure skype.jar, log4j-1.2.14.jar, commons-io-1.3.1.jar,
commons-codec-1.3.jar are in build path for Eclipse to remove the compiling
errors (is not required to build with ANT targets).
2. Make sure you compile against Java 5.0. This is the minimum version of
Java required.
3. Use build.xml ANT script to build the client:
  * Default target "buildAll" will build "socketBasedF2F" and
  	"skypeBasedF2F_Windows" targets.
  * Target "socketBasedF2F" will build Java socket-based F2F client
  	under "f2f_socket_client" directory.
  * Target "skypeBasedF2F_Windows" will build Skype4Java-based F2F client under
  	"f2f_skype_win_client" (currently fully supported under Windows only).
  * "_tmpBuild" directory is used as temporary build directory. This can be
  	removed if it should appear after build(s).

Running the F2F socket-based client.
------------------------------------
1. Compile the client with ANT target "socketBasedF2F"
2. Configure client's IP/port and IPs/ports of target nodes in
"f2f_socket.jar.properties".
3. Under Windows use "f2f_socket.bat" to run the F2F client. In other
environments use command:
java -jar -Dlog4j.configuration=file:log4j.properties f2f_socket.jar

Running the F2F Skype-based client.
------------------------------------
1. Compile the client with ANT target "skypeBasedF2F_Windows"
2. Start Skype.
3. Under Windows use "f2f_skype.bat" to run the F2F client. n other
environments use command:
java -jar -Dlog4j.configuration=file:log4j.properties f2f_skype.jar
4. Allow the program to access the Skype if requested (follow the prompt).
