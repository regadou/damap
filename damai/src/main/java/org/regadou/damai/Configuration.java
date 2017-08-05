package org.regadou.damai;

import java.net.URL;
import javax.activation.FileTypeMap;
import javax.script.ScriptEngineManager;

public interface Configuration {

   URL[] getClasspath();

   ScriptContextFactory getContextFactory();

   ConverterManager getConverterManager();

   ScriptEngineManager getEngineManager();

   MimeHandlerFactory getHandlerFactory();

   PropertyFactory getPropertyFactory();

   ResourceManager getResourceManager();

   FileTypeMap getTypeMap();

}