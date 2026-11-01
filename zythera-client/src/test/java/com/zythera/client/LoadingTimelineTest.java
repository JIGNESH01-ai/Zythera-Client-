package com.zythera.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoadingTimelineTest {
    @Test void pausesAndCompletionAreTimedAndMonotonic() {
        LoadingTimeline timeline = new LoadingTimeline(0);
        assertEquals(1, timeline.update(0, true));
        assertEquals(50, timeline.update(1000, true));
        assertEquals(50, timeline.update(5999, true));
        assertEquals(50, timeline.update(6000, true));
        assertTrue(timeline.update(6001, true) > 50);
        assertEquals(70, timeline.update(7000, true));
        assertEquals(70, timeline.update(11999, true));
        assertTrue(timeline.update(12001, true) > 70);
        assertEquals(95, timeline.update(13000, true));
        assertEquals(95, timeline.update(17999, true));
        assertTrue(timeline.update(18001, true) > 95);
        assertEquals(100, timeline.update(19000, true));
        assertFalse(timeline.canClose(19799));
        assertTrue(timeline.canClose(19800));
    }
    @Test void waitsForActualReloadBeforeCompletion() {
        LoadingTimeline timeline = new LoadingTimeline(100);
        double last = 0;
        for (long t = 100; t < 22000; t += 37) {
            double value = timeline.update(t, false);
            assertTrue(value >= last);
            assertTrue(value < 100);
            last = value;
        }
        assertFalse(timeline.canClose(30000));
        assertEquals(100, timeline.update(30000, true));
        assertFalse(timeline.canClose(30799));
        assertTrue(timeline.canClose(30800));
    }
    @Test void freshOverlayStartsAtOneWithoutOldState() {
        LoadingTimeline first = new LoadingTimeline(0);
        first.update(19000, true);
        LoadingTimeline next = new LoadingTimeline(25000);
        assertEquals(1, next.update(25000, false));
        assertFalse(next.canClose(25000));
    }
}
