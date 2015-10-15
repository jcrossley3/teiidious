(ns teiidious.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import [org.h2.tools Server RunScript]
           [org.teiid.runtime EmbeddedServer EmbeddedConfiguration]
           [org.teiid.translator.jdbc.h2 H2ExecutionFactory]
           [org.teiid.translator.file FileExecutionFactory]
           [org.teiid.resource.adapter.file FileManagedConnectionFactory]
           [org.teiid.query.metadata TransformationMetadata]
           [org.teiid.example EmbeddedHelper]))

(defn start-db
  "Returns the DB instance"
  []
  (-> (Server/createTcpServer (make-array String 0))
    .start))

(defn datasource
  []
  (EmbeddedHelper/newDataSource "org.h2.Driver", "jdbc:h2:mem://localhost/~/account", "sa", "sa"))

(defn init-db
  [ds]
  (with-open [conn (.getConnection ds)]
    (RunScript/execute conn (-> "data/customer-schema.sql" io/resource io/reader))))

(defn teiid-portfolio-server
  [ds]
  (doto (EmbeddedServer.)
    (.addTranslator "translator-h2" (doto (H2ExecutionFactory.)
                                      (.setSupportsDirectQueryProcedure true)
                                      (.start)))
    (.addConnectionFactory "java:/accounts-ds" ds)
    (.addTranslator "file" (doto (FileExecutionFactory.) (.start)))
    (.addConnectionFactory "java:/marketdata-file" (-> (doto (FileManagedConnectionFactory.)
                                                         (.setParentDirectory (.getAbsolutePath (io/file "resources/data"))))
                                                     (.createConnectionFactory)))
    (.start (doto (EmbeddedConfiguration.)
              (.setTransactionManager (EmbeddedHelper/getTransactionManager))))
    (.deployVDB (-> "portfolio-vdb.xml" io/resource io/input-stream))))

(defn db-spec
  [srv]
  {:factory (fn [&_] (-> srv .getDriver (.connect "jdbc:teiid:Portfolio" nil)))})

(defn query
  [srv & sql]
  (sql/query (db-spec srv) sql))

(defn vdbs
  [server]
  (-> server .getAdmin .getVDBs))

(defn metadata
  [vdb]
  (-> vdb
    (.getAttachment TransformationMetadata)
    .getMetadataStore))

(defn dump
  [metadata]
  (doseq [[sname s] (.getSchemas metadata) :when (not (#{"SYS" "SYSADMIN" "pg_catalog"} sname))]
    (println sname)
    (println "  Tables:")
    (doseq [[tname t] (.getTables s) :let [t (bean t)]]
      (println "      " tname)
      (doseq [c (:columns t) :let [c (bean c)]]
        (println "        " (:name c))))
    (println "  Procedures:")
    (doseq [[tname t] (.getProcedures s) :let [t (bean t)]]
      (println "      " tname)
      (doseq [c (:parameters t) :let [c (bean c)]]
        (println "        " (:name c))))
    (println "  Functions:")
    (doseq [[tname t] (.getFunctions s) :let [t (bean t)]]
      (println "      " tname)
      (doseq [c (:input-parameters t) :let [c (bean c)]]
        (println "        " (:name c))))))

;;; (-> metadata .getSchemas .getTables .getForeignKeys)
;;; ignore tables where isSystem = true

;;; An alternative metadata approach: JDBC
;;; (pprint (sql/with-db-metadata [md (db-spec srv)] (sql/metadata-query (.getTables md nil nil nil (into-array String ["TABLE"])))))
;;; (pprint (sql/with-db-metadata [md (db-spec srv)] (sql/metadata-query (.getColumns md nil nil "ACCOUNT" nil))))
