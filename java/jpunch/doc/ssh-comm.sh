#for receiving
while [ true ]; do \
 ssh -42Cvc blowfish artjom85@math.ut.ee "cat /tmp/pipe2" >> /tmp/pipe2; \
 done;

#for sending
while [ true ]; do \
 set `cat  /tmp/pipe1`; \
 ssh -42Cvc blowfish artjom85@math.ut.ee "echo $1 >> /tmp/pipe1"; \
 done;
