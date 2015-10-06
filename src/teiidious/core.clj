(ns teiidious.core
  (:require [clojure.java.io :as io])
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

(defn make-teiid-server
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

(defn vdbs
  [server]
  (-> server .getAdmin .getVDBs))

(defn metadata
  [vdb]
  (-> vdb
    (.getAttachment TransformationMetadata)
    .getMetadataStore))

;;; (-> metadata .getSchemas .getTables .getForeignKeys)
