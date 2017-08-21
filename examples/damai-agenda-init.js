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
            var folder = (data == null) ? null : data.folder;
            if (folder != null)
               session.folder = new java.io.File(folder);
            var dburl = (data == null) ? null : data.database;
            if (dburl != null)
               db = new org.regadou.repository.RepositoryMap(dburl, conf);
         }
      }
      session.database = db;
   }
   global.request = request;
   global.session = session;
}

