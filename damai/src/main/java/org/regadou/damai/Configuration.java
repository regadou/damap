package org.regadou.damai;

import java.net.URL;
import javax.activation.FileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptEngineManager;

public interface Configuration {

   URL[] getClasspath();

   Bindings getGlobalScope();

   URL getInitScript();

   ScriptContextFactory getContextFactory();

   Converter getConverter();

   ScriptEngineManager getEngineManager();

   MimeHandlerFactory getHandlerFactory();

   PropertyManager getPropertyManager();

   ResourceManager getResourceManager();

   FileTypeMap getTypeMap();

}
