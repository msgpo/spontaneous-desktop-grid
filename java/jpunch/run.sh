
if [ $# -ge 1 ]; then
	if [ $1 = "-dev" ]; then 
		java -cp build/classes:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
		-Djava.util.logging.config.file=./conf/logging.properties \
		-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
		net.ulno.jpunch.test.JPunchTest;
	elif [ $1 = "-help" ]; then
		echo "Usage :";
		echo "sh run.sh [options]";
		echo "Options :";
		echo "-help print this help message";
		echo "-dev run in development mode";
	fi;
else
	java -cp jPunch.jar:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
	-Djava.util.logging.config.file=./conf/logging.properties \
	-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
	net.ulno.jpunch.test.JPunchTest;
fi;