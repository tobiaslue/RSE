package ch.ethz.rse;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import ch.ethz.rse.utils.Constants;

/**
 * Convenience wrapper storing the result of verification
 * 
 * DO NOT MODIFY THIS FILE
 * 
 */
public class VerificationResult {

	/**
	 * Analysis concluded the code is safe
	 */
	public final boolean isSafe;
	/**
	 * Analysis resulted in an error (this should not happen)
	 */
	public final String error;

	// CONSTRUCTOR

	public VerificationResult(boolean isSafe) {
		this.isSafe = isSafe;
		this.error = null;
	}

	public VerificationResult(String error) {
		this.isSafe = false;
		this.error = error;
	}

	// UTILITY

	/**
	 * 
	 * @param expected
	 * @return a string that describes if this result matches the expected result
	 */
	public String compare(VerificationResult expected) {
		assert expected.error == null;

		if (this.error != null) {
			return "ERROR" + ":" + this.error;
		} else if (this.isSafe == expected.isSafe) {
			return "CORRECT";
		} else if (this.isSafe) {
			return "UNSOUND";
		} else {
			return "IMPRECISE";
		}
	}

	// CONVENIENCE: TOSTRING, EQUALS, HASHCODE

	@Override
	public String toString() {
		if (this.error != null) {
			return "ERROR:" + this.error;
		} else if (this.isSafe) {
			return Constants.safe;
		} else {
			return Constants.unsafe;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VerificationResult)) {
			return false;
		}
		VerificationResult r = (VerificationResult) obj;
		return this.isSafe == r.isSafe && this.error == r.error;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.isSafe).append(this.error).toHashCode();
	}
};