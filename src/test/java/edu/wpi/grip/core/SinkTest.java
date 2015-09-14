package edu.wpi.grip.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.wpi.grip.core.events.SetSinkEvent;
import edu.wpi.grip.core.events.SocketPublishedEvent;
import edu.wpi.grip.core.events.StepAddedEvent;
import edu.wpi.grip.core.operations.PythonScriptOperation;
import edu.wpi.grip.core.sinks.DummySink;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SinkTest {
    private final static class MockSink implements Sink {
        public Integer publishedValue;

        @Subscribe
        public void onSocketPublished(SocketPublishedEvent event) {
            publishedValue = (Integer) event.getSocket().getValue();
        }
    }

    private EventBus eventBus = new EventBus();
    private Socket<Integer> a, b, sum;

    @Before
    @SuppressWarnings("unchecked")
    public void createSimplePipeline() {
        final Pipeline pipeLine = new Pipeline(eventBus);

        final Step step = new Step(eventBus, new PythonScriptOperation("import edu.wpi.grip.core as grip\nimport java" +
                ".lang.Integer\n\ninputs = [\n    grip.SocketHint(\"a\", java.lang.Integer, grip.SocketHint.View.NON" +
                "E, None, 0),\n    grip.SocketHint(\"b\", java.lang.Integer, grip.SocketHint.View.NONE, None, 0),\n]" +
                "\n\noutputs = [\n    grip.SocketHint(\"sum\", java.lang.Integer),\n]\n\ndef perform(a, b):\n    retur" +
                "n a + b\n"));

        this.eventBus.post(new StepAddedEvent(step));

        this.a = (Socket<Integer>) step.getInputSockets()[0];
        this.b = (Socket<Integer>) step.getInputSockets()[1];
        this.sum = (Socket<Integer>) step.getOutputSockets()[0];
    }

    @Test
    public void testSetPublished() throws Exception {
        final MockSink sink = new MockSink();
        eventBus.post(new SetSinkEvent(sink));

        this.sum.setPublished(true);
        this.a.setValue(123);
        this.b.setValue(456);

        assertEquals((Integer) (123 + 456), sink.publishedValue);
    }

    @Test
    public void testSetPublishedAfter() throws Exception {
        final MockSink sink = new MockSink();
        eventBus.post(new SetSinkEvent(sink));

        this.a.setValue(123);
        this.b.setValue(456);
        this.sum.setPublished(true);

        assertEquals((Integer) (123 + 456), sink.publishedValue);
    }

    @Test
    public void testChangeSink() throws Exception {
        final MockSink sink = new MockSink();
        eventBus.post(new SetSinkEvent(sink));
        eventBus.post(new SetSinkEvent(new DummySink()));

        this.a.setValue(123);
        this.b.setValue(456);
        this.sum.setPublished(true);

        assertNotEquals((Integer) (123 + 456), sink.publishedValue);
    }
}
