package org.regadou.damai;

import java.util.Collection;

public interface Filterable {

   Collection filter(Expression filterExpression);
}
