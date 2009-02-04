MASTER=" ";

if [ $# -ge 1 ]; then
	if [ $1 = "-master" ]; then
		MASTER="-Dnet.ulno.jpunch.Master=yes";
	fi;
	
	if [ $# -ge 2 ]; then
		if [ $2 = "-master" ]; then
			MASTER="-Dnet.ulno.jpunch.Master=yes";
		fi;
	fi;

	if [ $1 = "-dev" ]; then
		java -cp build/classes:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
		-Djava.util.logging.config.file=./conf/logging.properties \
		-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
		 $MASTER \
		net.ulno.jpunch.test.JPunchTest;
	elif [ $1 = "-master" ]; then
		java -cp build/classes:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
		-Djava.util.logging.config.file=./conf/logging.properties \
		-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
		 $MASTER \
		net.ulno.jpunch.test.JPunchTest;
	elif [ $1 = "-help" ]; then
		echo "Usage :";
		echo "sh run.sh [options]";
		echo "Options :";
		echo "-help print this help message";
		echo "-dev run in development mode";
		echo "-master run master node";
	fi;
else
		java -cp jPunch.jar:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
		-Djava.util.logging.config.file=./conf/logging.properties \
		-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
		$MASTER \
		net.ulno.jpunch.test.JPunchTest;
fi;
