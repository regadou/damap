package org.regadou.damai;

import java.util.Map;

public interface PropertyFactory {

   Map<String,Property> getProperties(Object value);

   void setProperties(Class type, Action<Map<String,Property>> function);
}
