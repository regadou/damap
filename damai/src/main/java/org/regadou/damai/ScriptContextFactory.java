package org.regadou.damai;

import javax.script.ScriptContext;

public interface ScriptContextFactory {

   ScriptContext getScriptContext(Reference...properties);

   boolean closeScriptContext(ScriptContext context);
}
