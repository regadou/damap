init(this);

function init(scope) {
   var conf = scope["org.regadou.damai.Configuration"];
   var cx = conf.getContextFactory().getScriptContext();
   var global = scope["nashorn.global"];
   var session = cx.getBindings(scope.SESSION_SCOPE);
   var request = cx.getBindings(scope.ENGINE_SCOPE);
   if (session.database == null) {
      var db = new java.util.LinkedHashMap(); 
      var ref = conf.getResourceManager().getResource(scope.users);
      if (ref != null) {
         var nalasys = ref.getValue();
         var user = scope.request.remoteUser;
         if (user == null) {
            var auth = scope.request.getHeader("authorization");
            if (auth != null) {
               var parts = auth.split(" ");
               if (parts.length > 1) {
                  var credential = new java.lang.String(javax.xml.bind.DatatypeConverter.parseBase64Binary(parts[1])).split(":");
                  user = credential[0];
               }
            }
         }
         if (user != null) {
            var data = nalasys[user];
            if (data != null) {
               var folder = data.folder;
               if (folder != null) {
                  ref = conf.getResourceManager().getResource(folder);
                  if (ref != null)
                     session.folder = ref.getValue();
               }
               var dburl = data.database;
               if (dburl != null) {
                  ref = conf.getResourceManager().getResource(dburl);
                  if (ref != null)
                     db = ref.getValue();
               }
            }
         }
      }
      session.database = db;
   }
   global.request = request;
   global.session = session;
}

