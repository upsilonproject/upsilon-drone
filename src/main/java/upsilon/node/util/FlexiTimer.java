package upsilon.node.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.time.Duration;
import java.time.Instant; 
import java.time.temporal.ChronoUnit;

import upsilon.node.dataStructures.ResultKarma;

@XmlRootElement
public class FlexiTimer {
	public static long getIntWithinBounds(final long test, final long min, final long max) {
		if (test < min) {
			return min;
		} else if (test > max) {
			return max;
		} else {
			return test;
		}
	}

	public static Duration getPeriodWithinBounds(final Duration test, final Duration min, final Duration max) { 
		return Duration.ofSeconds(FlexiTimer.getIntWithinBounds(test.getSeconds(), min.getSeconds(), max.getSeconds()));
	} 

	protected Duration sleepMin = GlobalConstants.MIN_SERVICE_SLEEP;
	protected Duration sleepMax = GlobalConstants.MAX_SERVICE_SLEEP;

	protected Duration inc = GlobalConstants.DEF_INC_SERVICE_UPDATE;
	protected Duration currentDelay = GlobalConstants.DEF_INC_SERVICE_UPDATE;
	protected String name = "untitled timer";

	protected int consecutiveCount = 0;
	protected ResultKarma currentResult;
	protected Instant lastChanged;

	protected Instant lastTouched;
	protected boolean isAbrupt = true;

	protected static final Random RANDOM_TIMER = new Random();

	public FlexiTimer() {
		this.lastTouched = Instant.now();
		this.lastChanged = Instant.now();
	}

	public int getConsequtiveCount() {
		return this.consecutiveCount;
	}

	@XmlElement
	public Duration getCurrentDelay() {
		return this.currentDelay;
	}

	@XmlElement
	public Instant getEstimatedFireDate() {
		return this.lastTouched.plus(this.currentDelay);
	}

	@XmlElement
	public Duration getIncrement() {
		return this.inc;
	}

	@XmlElement
	public Instant getLastChanged() {
		return this.lastChanged;
	}

	@XmlElement
	public Instant getLastTouched() {
		return this.lastTouched;
	}

	@XmlElement
	public Duration getMaximumDelay() {
		return this.sleepMax;
	}

	@XmlElement
	public Duration getMinimumDelay() {
		return this.sleepMin;
	}

	@XmlElement
	public String getName() {
		return this.name;
	}

	public long getSecondsRemaining() {
		return this.getSecondsRemaining(Calendar.getInstance().getTime());
	}

	public long getSecondsRemaining(final Date from) {
		final Instant fromTime = from.toInstant();
		final Instant nextDue = this.lastTouched.plus(this.currentDelay);
 
		return ChronoUnit.SECONDS.between(fromTime, nextDue);
		
		// return lastTouched.plus(currentDelay).minus(from.getTime())
		// return
		// currentDelay.plus(lastTouched.getTime()).minus(from.getTime()).getStandardSeconds();

		// long secondsRemaining = ((this.lastTouched.getTime() +
		// this.currentDelay.getMillis()) - from.getTime()) / 1000;

		// return secondsRemaining;
	}

	public boolean isTouchedPassed() {
		return this.isTouchedPassed(Calendar.getInstance().getTime());
	}

	public boolean isTouchedPassed(final Date from) {
		return this.getSecondsRemaining(from) <= 0;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " {currentDelay: " + this.getCurrentDelay() + ", min:" + this.getMinimumDelay() + ", max:" + this.getMaximumDelay() + "}";
	}
}
