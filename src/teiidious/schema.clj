(ns teiidious.schema
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc])
  (:import [graphql.schema GraphQLObjectType GraphQLSchema GraphQLFieldDefinition
            DataFetcher DataFetchingEnvironment GraphQLNonNull GraphQLList]
           [graphql GraphQL Scalars]))

(def hello
  (-> (GraphQLObjectType/newObject)
    (.name "helloWorldQuery")
    (.field (-> (GraphQLFieldDefinition/newFieldDefinition)
              (.type Scalars/GraphQLString)
              (.name "hello")
              (.staticValue "world")
              .build))
    .build))

(defn field-type
  "SQL to GraphQL type mapping"
  [m]
  (let [t (case (:type_name m)
            "integer"    Scalars/GraphQLInt
            ;; TODO: define scalar for "bigdecimal" Scalars/GraphQLFloat
            "boolean"    Scalars/GraphQLBoolean
            Scalars/GraphQLString)]
    (if (= 0 (:nullable m))
      (GraphQLNonNull. t)
      t)))

(defn column->field
  "Expects map from result of (jdbc/metadata-query (.getColumns ...))"
  [m]
  (-> (GraphQLFieldDefinition/newFieldDefinition)
    (.type (field-type m))
    (.name (str/lower-case (:column_name m)))
    (.build)))

(defn table->object
  "Expects table name and db connection spec"
  [db-spec table]
  (let [columns (jdbc/with-db-metadata [md db-spec]
                  (jdbc/metadata-query (.getColumns md nil nil table nil)))]
    (-> (GraphQLObjectType/newObject)
      (.name table)
      (.fields (for [m columns] (column->field m)))
      (.build))))

(defn table->field
  [db-spec table]
  (-> (GraphQLFieldDefinition/newFieldDefinition)
    (.name (str/lower-case table))
    (.type (GraphQLList. (table->object db-spec table)))
    (.dataFetcher
      (reify DataFetcher
        (get [_ ^DataFetchingEnvironment env]
          (-> (jdbc/query db-spec [(str "select * from " table)])
            clojure.walk/stringify-keys))))
    (.build)))

(defn query
  [name & fields]
  (-> (GraphQLObjectType/newObject)
    (.name name)
    (.fields fields)
    (.build)))

(defn schema
  [query-type]
  (-> (GraphQLSchema/newSchema)
    (.query query-type)
    .build))
