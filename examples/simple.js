var conf = this["org.regadou.damai.Configuration"];
print("configuration = "+conf);
print("context = "+conf.getContextFactory().getScriptContext());
var arrayClass = java.lang.Class.forName("[Ljava.lang.Object;");
var it = java.util.Arrays.asList(conf.getClass().getMethods()).iterator();
while (it.hasNext()) {
   var m = it.next();
   if (m.getParameterCount() == 0 && m.getName().startsWith("get")) {
      var value = m.invoke(conf);
      if (arrayClass.isInstance(value))
         value = java.util.Arrays.asList(value);
      print(m.getName().substring(3)+" = "+value);   
   }
}
"";

