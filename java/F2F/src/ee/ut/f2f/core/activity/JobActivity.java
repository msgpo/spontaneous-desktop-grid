/**
 * 
 */
package ee.ut.f2f.core.activity;

import java.io.Serializable;

import ee.ut.f2f.activity.Activity;

/**
 * Signifies single job activity.
 * 
 * @author olegus
 */
public class JobActivity implements Activity, Serializable {

	private static final long serialVersionUID = 1L;
	
	private String jobID;
	
	public JobActivity(String jobID) {
		this.jobID = jobID;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.activity.Activity#getActivityName()
	 */
	public String getActivityName() {
		return jobID;
	}

	/* (non-Javadoc)
	 * @see ee.ut.f2f.activity.Activity#getParentActivity()
	 */
	public Activity getParentActivity() {
		return null;
	}

}
