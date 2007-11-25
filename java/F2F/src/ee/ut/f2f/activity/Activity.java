/**
 * 
 */
package ee.ut.f2f.activity;


/**
 * An activity (process) that happens inside F2F framework is expected to
 * implement this interface. This includes CPU requests, running tasks or STUN
 * server requests. This provides uniform way for GUI to deal with different F2F
 * internal threads.
 * 
 * Objects of this interface are expected to emit at least
 * {@link ActivityEvent.Type#STARTED}, {@link ActivityEvent.Type#FINISHED} and
 * {@link ActivityEvent.Type#FAILED} events.
 * 
 * Class implementing this interface is expected to have correct
 * {@link Object#hashCode()} implementation, because it may be used as a key in
 * different GUI tables and trees.
 * 
 * @author olegus
 * 
 */
public interface Activity {
	
	String getActivityName();
	Activity getParentActivity();

}
