package acceptance;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.delegate.*;
import java.util.*;

/** Fictional consumer-owned workflow behaviors; no runtime implementation or engine test helpers. */
public final class TextBatchApplication {

    private final Map<String, PvmProcessInstance> children = new HashMap<>();

    private final PvmProcessDefinition definition;

    private static void next(ActivityExecution e) {
        e.take(e.getActivity().getOutgoingTransitions().get(0));
    }

    private static void route(ActivityExecution e, String id) {
        for (PvmTransition t:e.getActivity().getOutgoingTransitions())if (id.equals(t.getId())) {
            e.take(t);
            return;
        }
        throw new IllegalArgumentException("unknown route "+id);
    }

    private static void failIf(ActivityExecution e, String stage) {
        if (stage.equals(e.getVariable("fault")))throw new IllegalStateException("injected "+stage+" failure");
    }

    @SuppressWarnings("unchecked")

    private static List<String> records(DelegateExecution e) {
        return (List<String>)e.getVariable("records");
    }

    private static void normalize(ActivityExecution e) {
        e.setVariable("normalizationStarted", true);
        failIf(e, "normalize");
        e.createVariableLocal("scratch", "temporary");
        if (!"temporary".equals(e.getVariableLocal("scratch")))throw new IllegalStateException("local variable contract failed");
        List<String> result=new ArrayList<>();
        for (String value:records(e))result.add(value.trim().toUpperCase(Locale.ROOT));
        e.setVariable("normalized", result);
        e.removeVariableLocal("scratch");
    }

    private interface TaskAction {
        void run(ActivityExecution e);
    }

    private static SignallableActivityBehavior task(String accepted, TaskAction action) {
        return new SignallableActivityBehavior() {
            public void execute(ActivityExecution e) {
            }
            public void signal(ActivityExecution e, String name, Object data) {
                if (!accepted.equals(name))throw new IllegalArgumentException("unexpected signal "+name);
                action.run(e);
            }
        };
    }

    private static final class Parallel implements ActivityBehavior {
        public void execute(ActivityExecution e) {
            e.inactivate();
            List<ActivityExecution> joined=e.findInactiveConcurrentExecutions(e.getActivity());
            if (joined.size()==e.getActivity().getIncomingTransitions().size())e.takeAll(e.getActivity().getOutgoingTransitions(), joined);
        }
    }

    public TextBatchApplication() {
        PvmProcessDefinition childDefinition=new ProcessDefinitionBuilder("validation")
            .createActivity("validationScope").scope()
                .createActivity("validate").initial().behavior(task("check", e->{
                    failIf(e, "validation");e.setVariable("valid", !records(e).isEmpty());e.end();
                })).endActivity()
            .endActivity().buildProcessDefinition();
        SubProcessActivityBehavior validationCall=new SubProcessActivityBehavior(){
            public void execute(ActivityExecution e){
                PvmProcessInstance child=e.createSubProcessInstance(childDefinition);
                if (child.hasVariable("records"))throw new IllegalStateException("child inherited caller input");
                child.setVariable("records", new ArrayList<>(records(e)));
                child.setVariable("fault", e.getVariable("fault"));
                children.put((String)e.getVariable("batchId"), child);child.start();
            }
            public void completing(DelegateExecution e, DelegateExecution child){e.setVariable("valid", child.getVariable("valid"));}
            public void completed(ActivityExecution e){
                e.setVariable("validationCallbacks", (Integer)e.getVariable("validationCallbacks")+1);next(e);
            }
        };
        definition=new ProcessDefinitionBuilder("text-batch")
            .executionListener(PvmEvent.EVENTNAME_END, e->{
                e.setVariable("endEvents", (Integer)e.getVariable("endEvents")+1);
                if ("end".equals(e.getVariable("fault")))throw new IllegalStateException("injected end failure");
            })
            .createActivity("submit").initial().behavior(task("submit", TextBatchApplication::next)).transition("choose").endActivity()
            .createActivity("choose").behavior(e->{
                boolean invalid=records(e).isEmpty()||records(e).stream().anyMatch(x->x==null||x.trim().isEmpty());
                route(e, invalid?"reject":records(e).size()==1?"quick":"full");
            }).transition("rejected", "reject").transition("quick", "quick").transition("fork", "full").endActivity()
            .createActivity("processingScope").scope()
                .createActivity("fork").behavior(new Parallel()).transition("normalize").transition("classify").transition("validationCall").endActivity()
                .createActivity("normalizationScope").scope()
                    .createActivity("normalize").behavior(task("run", e->{normalize(e);next(e);})).transition("join").endActivity()
                .endActivity()
                .createActivity("classify").behavior(task("run", e->{e.setVariable("category", "batch");next(e);})).transition("join").endActivity()
                .createActivity("validationCall").behavior(validationCall).transition("join").endActivity()
                .createActivity("join").behavior(new Parallel()).transition("publish").endActivity()
            .endActivity()
            .createActivity("quick").behavior(e->{normalize(e);e.setVariable("valid", true);e.setVariable("category", "single");next(e);}).transition("publish").endActivity()
            .createActivity("publish").behavior(e->{
                e.setVariable("publishAttempts", (Integer)e.getVariable("publishAttempts")+1);failIf(e, "publish");
                if (!Boolean.TRUE.equals(e.getVariable("valid")))throw new IllegalStateException("validation result missing");
                e.setVariable("result", e.getVariable("normalized"));e.setVariable("status", "accepted");e.end();
            }).endActivity()
            .createActivity("rejected").behavior(e->{e.setVariable("status", "rejected");e.end();}).endActivity()
            .buildProcessDefinition();
    }

    public PvmProcessInstance start(String id, List<String> values, String fault) {
        PvmProcessInstance root=definition.createProcessInstance();
        root.setVariable("batchId", id);
        root.setVariable("records", new ArrayList<>(values));
        root.setVariable("fault", fault);
        root.setVariable("status", "pending");
        root.setVariable("validationCallbacks", 0);
        root.setVariable("publishAttempts", 0);
        root.setVariable("endEvents", 0);
        root.start();
        return root;
    }

    public PvmProcessInstance child(String id) {
        return children.get(id);
    }

    public void complete(PvmProcessInstance root, String task) {
        if ("validate".equals(task))child((String)root.getVariable("batchId")).findExecution("validate").signal("check", null);
        else root.findExecution(task).signal("run", null);
    }

    public static void main(String[] args) {
        System.out.println("ENGINE_SOURCE="+ProcessDefinitionBuilder.class.getProtectionDomain().getCodeSource().getLocation());
        TextBatchApplication app=new TextBatchApplication();
        PvmProcessInstance p=app.start("fictional-001", Arrays.asList(" alpha ", "Beta"), null);
        p.signal("submit", null);
        System.out.println("AFTER_SUBMIT="+p.findActiveActivityIds());
        for (String task:Arrays.asList("classify", "validate", "normalize"))app.complete(p, task);
        if (!p.isEnded()||!Arrays.asList("ALPHA", "BETA").equals(p.getVariable("result")))throw new IllegalStateException("acceptance failed");
        System.out.println("RESULT="+p.getVariable("result")+" STATUS="+p.getVariable("status")+" ENDED="+p.isEnded()+" END_EVENTS="+p.getVariable("endEvents"));
    }
}
