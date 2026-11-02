package com.cloud.engine.delegate;

import java.io.Serializable;

public interface Expression extends Serializable {

   Object getValue(VariableScope variableScope);

   void setValue(Object value, VariableScope variableScope);

   String getExpressionText();

}
