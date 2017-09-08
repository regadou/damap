print("arguments = "+arguments);
var conf = this["org.regadou.damai.Configuration"];
print("configuration = "+conf);
var it = java.util.Arrays.asList(conf.getClass().getMethods()).iterator();
while (it.hasNext()) {
   var m = it.next();
   if (m.getParameterCount() == 0 && m.getName().startsWith("get"))
      print(m.getName().substring(3)+" = "+m.invoke(conf));
}
print("context = "+conf.getContextFactory().getScriptContext());
var global = this["nashorn.global"];
for (var i in global)
   print(i+" = "+global[i]);
print("mimetype file is "+conf.getContextFactory().getClass().getResource("/mimetypes"));
"";

