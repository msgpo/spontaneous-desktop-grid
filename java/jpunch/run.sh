MASTER=" ";
DEV="jPunch.jar";
FILENAME="./file";
HELP="off"

#get the command line arguments
if [ $# -ge 1 ]; then
	while getopts hdmf: opt
	do
		case $opt in
			m) MASTER="-Dnet.ulno.jpunch.Master=yes";;
			d) DEV="build/classes";;
			f) FILENAME="$OPTARG";;
			h) HELP="on" 
		esac;
	done;
	shift `expr $OPTIND - 1`
fi;

#echo "MASTER=${MASTER}"
#echo "DEV=${DEV}"
#echo "FILENAME=${FILENAME}"

if [ $HELP = "on" ]; then
		echo "Usage :";
		echo "sh run.sh [options]";
		echo "Options :";
		echo "-h print this help message";
		echo "-d run in development mode";
		echo "-m run master node";
		echo "-f filename in/out";	
else
		java -cp ${DEV}:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
		-Djava.util.logging.config.file=./conf/logging.properties \
		-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
		${MASTER} \
		-Dnet.ulno.jpunch.Filename=${FILENAME} \
		net.ulno.jpunch.test.JPunchTest;
fi;