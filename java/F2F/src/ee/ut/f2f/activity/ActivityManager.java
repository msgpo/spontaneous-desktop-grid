/**
 * 
 */
package ee.ut.f2f.activity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ee.ut.f2f.util.logging.Logger;

/**
 * This is central place where all activity events are emitted and forwarded to
 * subscribers. This makes transparent link between F2F layer and GUI elements,
 * that require information about F2F layer activities.
 * 
 * @author olegus
 * 
 * @todo olegus: this class may require different thread safety
 * @todo olegus: registering mechanism may be changed
 * @todo olegus: The better name for this class would be ActivityEventManager or
 *       even EventManager, because it does not manage activities
 */
public class ActivityManager {
	private final static Logger logger = Logger.getLogger(
			ActivityManager.class.getName());
	
	private static ActivityManager defaultActivityManager;
	
	private HashMap<Enum, Set<ActivityListener>> listeners = new HashMap<Enum, Set<ActivityListener>>();
	
	public ActivityManager() {
	}
	
	/**
	 * @param event
	 */
	public void emitEvent(ActivityEvent event) {
		if(logger.isTraceEnabled()) {
			logger.trace("Activity event "+event+" emitted.");
		}
		Set<ActivityListener> typeListeners = listeners.get(event.getType());
		
		if (typeListeners == null) return;
		for(ActivityListener listener: typeListeners) {
			try {
				listener.activityEvent(event);
			} catch (Throwable e) {
				logger.warn("Error in activity event notification.", e);
			}			
		}
	}

	/**
	 * Register listener for the given event types.
	 * @param eventTypes
	 * @param listener
	 */
	public void addListener(Enum[] eventTypes, ActivityListener listener) {
		synchronized (this.listeners) {
			for(Enum eventType: eventTypes) {
				getListeners(eventType).add(listener);
			}
		}
	}
	
	private Set<ActivityListener> getListeners(Enum eventType) {
		if(!listeners.containsKey(eventType)) {
			listeners.put(eventType, new HashSet<ActivityListener>());
		}
		return listeners.get(eventType);
	}

	/**
	 * Unregister listener from all event types.
	 * @param listener
	 */
	public void removeListener(ActivityListener listener) {
		synchronized (listeners) {
			for(Set<ActivityListener> typeListeners: listeners.values()) {
				typeListeners.remove(listener);
			}
		}
	}

	public static ActivityManager getDefault() {
		if(defaultActivityManager == null) {
			defaultActivityManager = new ActivityManager();
		}
		return defaultActivityManager;
	}
}
