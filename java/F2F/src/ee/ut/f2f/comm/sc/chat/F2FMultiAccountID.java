package ee.ut.f2f.comm.sc.chat;

import java.util.Hashtable;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolNames;

public class F2FMultiAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    F2FMultiAccountID(String id)
    {
        super(id, new Hashtable(), ProtocolNames.F2F, ProtocolNames.F2F);
    }
}