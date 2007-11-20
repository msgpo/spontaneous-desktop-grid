package ee.ut.f2f.examples.pi;

import java.io.Serializable;

public class AtomicLongVector implements Serializable{
	private static final long serialVersionUID = 1L;
	private long total = 0;
	private long positive = 0;

	public AtomicLongVector(long total, long positive) {
		this.total = total;
		this.positive = positive;
	}

	// return a copy of the actual object but synchronized
	public synchronized AtomicLongVector get() {
		return new AtomicLongVector(total, positive);
	}

	public synchronized void set(long total, long positive) {
		this.total = total;
		this.positive = positive;
	}

	public synchronized void add(long total, long positive) {
		this.total += total;
		this.positive += positive;
	}

	public synchronized long getUnSyncPositive() {
		return positive;
	}

	public synchronized long getUnSyncTotal() {
		return total;
	}

	public synchronized void add(AtomicLongVector v) {
		add(v.total, v.positive);
	}
	
	public synchronized void positiveHit () {
		total ++;
		positive ++;
	}
	
	public synchronized void negativeHit () {
		total ++;
	}

}
