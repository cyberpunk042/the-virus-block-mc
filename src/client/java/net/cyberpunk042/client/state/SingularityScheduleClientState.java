package net.cyberpunk042.client.state;

import java.util.Collections;
import java.util.List;

import net.cyberpunk042.network.SingularitySchedulePayload;

public final class SingularityScheduleClientState {
	private static List<SingularitySchedulePayload.RingEntry> rings = Collections.emptyList();

	private SingularityScheduleClientState() {
	}

	public static void apply(SingularitySchedulePayload payload) {
		if (payload == null) {
			rings = Collections.emptyList();
		} else {
			rings = List.copyOf(payload.rings());
		}
	}

	public static List<SingularitySchedulePayload.RingEntry> rings() {
		return rings;
	}

	public static void reset() {
		rings = Collections.emptyList();
	}
}

