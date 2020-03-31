package ch.ethz.rse;

/**
 * We are verifying calls into this class
 * 
 * DO NOT MODIFY THIS FILE
 * 
 */
public class TrainStation {

	private final int nTracks;
	private final Boolean[] occupied;

	public TrainStation(int nTracks) {
		this.nTracks = nTracks;
		this.occupied = new Boolean[nTracks];
		// all entries are initially false:
		for (int i = 0; i < nTracks; i++) {
			this.occupied[i] = false;
		}
	}

	public void arrive(int track) {
		// check TRACK_NON_NEGATIVE
		assert 0 <= track;
		// check TRACK_IN_RANGE
		assert track < this.nTracks;

		// check NO_CRASH
		assert !this.occupied[track];

		this.occupied[track] = true;
	}
}
