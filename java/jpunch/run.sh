java -cp build/classes:lib/commons-codec-1.3.jar:lib/jstun-0.6.1.jar \
-Djava.util.logging.config.file=./conf/logging.properties \
-Dnet.ulno.jpunch.StunServers=stun.ekiga.net,stun.fwdnet.net,stun.ideasip.com,stun01.sipphone.com,stun.softjoys.com,stun.voipbuster.com,stun.voxgratia.org,stun.xten.com,stunserver.org,stun.sipgate.net:10000 \
net.ulno.jpunch.test.JPunchTest