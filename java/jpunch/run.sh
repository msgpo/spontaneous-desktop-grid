
if [ $1 = "-dev" ]; then 
java -cp build/classes:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
-Djava.util.logging.config.file=./conf/logging.properties \
-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
net.ulno.jpunch.test.JPunchTest;
fi;

if [ $1 = "-rel" ]; then
java -cp jPunch.jar:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
-Djava.util.logging.config.file=./conf/logging.properties \
-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
net.ulno.jpunch.test.JPunchTest;
fi;