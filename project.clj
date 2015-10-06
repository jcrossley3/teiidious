(defproject teiidious "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.jboss.teiid.examples/teiid-examples-common "1.0.0-SNAPSHOT"]
                 [com.h2database/h2 "1.3.152"]
                 [org.jboss.teiid.connectors/translator-file "8.12.0.CR2-SNAPSHOT"]
                 [org.jboss.teiid.connectors/connector-file "8.12.0.CR2-SNAPSHOT" :classifier "lib"]
                 [org.jboss.teiid.connectors/translator-jdbc "8.12.0.CR2-SNAPSHOT"]]
  :repositories [["jboss-dev" "http://repository.jboss.org/nexus/content/groups/developer/"]
                 ["jboss-earlyaccess" "https://maven.repository.redhat.com/earlyaccess/all/"]])
