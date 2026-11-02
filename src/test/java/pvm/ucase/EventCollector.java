package pvm.ucase;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.delegate.ExecutionListenerExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EventCollector implements ExecutionListener {

  private static final long serialVersionUID = 1L;

  private static Logger log = LoggerFactory.getLogger(EventCollector.class);

  public List<String> events = new ArrayList<String>();

  public void notify(DelegateExecution execution) throws Exception {
    notify((ExecutionListenerExecution)execution);
  }

  public void notify(ExecutionListenerExecution execution) {

    log.debug("collecting event: {} on {}", execution.getEventName(), execution.getEventSource());
    events.add(execution.getEventName()+" on "+execution.getEventSource());
  }

  public String toString() {
    StringBuilder text = new StringBuilder();
    for (String event: events) {
      text.append(event);
      text.append("\n");
    }
    return text.toString();
  }
}
