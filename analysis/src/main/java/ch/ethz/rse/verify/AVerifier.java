package ch.ethz.rse.verify;

import ch.ethz.rse.VerificationProperty;

/**
 * DO NOT MODIFY THIS FILE
 */
public abstract class AVerifier {

	public boolean check(VerificationProperty t) {
		switch (t) {
		case TRACK_NON_NEGATIVE:
			return this.checkTrackNonNegative();
		case TRACK_IN_RANGE:
			return this.checkTrackInRange();
		case NO_CRASH:
			return this.checkNoCrash();
		default:
			throw new UnsupportedOperationException(t.toString());
		}
	}

	public abstract boolean checkTrackNonNegative();

	public abstract boolean checkTrackInRange();

	public abstract boolean checkNoCrash();
}
