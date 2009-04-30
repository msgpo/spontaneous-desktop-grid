#
#	Run script for jPunch	(Artjom Lind 02.02.2009)
#	
#	Establishes backend connectivty for initializing UDP Hole Punching
#	Requires relay host
#

MASTER=" ";
DEV="jPunch.jar";
FILENAME="./file";
HELP="off";
RELAYHOST=" ";
IDENTITY="";
A_PIPE="/tmp/a_host_pipe";
B_PIPE="/tmp/b_host_pipe";

#Help message
help()
{
	echo "Usage :";
	echo "sh run.sh [options]";
	echo "Options :";
	echo "-h print this help message";
	echo "-d run in development mode";
	echo "-m run master node";
	echo "-f filename in/out";
	echo "-r relay account@hostname";
	echo "-i relay identity file";
}

#run java function
# 1'st arg DEV
# 2'nd arg FILENAME
# 3'd  arg MASTER
run_java ()
{
	java -cp $1:lib/log4j-1.2.14.jar:lib/jstun-0.6.1.jar:lib/commons-codec-1.3.jar:lib/junit.jar \
	-Dlog4j.configuration=file:./conf/log4j.properties \
	-Dnet.ulno.jpunch.PropertiesFile=./conf/jpunch.properties \
	-Dnet.ulno.jpunch.Filename=$2 \
	$3 \
	net.ulno.jpunch.test.JPunchTest;
}

#run relay
# 1'st arg =>0 send, <0 receive
# 2'nd RELAYHOST
# 3'd  PIPE
# 4'th IDENTITY
relay()
{
	# Create/destroy pipes commands
	CREATE_PIPES="mkfifo ${A_PIPE}; mkfifo ${B_PIPE}; chmod 777 ${A_PIPE} ${B_PIPE};";
	DESTROY_PIPES="rm ${A_PIPE}; rm ${B_PIPE};";
	# Create,Send/Receive,Destroy pipes
	SEND="${CREATE_PIPES} cat >> $3; ${DESTROY_PIPES}"; 
	RECEIVE="${CREATE_PIPES} cat $3; ${DESTROY_PIPES}";
	ACTION=" ";
	if [ $1 -ge 0 ]; then
		ACTION=${SEND};	
	else
		ACTION=${RECEIVE};
	fi;
	
	# if no identity file given
	if [ -z $4 ]; then
		ssh -42Cc blowfish "$2" ${ACTION};
	else
		ssh -42Cc blowfish "-i" "$4" "$2" ${ACTION};
	fi;
}


#get the command line arguments
if [ $# -ge 1 ]; then
	while getopts hdmf:r:i: opt
	do
		case $opt in
			m) MASTER="-Dnet.ulno.jpunch.Master=yes";;
			d) DEV="build/classes";;
			f) FILENAME="$OPTARG";;
			r) RELAYHOST="$OPTARG";;
			i) IDENTITY="$OPTARG";;
			h) HELP="on";;
		esac;
	done;
	shift `expr $OPTIND - 1`;
fi;

#set-up constants
echo "MASTER=${MASTER}"
echo "DEV=${DEV}"
echo "FILENAME=${FILENAME}"
echo "RELAY=${RELAYHOST}"
echo "IDENTITY=${IDENTITY}"

# Shortcuts for send/receive
send_receive()
{
	relay -1 "$1" $2 "${IDENTITY}" | run_java ${DEV} ${FILENAME} ${MASTER} | relay 1 "$1" $3 "${IDENTITY}"
}

if [ $HELP = "on" ]; then
	help;
else
	if [ -z $MASTER ]; then
		send_receive "${RELAYHOST}" ${A_PIPE} ${B_PIPE};
	else
		send_receive "${RELAYHOST}" ${B_PIPE} ${A_PIPE};
	fi; 
fi;

