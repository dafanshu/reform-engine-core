package acceptance;

import com.cloud.engine.pvm.*;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

public class TextBatchAcceptanceTest {

    private static final List<String> INPUT=Arrays.asList(" alpha ", "Beta");

    private void accepted(PvmProcessInstance p, List<String> result) {
        assertTrue(p.isEnded());
        assertTrue(p.findActiveActivityIds().isEmpty());
        assertEquals("accepted", p.getVariable("status"));
        assertEquals(result, p.getVariable("result"));
        assertEquals(1, p.getVariable("publishAttempts"));
        assertEquals(1, p.getVariable("endEvents"));
        assertFalse(p.hasVariable("scratch"));
        try {
            p.signal("submit", null);
            fail("ended instance must reject signals");
        } catch (PvmException expected) {
        }
        assertEquals(1, p.getVariable("endEvents"));
    }

    @Test
    public void artifactIsJarAndEngineTestNodesAreAbsent() throws Exception {
        Path source = Paths.get(ProcessDefinitionBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String expectedJar = System.getProperty("acceptance.engineJar");
        assertNotNull("Run acceptance/run.sh to identify the freshly built engine JAR", expectedJar);
        assertTrue(source.toString(), source.toString().endsWith(".jar"));
        assertArrayEquals("Consumer must use the freshly built production JAR", Files.readAllBytes(Paths.get(expectedJar)), Files.readAllBytes(source));
        System.out.println("VERIFIED_ENGINE_JAR=" + source);
        try {
            Class.forName("pvm.ucase.behavior.ParallelGateway");
            fail("engine test classes leaked into consumer");
        } catch (ClassNotFoundException expected) {
        }
    }

    @Test
    public void fullPathAllSixCompletionOrders() {
        String[][] orders = {
            {"normalize", "classify", "validate"},
            {"normalize", "validate", "classify"},
            {"classify", "normalize", "validate"},
            {"classify", "validate", "normalize"},
            {"validate", "normalize", "classify"},
            {"validate", "classify", "normalize"}
        };
        for (String[] order:orders) {
            TextBatchApplication app=new TextBatchApplication();
            PvmProcessInstance p=app.start("full", INPUT, null);
            p.signal("submit", null);
            assertEquals(new TreeSet<>(Arrays.asList("normalize", "classify", "validationCall")), new TreeSet<>(p.findActiveActivityIds()));
            for (int i = 0; i < 3; i++) {
                app.complete(p, order[i]);
                if (i<2) {
                    assertFalse(p.isEnded());
                    assertEquals(0, p.getVariable("publishAttempts"));
                    assertNull(p.getVariable("result"));
                }
            }
            accepted(p, Arrays.asList("ALPHA", "BETA"));
            assertEquals(1, p.getVariable("validationCallbacks"));
            assertTrue(app.child("full").isEnded());
        }
    }

    @Test
    public void smallBatchUsesQuickPath() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("small", Collections.singletonList(" one "), null);
        p.signal("submit", null);
        accepted(p, Collections.singletonList("ONE"));
        assertEquals("single", p.getVariable("category"));
        assertNull(app.child("small"));
        assertEquals(0, p.getVariable("validationCallbacks"));
    }

    @Test
    public void invalidDataProducesRejection() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("invalid", Arrays.asList("valid", " "), null);
        p.signal("submit", null);
        assertTrue(p.isEnded());
        assertEquals("rejected", p.getVariable("status"));
        assertNull(p.getVariable("result"));
        assertEquals(0, p.getVariable("publishAttempts"));
        assertEquals(1, p.getVariable("endEvents"));
    }

    @Test
    public void twoInstancesInterleaveWithoutBindingLeakage() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance a=app.start("a", INPUT, null), b=app.start("b", Arrays.asList(" x ", "y"), null);
        a.signal("submit", null);
        b.signal("submit", null);
        app.complete(a, "normalize");
        app.complete(b, "validate");
        app.complete(a, "validate");
        app.complete(a, "classify");
        accepted(a, Arrays.asList("ALPHA", "BETA"));
        assertFalse(b.isEnded());
        assertNull(b.getVariable("normalized"));
        assertNull(b.getVariable("result"));
        app.complete(b, "classify");
        app.complete(b, "normalize");
        accepted(b, Arrays.asList("X", "Y"));
        assertNotSame(a.getVariable("result"), b.getVariable("result"));
    }

    @Test
    public void nodeFailurePropagatesAndUpperLayerCancels() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("node-failure", INPUT, "normalize");
        p.signal("submit", null);
        expectFailure(()->app.complete(p, "normalize"), "normalize");
        assertEquals(true, p.getVariable("normalizationStarted"));
        assertFalse(p.isEnded());
        assertEquals(0, p.getVariable("publishAttempts"));
        assertEquals(0, p.getVariable("validationCallbacks"));
        assertNull(p.getVariable("result"));
        p.deleteCascade("upper layer aborts failed batch");
        assertTrue(p.isEnded());
        assertTrue(app.child("node-failure").isEnded());
        assertEquals(0, p.getVariable("validationCallbacks"));
        assertEquals(1, p.getVariable("endEvents"));
    }

    @Test
    public void childFailurePropagatesWithoutPublishing() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("child-failure", INPUT, "validation");
        p.signal("submit", null);
        app.complete(p, "normalize");
        app.complete(p, "classify");
        expectFailure(()->app.complete(p, "validate"), "validation");
        assertFalse(p.isEnded());
        assertFalse(app.child("child-failure").isEnded());
        assertEquals(0, p.getVariable("publishAttempts"));
        assertEquals(0, p.getVariable("validationCallbacks"));
        assertNull(p.getVariable("result"));
    }

    @Test
    public void publishFailureRetainsEarlierDataButDoesNotReturnSuccess() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("publish-failure", INPUT, "publish");
        p.signal("submit", null);
        app.complete(p, "normalize");
        app.complete(p, "validate");
        expectFailure(()->app.complete(p, "classify"), "publish");
        assertEquals(Arrays.asList("ALPHA", "BETA"), p.getVariable("normalized"));
        assertEquals(true, p.getVariable("valid"));
        assertEquals(1, p.getVariable("publishAttempts"));
        assertFalse(p.isEnded());
        assertNull(p.getVariable("result"));
        assertEquals(0, p.getVariable("endEvents"));
    }

    @Test
    public void endListenerFailureIsErrorEvenWithResultAndEndedFlag() {
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("end-failure", Collections.singletonList(" x "), "end");
        expectFailure(()->p.signal("submit", null), "end");
        assertTrue(p.isEnded());
        assertEquals(Collections.singletonList("X"), p.getVariable("result"));
        assertEquals(1, p.getVariable("endEvents"));
        // The driver saw an exception; persisted business data is not a success return and is not rolled back.
    }

    private void expectFailure(Runnable action, String stage) {
        try {
            action.run();
            fail("driver must receive failure");
        } catch (IllegalStateException expected) {
            assertEquals("injected "+stage+" failure", expected.getMessage());
        }
    }
}
