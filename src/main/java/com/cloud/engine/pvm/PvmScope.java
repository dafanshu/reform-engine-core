package com.cloud.engine.pvm;
import java.util.List;
public interface PvmScope extends PvmProcessElement {

  List<? extends PvmActivity> getActivities();

  PvmActivity findActivity(String activityId);

}
