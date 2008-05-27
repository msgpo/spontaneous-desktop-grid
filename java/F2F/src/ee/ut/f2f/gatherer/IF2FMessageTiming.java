package ee.ut.f2f.gatherer;

import java.util.Date;

public interface IF2FMessageTiming {
	
	public Date getRequestDateSent();
	
	public void setRequestDateSent(Date dateSent);
	
	public Date getResponseDateReceived();
	
	public void setResponseDateReceived(Date dateReceived);

}
