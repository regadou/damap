var conf = this["org.regadou.damai.Configuration"];
if (session == null) {
   var cx = conf.getContextFactory().getScriptContext();
   cx.setAttribute("session", {}, SESSION_SCOPE);
}
if (database == null) {
   var cx = conf.getContextFactory().getScriptContext();
   var db = {}; 
   var ref = conf.getResourceManager().getResource(users);
   if (ref != null) {
      var nalasys = ref.getValue();
      var user = request.remoteUser;
      if (user != null) {
         var data = nalasys[user];
         var folder = (data == null) ? null : data.folder;
         if (folder != null)
            cx.setAttribute("folder", new java.io.File(folder), SESSION_SCOPE);
         var dburl = (data == null) ? null : data.database;
         if (dburl != null)
            db = new org.regadou.repository.RepositoryMap(dburl, conf);
      }
   }
   cx.setAttribute("database", db, SESSION_SCOPE);
}

