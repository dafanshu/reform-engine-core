package pvm;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import junit.framework.TestCase;

public class ProcessDefinitionBuilderRegressionTest extends TestCase {
    public void testNamedBuilderAcceptsProcessPropertyImmediately() {
        PvmProcessDefinition definition = new ProcessDefinitionBuilder("p")
                .property("label", "process").buildProcessDefinition();
        assertEquals("p", definition.getId());
        assertEquals("process", definition.getProperty("label"));
    }

    public void testDefaultBuilderAcceptsProcessPropertyImmediately() {
        assertEquals("process", new ProcessDefinitionBuilder().property("label", "process")
                .buildProcessDefinition().getProperty("label"));
    }

    public void testPropertiesFollowActivityAndTransitionContext() {
        PvmProcessDefinition definition = new ProcessDefinitionBuilder("p")
                .property("label", "process")
                .createActivity("a").property("label", "activity")
                    .startTransition("b").property("label", "transition").endTransition()
                    .property("afterTransition", "activity")
                .endActivity().property("afterActivity", "process")
                .createActivity("b").endActivity().buildProcessDefinition();
        assertEquals("process", definition.getProperty("label"));
        assertEquals("process", definition.getProperty("afterActivity"));
        assertEquals("activity", definition.findActivity("a").getProperty("label"));
        assertEquals("activity", definition.findActivity("a").getProperty("afterTransition"));
        assertEquals("transition", definition.findActivity("a").getOutgoingTransitions().get(0).getProperty("label"));
    }
}
