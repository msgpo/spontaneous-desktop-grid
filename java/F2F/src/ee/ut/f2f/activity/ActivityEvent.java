/**
 * 
 */
package ee.ut.f2f.activity;

import java.util.Date;


/**
 * Every type activity state changes it should emit event of the given class or
 * one of its subclasses.
 * 
 * @author olegus
 * 
 */
public class ActivityEvent {
	/**
	 * Basic activity types. It is preferrable to keep this list minimal and
	 * define other enum classes for more specific events.
	 */
	public enum Type {
		STARTED,
		CHANGED,
		FINISHED,
		FAILED,
	}
	
	private Enum type;
	private Activity activity;
	private String description;
	private long time;
	
	/**
	 * @param type event type
	 * @param activity process where event has happened
	 */
	public ActivityEvent(Activity activity, Enum type) {
		this.type = type;
		this.activity = activity;
		this.time = System.currentTimeMillis();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Activity getActivity() {
		return activity;
	}

	public Enum getType() {
		return type;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}
