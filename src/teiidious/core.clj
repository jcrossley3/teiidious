(ns teiidious.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [immutant.web :as web]
            [immutant.codecs :refer (encode decode)]
            [teiidious.schema :as s])
  (:import [org.h2.tools Server RunScript]
           [org.teiid.runtime EmbeddedServer EmbeddedConfiguration]
           [org.teiid.translator.jdbc.h2 H2ExecutionFactory]
           [org.teiid.translator.file FileExecutionFactory]
           [org.teiid.resource.adapter.file FileManagedConnectionFactory]
           [org.teiid.query.metadata TransformationMetadata]
           [org.teiid.example EmbeddedHelper]
           [graphql GraphQL]))

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

(defn vdb
  [server]
  (-> server .getAdmin .getVDBs first))

(defn metadata
  [vdb]
  (-> vdb
    (.getAttachment TransformationMetadata)
    .getMetadataStore))

(defn tables
  [server]
  (->> (vdb server)
    metadata
    .getSchemas
    vals
    (remove #(every? (memfn isSystem) (vals (.getTables %)))) ; teiid system tables
    (map (comp vals (memfn getTables)))
    flatten
    (map (memfn getName))))

;;; (-> metadata .getSchemas .getTables .getForeignKeys)
;;; ignore tables where isSystem = true

;;; An alternative metadata approach: JDBC
;;; (pprint (sql/with-db-metadata [md (db-spec srv)] (sql/metadata-query (.getTables md nil nil nil (into-array String ["TABLE"])))))
;;; (pprint (sql/with-db-metadata [md (db-spec srv)] (sql/metadata-query (.getColumns md nil nil "ACCOUNT" nil))))

(defn handler
  [graphql]
  (fn [req]
    {:status 200
     :headers {"Access-Control-Allow-Origin"  "*"
               "Access-Control-Allow-Headers" "Content-Type"
               "Access-Control-Allow-Methods" "GET,POST,OPTIONS"}
     :body (when (= :post (:request-method req))
             (as-> (decode (slurp (:body req)) :json) x
               (.execute graphql (:query x))
               {:data (.getData x) :errors (.getErrors x)}
               (encode x :json)))}))

(defn mount
  [schema]
  (web/run (handler (GraphQL. schema))))


(defn start-teiid []
  (start-db)
  (let [ds (datasource)]
    (init-db ds)
    (teiid-portfolio-server ds)))

(defn start-graphql [teiid-server]
  (->> (tables teiid-server)
    (map (partial s/table->field (db-spec teiid-server)))
    (apply s/query (.getName (vdb teiid-server)))
    s/schema
    mount))
