(defproject teiidious "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.jboss.teiid.examples/embedded-portfolio "1.0.1-SNAPSHOT"]
                 [com.h2database/h2 "1.3.152"]
                 [org.jboss.teiid.connectors/translator-file "9.0.0.Alpha2"]
                 [org.jboss.teiid.connectors/connector-file "9.0.0.Alpha2"]
                 [org.jboss.teiid.connectors/translator-jdbc "9.0.0.Alpha2"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [com.graphql-java/graphql-java "1.3"]
                 [org.immutant/web "2.1.0" :exclusions [org.jboss.logging/jboss-logging]]
                 [cheshire "5.4.0"]]
  :repositories [["jboss-dev" "http://repository.jboss.org/nexus/content/groups/developer/"]
                 ["jboss-earlyaccess" "https://maven.repository.redhat.com/earlyaccess/all/"]])
