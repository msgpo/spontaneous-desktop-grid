package ee.ut.f2f.comm;

import java.util.HashMap;
import java.util.UUID;

import ee.ut.f2f.core.CommunicationFailedException;
import ee.ut.f2f.core.F2FComputing;
import ee.ut.f2f.core.MessageNotDeliveredException;

public abstract class BlockingMessageSender
{
    private long msgID = 0;
    private synchronized long getMsgID() { return ++msgID; }
    private HashMap<Long, BlockingMessage> blockingMessages = new HashMap<Long, BlockingMessage>();
    //protected HashMap<Long, BlockingMessage> getBlockingMessages() { return blockingMessages; }

    public void sendMessageBlocking(Object message, long timeout, boolean countTimeout)
        throws CommunicationFailedException, InterruptedException
    {
        if (countTimeout && timeout <= 0) throw new MessageNotDeliveredException(message);
        
        BlockingMessage msg = new BlockingMessage(message, getMsgID());
        blockingMessages.put(msg.ID, msg);
        // start to wait for the confirmation before the message is sent out
        // this ensures that the reply is not received before the wait is called
        BlockingMessageThread t = new BlockingMessageThread(msg, timeout, countTimeout);
        t.start();
        // wait until the waiting thread has started before sending the message out
        while (!t.startedWaiting ||
               !(t.getState() != Thread.State.WAITING || 
                 t.getState() != Thread.State.TIMED_WAITING)) Thread.sleep(5);
        sendMessage(msg);
        // wait until the confirmation is received
        t.join();
        // throw an exception if it occurred
        if (t.interruptEx != null) throw t.interruptEx;
        if (t.notDeliveredEx != null) throw t.notDeliveredEx;
    }
    
    protected void messageReceived(Object message, UUID id) throws CommunicationFailedException
    {
        if (message instanceof BlockingMessage)
        {
            BlockingMessage msg = (BlockingMessage) message;
            //NB! send reply before forwarding the message to Core for processing
            sendMessage(new BlockingReply(msg));
            F2FComputing.messageReceived(msg.data, id);
        }
        else if (message instanceof BlockingReply)
        {
            BlockingReply msg = (BlockingReply) message;
            if (blockingMessages.containsKey(msg.ID))
            {
                BlockingMessage blockMsg = blockingMessages.get(msg.ID);
                blockingMessages.remove(msg.ID);
                synchronized (blockMsg)
                {
                    //log.debug(msg.ID +" end WAIT "+System.currentTimeMillis());
                    blockMsg.notify();
                }
            }
        }
        else F2FComputing.messageReceived(message, id);
    }
    
    protected abstract void sendMessage(Object message) throws CommunicationFailedException;
    
    class BlockingMessageThread extends Thread
    {
        BlockingMessage msg;
        long timeout;
        boolean countTimeout;
        InterruptedException interruptEx = null;
        MessageNotDeliveredException notDeliveredEx = null;
        boolean startedWaiting = false;
        
        BlockingMessageThread(BlockingMessage msg, long timeout, boolean countTimeout)
        {
            this.msg = msg;
            this.timeout = timeout;
            this.countTimeout = countTimeout;
        }
        
        public void run()
        {
            synchronized(msg)
            {
                try {
                    //log.debug(msg.ID + " start WAIT "+System.currentTimeMillis() + " - "+msg.data);
                    startedWaiting = true;
                    if (countTimeout)
                        msg.wait(timeout);
                    else msg.wait(0);
                } catch (InterruptedException ex) {
                    interruptEx = ex;
                    return;
                }
                if (blockingMessages.containsKey(msg.ID))
                {
                    blockingMessages.remove(msg.ID);
                    notDeliveredEx = new MessageNotDeliveredException(msg.data);
                }
            }               
        }
    }
}
